import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import Editor, { type OnMount } from '@monaco-editor/react';
import { monaco } from './monaco';
import { api, ApiError } from '../api';
import { loadProject, saveProject, type StoredProject } from '../storage';
import type { CheckResult, Diagnostic, OutputFile, ProjectFile } from '../types';
import { CheckOutput } from './CheckOutput';
import { FileTabs } from './FileTabs';
import { applyOutputFiles, isSource, languageOf, MAIN_FILE, newFileContent, sortFiles, type FileChanges } from './project';
import { ConsoleSocket, type ConsoleEvent } from './consoleSocket';
import { appendSegment, EMPTY_TERMINAL, Terminal, type SegmentKind, type TerminalState } from './Terminal';
import { Mascot, type MascotMood } from '../components/Mascot';

type Tab = 'console' | 'tests';

export interface IdeProps {
  /** Ключ для сохранения черновика в браузере. */
  storageKey: string;
  /** Стартовый проект: один Main.java или несколько файлов. */
  initialFiles: ProjectFile[];
  /** Если задан, доступна кнопка «Проверить». */
  lessonSlug?: string;
  onPassed?: () => void;
  /** Вызывается после каждой проверки — для подсказок и аналитики урока. */
  onChecked?: (result: CheckResult) => void;
  /** Вызывается после каждого запуска. */
  onRun?: (status: string, firstErrorCode: string | null) => void;
}

interface LiveState {
  status: 'idle' | 'checking' | 'ok' | 'errors' | 'unavailable';
  diagnostics: Diagnostic[];
}

type MonacoEditor = Parameters<OnMount>[0];

const LIVE_DELAY_MS = 700;
const MAX_LIVE_SOURCE = 50_000;

function initialProject(files: ProjectFile[]): StoredProject {
  const sorted = sortFiles(files);
  return { files: sorted, active: sorted.find((f) => isSource(f.name))?.name ?? sorted[0]?.name ?? MAIN_FILE };
}

const sameFiles = (a: ProjectFile[], b: ProjectFile[]) =>
  a.length === b.length && a.every((f) => b.some((g) => g.name === f.name && g.content === f.content));

export default function Ide({ storageKey, initialFiles, lessonSlug, onPassed, onChecked, onRun }: IdeProps) {
  const [project, setProjectState] = useState<StoredProject>(
    () => loadProject(storageKey, initialFiles) ?? initialProject(initialFiles),
  );
  const { files, active } = project;
  const activeFile = files.find((f) => f.name === active) ?? files[0];
  const [tab, setTab] = useState<Tab>('console');
  const [terminal, setTerminal] = useState<TerminalState>(EMPTY_TERMINAL);
  const [checking, setChecking] = useState(false);
  const [checkResult, setCheckResult] = useState<CheckResult | null>(null);
  const [checkError, setCheckError] = useState<string | null>(null);
  const [live, setLive] = useState<LiveState>({ status: 'idle', diagnostics: [] });
  const [cursor, setCursor] = useState({ line: 1, column: 1 });
  const [reaction, setReaction] = useState<{ mood: MascotMood; key: number }>({ mood: 'idle', key: 0 });
  const editorRef = useRef<MonacoEditor | null>(null);
  const pendingReveal = useRef<{ line: number; column: number } | null>(null);
  const socketRef = useRef<ConsoleSocket | null>(null);
  const markersRef = useRef<Diagnostic[]>([]);
  const compileTimeRef = useRef(0);
  const pendingOutput = useRef<{ kind: SegmentKind; text: string }[]>([]);
  const flushScheduled = useRef(false);
  const callbacks = useRef({ onRun });
  callbacks.current = { onRun };

  const setMood = useCallback((mood: MascotMood) => setReaction((r) => ({ mood, key: r.key + 1 })), []);

  /** Каждый файл — отдельная модель Monaco со своей историей правок; адрес уникален для страницы. */
  const modelPath = useCallback((name: string) => `file:///${storageKey.replace(/[^\w-]/g, '_')}/${name}`, [storageKey]);

  const setProject = useCallback(
    (update: (p: StoredProject) => StoredProject) =>
      setProjectState((p) => {
        const next = update(p);
        saveProject(storageKey, sameFiles(next.files, initialFiles) ? null : next);
        return next;
      }),
    [initialFiles, storageKey],
  );

  /** Подчёркивает ошибки в каждом открытом файле. Сообщения без файла относятся к проекту целиком. */
  const applyMarkers = useCallback(() => {
    const diagnostics = markersRef.current;
    for (const model of monaco.editor.getModels()) {
      const uri = model.uri.toString();
      const prefix = modelPath('');
      if (!uri.startsWith(prefix)) continue;
      const name = decodeURIComponent(uri.slice(prefix.length));
      monaco.editor.setModelMarkers(
        model,
        'javac',
        diagnostics
          .filter((d) => d.line > 0 && (d.file ?? MAIN_FILE) === name)
          .map((d) => ({
            severity: d.severity === 'ERROR' ? monaco.MarkerSeverity.Error : monaco.MarkerSeverity.Warning,
            startLineNumber: d.line,
            endLineNumber: d.line,
            startColumn: Math.max(1, d.column),
            endColumn: Math.max(d.column + 1, d.endColumn),
            message: d.hint ? `${d.message}\n\nПодсказка: ${d.hint}` : d.message,
          })),
      );
    }
  }, [modelPath]);

  const showDiagnostics = useCallback(
    (diagnostics: Diagnostic[]) => {
      markersRef.current = diagnostics;
      applyMarkers();
    },
    [applyMarkers],
  );

  // Актуальный проект для обработчиков событий консоли, которые живут дольше одного рендера.
  const projectRef = useRef(project);
  projectRef.current = project;

  /** Что программа сделала с файлами рабочей папки — переносим в проект, как на настоящем диске. */
  const syncOutputFiles = useCallback(
    (outputs: OutputFile[] | undefined): FileChanges | null => {
      if (!outputs) return null;
      const current = projectRef.current;
      const result = applyOutputFiles(current.files, outputs);
      const { created, changed, deleted, binary } = result.changes;
      if (created.length + changed.length + deleted.length > 0) {
        const active = result.files.some((f) => f.name === current.active) ? current.active : result.files[0].name;
        setProject(() => ({ files: result.files, active }));
      }
      return created.length + changed.length + deleted.length + binary.length > 0 ? result.changes : null;
    },
    [setProject],
  );

  // Вывод программы приходит частями; склеиваем их и обновляем экран не чаще раза за кадр.
  const flushOutput = useCallback(() => {
    flushScheduled.current = false;
    const chunks = pendingOutput.current;
    if (chunks.length === 0) return;
    pendingOutput.current = [];
    setTerminal((t) => ({
      ...t,
      segments: chunks.reduce((segments, chunk) => appendSegment(segments, chunk.kind, chunk.text), t.segments),
    }));
  }, []);

  const queueOutput = useCallback(
    (kind: SegmentKind, text: string) => {
      pendingOutput.current.push({ kind, text });
      if (!flushScheduled.current) {
        flushScheduled.current = true;
        requestAnimationFrame(flushOutput);
      }
    },
    [flushOutput],
  );

  const handleEvent = useCallback(
    (event: ConsoleEvent) => {
      switch (event.type) {
        case 'started':
          compileTimeRef.current = event.compileTimeMs;
          showDiagnostics(event.diagnostics);
          setLive({ status: 'ok', diagnostics: event.diagnostics });
          setTerminal((t) => ({ ...t, phase: 'running' }));
          break;
        case 'compile_error':
          showDiagnostics(event.diagnostics);
          setLive({ status: 'errors', diagnostics: event.diagnostics });
          setTerminal({
            ...EMPTY_TERMINAL,
            phase: 'done',
            compileErrors: event.diagnostics.filter((d) => d.severity === 'ERROR'),
          });
          setMood('sad');
          callbacks.current.onRun?.('COMPILATION_ERROR', firstErrorCode(event.diagnostics));
          break;
        case 'out':
          queueOutput(event.stream, event.data);
          break;
        case 'exit': {
          flushOutput();
          const fileChanges = event.status === 'FILES_LIMIT' ? null : syncOutputFiles(event.files);
          setTerminal((t) => ({
            ...t,
            phase: 'done',
            exit: {
              status: event.status,
              exitCode: event.exitCode,
              timeMs: event.timeMs,
              compileTimeMs: compileTimeRef.current,
            },
            fileChanges,
          }));
          setMood(event.status === 'SUCCESS' || event.status === 'STOPPED' ? 'idle' : 'sad');
          callbacks.current.onRun?.(event.status, null);
          break;
        }
        case 'error':
          flushOutput();
          setTerminal((t) => ({ ...t, phase: 'done', error: event.message }));
          break;
      }
    },
    [flushOutput, queueOutput, setMood, showDiagnostics, syncOutputFiles],
  );

  const socket = useCallback(() => {
    if (!socketRef.current) {
      socketRef.current = new ConsoleSocket(handleEvent, () =>
        setTerminal((t) =>
          t.phase === 'running' || t.phase === 'compiling'
            ? { ...t, phase: 'done', error: 'Соединение с сервером прервалось. Запустите программу ещё раз.' }
            : t,
        ),
      );
    }
    return socketRef.current;
  }, [handleEvent]);

  useEffect(() => () => socketRef.current?.close(), []);

  /** Запуск без интерактивной консоли — если WebSocket недоступен (например, его режет прокси). */
  const runWithoutConsole = useCallback(async () => {
    try {
      const result = await api.run(files, '');
      showDiagnostics(result.diagnostics);
      if (result.status === 'COMPILATION_ERROR') {
        setTerminal({
          ...EMPTY_TERMINAL,
          phase: 'done',
          compileErrors: result.diagnostics.filter((d) => d.severity === 'ERROR'),
        });
      } else {
        let segments = appendSegment([], 'stdout', result.stdout);
        segments = appendSegment(segments, 'stderr', result.stderr);
        const fileChanges = result.status === 'FILES_LIMIT' ? null : syncOutputFiles(result.files);
        setTerminal({
          ...EMPTY_TERMINAL,
          phase: 'done',
          segments,
          error: 'Интерактивная консоль недоступна, поэтому программа запущена без ввода.',
          exit: {
            status: result.status,
            exitCode: result.exitCode,
            timeMs: result.runTimeMs,
            compileTimeMs: result.compileTimeMs,
          },
          fileChanges,
        });
      }
      setMood(result.status === 'SUCCESS' ? 'idle' : 'sad');
      callbacks.current.onRun?.(result.status, firstErrorCode(result.diagnostics));
    } catch (e) {
      setTerminal({
        ...EMPTY_TERMINAL,
        phase: 'done',
        error: e instanceof ApiError ? e.message : 'Не удалось выполнить программу.',
      });
    }
  }, [files, setMood, showDiagnostics, syncOutputFiles]);

  const run = useCallback(async () => {
    setTab('console');
    pendingOutput.current = [];
    setTerminal({ ...EMPTY_TERMINAL, phase: 'compiling' });
    try {
      await socket().send({ type: 'run', files });
    } catch {
      await runWithoutConsole();
    }
  }, [files, runWithoutConsole, socket]);

  const stop = useCallback(() => {
    socket()
      .send({ type: 'stop' })
      .catch(() => undefined);
  }, [socket]);

  const sendInput = useCallback(
    (line: string) => {
      queueOutput('stdin', line);
      socket()
        .send({ type: 'input', data: line })
        .catch(() => undefined);
    },
    [queueOutput, socket],
  );

  const sendEof = useCallback(() => {
    socket()
      .send({ type: 'eof' })
      .catch(() => undefined);
  }, [socket]);

  const check = useCallback(async () => {
    if (checking || !lessonSlug) return;
    setChecking(true);
    setCheckError(null);
    setTab('tests');
    try {
      const result = await api.check(lessonSlug, files);
      setCheckResult(result);
      showDiagnostics(result.diagnostics);
      setMood(result.passed ? 'happy' : 'sad');
      onChecked?.(result);
      if (result.passed) onPassed?.();
    } catch (e) {
      setCheckError(e instanceof ApiError ? e.message : 'Не удалось проверить решение.');
    } finally {
      setChecking(false);
    }
  }, [checking, files, lessonSlug, onChecked, onPassed, setMood, showDiagnostics]);

  // Проверка на ошибки во время набора: через паузу после последнего изменения исходников.
  const sources = useMemo(() => files.filter((f) => isSource(f.name)), [files]);
  useEffect(() => {
    const length = sources.reduce((sum, f) => sum + f.content.length, 0);
    if (length > MAX_LIVE_SOURCE || sources.every((f) => !f.content.trim())) return;
    const controller = new AbortController();
    const timer = window.setTimeout(async () => {
      setLive((l) => ({ ...l, status: 'checking' }));
      try {
        const result = await api.compile(sources, controller.signal);
        const hasErrors = result.diagnostics.some((d) => d.severity === 'ERROR');
        setLive({ status: hasErrors ? 'errors' : 'ok', diagnostics: result.diagnostics });
        showDiagnostics(result.diagnostics);
      } catch (e) {
        if (e instanceof DOMException && e.name === 'AbortError') return;
        setLive((l) => ({ ...l, status: 'unavailable' }));
      }
    }, LIVE_DELAY_MS);
    return () => {
      window.clearTimeout(timer);
      controller.abort();
    };
  }, [sources, showDiagnostics]);

  // Горячие клавиши регистрируются один раз, поэтому вызываем актуальные обработчики через ref.
  const actions = useRef({ run, check });
  actions.current = { run, check };

  const handleMount: OnMount = (editor) => {
    editorRef.current = editor;
    editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyCode.Enter, () => actions.current.run());
    editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyMod.Shift | monaco.KeyCode.Enter, () =>
      actions.current.check(),
    );
    editor.onDidChangeCursorPosition((e) => setCursor({ line: e.position.lineNumber, column: e.position.column }));
    // При переключении вкладки Monaco подставляет другую модель — подчёркивания и переход к ошибке.
    editor.onDidChangeModel(() => {
      applyMarkers();
      const target = pendingReveal.current;
      pendingReveal.current = null;
      if (target) reveal(editor, target.line, target.column);
    });
    applyMarkers();
  };

  // Модели этой страницы удаляются вместе с редактором, иначе при следующем открытии покажется старый текст.
  useEffect(
    () => () => {
      const prefix = modelPath('');
      monaco.editor.getModels().forEach((m) => m.uri.toString().startsWith(prefix) && m.dispose());
    },
    [modelPath],
  );

  const handleChange = (value: string | undefined) => {
    const next = value ?? '';
    const name = active;
    setProject((p) => ({ ...p, files: p.files.map((f) => (f.name === name ? { ...f, content: next } : f)) }));
  };

  const reset = () => {
    if (!sameFiles(files, initialFiles) && !window.confirm('Вернуть исходный код? Ваши изменения будут потеряны.')) {
      return;
    }
    // Модели удаляем, чтобы у файлов не осталась история правок и текст из черновика.
    const prefix = modelPath('');
    monaco.editor.getModels().forEach((m) => m.uri.toString().startsWith(prefix) && m.dispose());
    setProjectState(initialProject(initialFiles));
    saveProject(storageKey, null);
    setTerminal(EMPTY_TERMINAL);
    setCheckResult(null);
    showDiagnostics([]);
  };

  const selectFile = useCallback((name: string) => setProject((p) => ({ ...p, active: name })), [setProject]);

  const goTo = useCallback(
    (line: number, column: number, file?: string | null) => {
      const editor = editorRef.current;
      if (!editor || line <= 0) return;
      const target = file ?? MAIN_FILE;
      if (target !== active && files.some((f) => f.name === target)) {
        pendingReveal.current = { line, column };
        selectFile(target);
        return;
      }
      reveal(editor, line, column);
    },
    [active, files, selectFile],
  );

  const createFile = (name: string) =>
    setProject((p) => ({ files: sortFiles([...p.files, { name, content: newFileContent(name) }]), active: name }));

  const renameFile = (from: string, to: string) => {
    const prefix = modelPath('');
    monaco.editor.getModel(monaco.Uri.parse(prefix + from))?.dispose();
    setProject((p) => ({
      files: sortFiles(p.files.map((f) => (f.name === from ? { ...f, name: to } : f))),
      active: p.active === from ? to : p.active,
    }));
  };

  const deleteFile = (name: string) => {
    monaco.editor.getModel(monaco.Uri.parse(modelPath(name)))?.dispose();
    setProject((p) => {
      const rest = p.files.filter((f) => f.name !== name);
      return { files: rest, active: p.active === name ? (rest.find((f) => isSource(f.name)) ?? rest[0]).name : p.active };
    });
  };

  const isMac = typeof navigator !== 'undefined' && /Mac|iPhone|iPad/.test(navigator.platform);
  const mod = isMac ? '⌘' : 'Ctrl';
  const running = terminal.phase === 'running';
  const compiling = terminal.phase === 'compiling';
  const errors = live.diagnostics.filter((d) => d.severity === 'ERROR');
  const warnings = live.diagnostics.filter((d) => d.severity === 'WARNING');
  const firstProblem = errors[0] ?? warnings[0];
  const errorsByFile: Record<string, number> = {};
  for (const d of errors) {
    const name = d.file ?? MAIN_FILE;
    errorsByFile[name] = (errorsByFile[name] ?? 0) + 1;
  }
  const multiFile = files.length > 1;

  return (
    <div className="ide">
      <div className="ide-toolbar">
        {running ? (
          <button className="btn btn-secondary btn-sm btn-stop" onClick={stop} title="Остановить программу">
            <StopIcon />
            Стоп
          </button>
        ) : (
          <button className="btn btn-primary btn-sm" onClick={run} disabled={compiling} title={`${mod} + Enter`}>
            {compiling ? <span className="spinner" /> : <PlayIcon />}
            Запустить
          </button>
        )}
        {lessonSlug && (
          <button
            className="btn btn-secondary btn-sm btn-check"
            onClick={check}
            disabled={checking}
            title={`${mod} + Shift + Enter`}
          >
            {checking ? <span className="spinner" /> : <CheckIcon />}
            Проверить
          </button>
        )}
        <span className="ide-hint">
          <kbd>{mod}</kbd>
          <kbd>↵</kbd>
          запуск
        </span>
        <button className="btn btn-ghost btn-sm" onClick={reset} title="Вернуть исходный код">
          <ResetIcon />
          <span className="hide-narrow">Сбросить</span>
        </button>
      </div>

      <FileTabs
        files={files}
        active={activeFile.name}
        errors={errorsByFile}
        onSelect={selectFile}
        onCreate={createFile}
        onRename={renameFile}
        onDelete={deleteFile}
      />

      <div className="ide-editor">
        <Editor
          path={modelPath(activeFile.name)}
          language={languageOf(activeFile.name)}
          theme="byte-dark"
          value={activeFile.content}
          onChange={handleChange}
          onMount={handleMount}
          loading={<div className="ide-loading">Загружаем редактор…</div>}
          options={{
            fontSize: 13.5,
            fontFamily: "'SF Mono', SFMono-Regular, ui-monospace, 'JetBrains Mono', Menlo, Consolas, monospace",
            fontLigatures: false,
            lineHeight: 22,
            minimap: { enabled: false },
            scrollBeyondLastLine: false,
            automaticLayout: true,
            tabSize: 4,
            insertSpaces: true,
            renderWhitespace: 'selection',
            bracketPairColorization: { enabled: true },
            guides: { bracketPairs: true, indentation: true },
            padding: { top: 12, bottom: 12 },
            smoothScrolling: true,
            fixedOverflowWidgets: true,
            wordBasedSuggestions: 'currentDocument',
            quickSuggestions: { other: true, comments: false, strings: false },
            suggestOnTriggerCharacters: true,
          }}
        />
      </div>

      <div className="ide-statusbar">
        <button
          className={`statusbar-problems ${live.status}`}
          onClick={() => firstProblem && goTo(firstProblem.line, firstProblem.column, firstProblem.file)}
          disabled={!firstProblem}
          title={firstProblem ? 'Перейти к ошибке' : undefined}
        >
          {live.status === 'checking' ? (
            <>
              <span className="spinner" /> Проверка…
            </>
          ) : live.status === 'unavailable' ? (
            'Проверка недоступна'
          ) : errors.length > 0 ? (
            <>
              <span className="statusbar-dot fail" />
              {errors.length} {plural(errors.length, ['ошибка', 'ошибки', 'ошибок'])}
              <span className="statusbar-message">
                · {multiFile && errors[0].file ? `${errors[0].file}, ` : ''}строка {errors[0].line}:{' '}
                {errors[0].hint ?? errors[0].message}
              </span>
            </>
          ) : live.status === 'ok' ? (
            <>
              <span className="statusbar-dot ok" />
              Ошибок нет
              {warnings.length > 0 && (
                <span className="statusbar-message">
                  · {warnings.length} {plural(warnings.length, ['предупреждение', 'предупреждения', 'предупреждений'])}
                </span>
              )}
            </>
          ) : (
            ''
          )}
        </button>
        <span className="statusbar-item">
          Стр {cursor.line}, стлб {cursor.column}
        </span>
        <span className="statusbar-item hide-narrow">Java 21</span>
      </div>

      <div className="ide-console">
        <div className="tabs" role="tablist">
          <TabButton active={tab === 'console'} onClick={() => setTab('console')}>
            Консоль
            {running && <span className="status-dot running" />}
            {terminal.exit && <StatusDot ok={terminal.exit.status === 'SUCCESS'} />}
          </TabButton>
          {lessonSlug && (
            <TabButton active={tab === 'tests'} onClick={() => setTab('tests')}>
              Тесты
              {checkResult && <StatusDot ok={checkResult.passed} />}
            </TabButton>
          )}
          <Mascot className="ide-mascot" mood={reaction.mood} reactKey={reaction.key} />
        </div>
        <div className="console-body">
          {tab === 'console' && (
            <Terminal state={terminal} onInput={sendInput} onEof={sendEof} onDiagnosticClick={goTo} />
          )}
          {tab === 'tests' && lessonSlug && (
            <div className="tests-panel">
              {checkError && <div className="console-error">{checkError}</div>}
              {checkResult ? (
                <CheckOutput key={reaction.key} result={checkResult} onDiagnosticClick={goTo} />
              ) : (
                <p className="console-placeholder">Нажмите «Проверить», чтобы запустить программу на тестах задания.</p>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function plural(n: number, forms: [string, string, string]): string {
  const mod10 = n % 10;
  const mod100 = n % 100;
  if (mod10 === 1 && mod100 !== 11) return forms[0];
  if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) return forms[1];
  return forms[2];
}

function TabButton({ active, onClick, children }: { active: boolean; onClick: () => void; children: ReactNode }) {
  return (
    <button role="tab" aria-selected={active} className={`tab ${active ? 'tab-active' : ''}`} onClick={onClick}>
      {children}
    </button>
  );
}

function StatusDot({ ok }: { ok: boolean }) {
  return <span className={`status-dot ${ok ? 'ok' : 'fail'}`} aria-label={ok ? 'успешно' : 'ошибка'} />;
}

function PlayIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 16 16" aria-hidden="true">
      <path d="M4 2.5v11l9-5.5z" fill="currentColor" />
    </svg>
  );
}

function StopIcon() {
  return (
    <svg width="12" height="12" viewBox="0 0 16 16" aria-hidden="true">
      <rect x="3" y="3" width="10" height="10" rx="2" fill="currentColor" />
    </svg>
  );
}

function CheckIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 16 16" aria-hidden="true">
      <path
        d="M2.5 8.5l3.5 3.5 7.5-8"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function ResetIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 16 16" aria-hidden="true">
      <path
        d="M3 8a5 5 0 1 0 1.5-3.5M3 2.5v2.5h2.5"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.6"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function firstErrorCode(diagnostics: Diagnostic[]): string | null {
  return diagnostics.find((d) => d.severity === 'ERROR')?.code ?? null;
}

function reveal(editor: MonacoEditor, line: number, column: number) {
  editor.revealLineInCenter(line);
  editor.setPosition({ lineNumber: line, column: Math.max(1, column) });
  editor.focus();
}
