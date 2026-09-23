import { useCallback, useEffect, useRef, useState, type ReactNode } from 'react';
import Editor, { type OnMount } from '@monaco-editor/react';
import { monaco } from './monaco';
import { api, ApiError } from '../api';
import { loadCode, loadStdin, saveCode, saveStdin } from '../storage';
import type { CheckResult, Diagnostic, RunResult } from '../types';
import { RunOutput } from './RunOutput';
import { CheckOutput } from './CheckOutput';
import { Mascot, type MascotMood } from '../components/Mascot';

type Tab = 'console' | 'input' | 'tests';
type Busy = 'run' | 'check' | null;

export interface IdeProps {
  /** Ключ для сохранения черновика и ввода в браузере. */
  storageKey: string;
  initialCode: string;
  initialStdin?: string;
  /** Если задан, доступна кнопка «Проверить». */
  lessonSlug?: string;
  onPassed?: () => void;
}

type MonacoEditor = Parameters<OnMount>[0];

export default function Ide({ storageKey, initialCode, initialStdin = '', lessonSlug, onPassed }: IdeProps) {
  const [code, setCode] = useState(() => loadCode(storageKey) ?? initialCode);
  const [stdin, setStdin] = useState(() => loadStdin(storageKey) || initialStdin);
  const [tab, setTab] = useState<Tab>('console');
  const [busy, setBusy] = useState<Busy>(null);
  const [runResult, setRunResult] = useState<RunResult | null>(null);
  const [checkResult, setCheckResult] = useState<CheckResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [reaction, setReaction] = useState<{ mood: MascotMood; key: number }>({ mood: 'idle', key: 0 });
  const editorRef = useRef<MonacoEditor | null>(null);

  const setMood = useCallback((mood: MascotMood) => setReaction((r) => ({ mood, key: r.key + 1 })), []);

  const showDiagnostics = useCallback((diagnostics: Diagnostic[]) => {
    const model = editorRef.current?.getModel();
    if (!model) return;
    monaco.editor.setModelMarkers(
      model,
      'javac',
      diagnostics
        .filter((d) => d.line > 0)
        .map((d) => ({
          severity: d.severity === 'ERROR' ? monaco.MarkerSeverity.Error : monaco.MarkerSeverity.Warning,
          startLineNumber: d.line,
          endLineNumber: d.line,
          startColumn: Math.max(1, d.column),
          endColumn: Math.max(d.column + 1, d.endColumn),
          message: d.hint ? `${d.message}\n\nПодсказка: ${d.hint}` : d.message,
        })),
    );
  }, []);

  const run = useCallback(async () => {
    if (busy) return;
    setBusy('run');
    setError(null);
    setTab('console');
    try {
      const result = await api.run(code, stdin);
      setRunResult(result);
      showDiagnostics(result.diagnostics);
      setMood(result.status === 'SUCCESS' ? 'idle' : 'sad');
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Не удалось выполнить программу.');
    } finally {
      setBusy(null);
    }
  }, [busy, code, stdin, showDiagnostics, setMood]);

  const check = useCallback(async () => {
    if (busy || !lessonSlug) return;
    setBusy('check');
    setError(null);
    setTab('tests');
    try {
      const result = await api.check(lessonSlug, code);
      setCheckResult(result);
      showDiagnostics(result.diagnostics);
      setMood(result.passed ? 'happy' : 'sad');
      if (result.passed) onPassed?.();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Не удалось проверить решение.');
    } finally {
      setBusy(null);
    }
  }, [busy, code, lessonSlug, onPassed, showDiagnostics, setMood]);

  // Горячие клавиши регистрируются один раз, поэтому вызываем актуальные обработчики через ref.
  const actions = useRef({ run, check });
  actions.current = { run, check };

  const handleMount: OnMount = (editor) => {
    editorRef.current = editor;
    editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyCode.Enter, () => actions.current.run());
    editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyMod.Shift | monaco.KeyCode.Enter, () =>
      actions.current.check(),
    );
  };

  const handleChange = (value: string | undefined) => {
    const next = value ?? '';
    setCode(next);
    saveCode(storageKey, next === initialCode ? null : next);
    const model = editorRef.current?.getModel();
    if (model) monaco.editor.setModelMarkers(model, 'javac', []);
  };

  const reset = () => {
    if (code !== initialCode && !window.confirm('Вернуть исходный код? Ваши изменения будут потеряны.')) return;
    setCode(initialCode);
    saveCode(storageKey, null);
    setRunResult(null);
    setCheckResult(null);
    const model = editorRef.current?.getModel();
    if (model) monaco.editor.setModelMarkers(model, 'javac', []);
  };

  const goTo = (line: number, column: number) => {
    const editor = editorRef.current;
    if (!editor || line <= 0) return;
    editor.revealLineInCenter(line);
    editor.setPosition({ lineNumber: line, column: Math.max(1, column) });
    editor.focus();
  };

  useEffect(() => {
    saveStdin(storageKey, stdin === initialStdin ? '' : stdin);
  }, [storageKey, stdin, initialStdin]);

  const isMac = typeof navigator !== 'undefined' && /Mac|iPhone|iPad/.test(navigator.platform);
  const mod = isMac ? '⌘' : 'Ctrl';

  return (
    <div className="ide">
      <div className="ide-toolbar">
        <button className="btn btn-primary btn-sm" onClick={run} disabled={busy !== null} title={`${mod} + Enter`}>
          {busy === 'run' ? <span className="spinner" /> : <PlayIcon />}
          Запустить
        </button>
        {lessonSlug && (
          <button className="btn btn-secondary btn-sm btn-check" onClick={check} disabled={busy !== null} title={`${mod} + Shift + Enter`}>
            {busy === 'check' ? <span className="spinner" /> : <CheckIcon />}
            Проверить
          </button>
        )}
        <span className="ide-hint">
          <kbd>{mod}</kbd>
          <kbd>↵</kbd>
          запуск
        </span>
        <button className="btn btn-ghost btn-sm" onClick={reset} disabled={busy !== null} title="Вернуть исходный код">
          <ResetIcon />
          <span className="hide-narrow">Сбросить</span>
        </button>
      </div>

      <div className="ide-editor">
        <Editor
          language="java"
          theme="byte-dark"
          value={code}
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
          }}
        />
      </div>

      <div className="ide-console">
        <div className="tabs" role="tablist">
          <TabButton active={tab === 'console'} onClick={() => setTab('console')}>
            Консоль
            {runResult && <StatusDot ok={runResult.status === 'SUCCESS'} />}
          </TabButton>
          <TabButton active={tab === 'input'} onClick={() => setTab('input')}>
            Ввод{stdin.trim() && <span className="tab-badge" />}
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
          {error && <div className="console-error">{error}</div>}
          {tab === 'console' &&
            (runResult ? (
              <RunOutput key={reaction.key} result={runResult} onDiagnosticClick={goTo} />
            ) : (
              <p className="console-placeholder">
                Нажмите «Запустить», чтобы скомпилировать и выполнить программу. Результат появится здесь.
              </p>
            ))}
          {tab === 'input' && (
            <div className="stdin">
              <label htmlFor={`stdin-${storageKey}`} className="stdin-label">
                Данные, которые программа прочитает из System.in:
              </label>
              <textarea
                id={`stdin-${storageKey}`}
                className="stdin-area"
                value={stdin}
                onChange={(e) => setStdin(e.target.value)}
                placeholder="Например: 2 3"
                spellCheck={false}
              />
            </div>
          )}
          {tab === 'tests' &&
            lessonSlug &&
            (checkResult ? (
              <CheckOutput key={reaction.key} result={checkResult} onDiagnosticClick={goTo} />
            ) : (
              <p className="console-placeholder">
                Нажмите «Проверить», чтобы запустить программу на тестах задания.
              </p>
            ))}
        </div>
      </div>
    </div>
  );
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

function CheckIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 16 16" aria-hidden="true">
      <path d="M2.5 8.5l3.5 3.5 7.5-8" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function ResetIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 16 16" aria-hidden="true">
      <path d="M3 8a5 5 0 1 0 1.5-3.5M3 2.5v2.5h2.5" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}
