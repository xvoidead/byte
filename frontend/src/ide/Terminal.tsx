import { useEffect, useLayoutEffect, useRef, useState, type ClipboardEvent, type KeyboardEvent } from 'react';
import type { Diagnostic, RunStatus } from '../types';
import { DiagnosticList, runtimeHint, statusText } from './RunOutput';

export type SegmentKind = 'stdout' | 'stderr' | 'stdin' | 'system';

export interface Segment {
  kind: SegmentKind;
  text: string;
}

export type TerminalPhase = 'idle' | 'compiling' | 'running' | 'done';

export interface TerminalState {
  phase: TerminalPhase;
  segments: Segment[];
  compileErrors: Diagnostic[];
  exit: { status: RunStatus; exitCode: number | null; timeMs: number; compileTimeMs: number } | null;
  error: string | null;
}

export const EMPTY_TERMINAL: TerminalState = {
  phase: 'idle',
  segments: [],
  compileErrors: [],
  exit: null,
  error: null,
};

/** Добавляет вывод, склеивая соседние куски одного вида — так DOM остаётся небольшим. */
export function appendSegment(segments: Segment[], kind: SegmentKind, text: string): Segment[] {
  if (!text) return segments;
  const last = segments[segments.length - 1];
  if (last && last.kind === kind) {
    return [...segments.slice(0, -1), { kind, text: last.text + text }];
  }
  return [...segments, { kind, text }];
}

interface TerminalProps {
  state: TerminalState;
  onInput: (line: string) => void;
  onEof: () => void;
  onDiagnosticClick: (line: number, column: number) => void;
}

/**
 * Консоль как в IDE: вывод программы появляется по мере работы, а когда программа ждёт ввода,
 * его можно набрать прямо здесь — поле ввода стоит сразу после приглашения.
 */
export function Terminal({ state, onInput, onEof, onDiagnosticClick }: TerminalProps) {
  const [value, setValue] = useState('');
  const scrollRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const stickToBottom = useRef(true);
  const running = state.phase === 'running';

  useLayoutEffect(() => {
    const el = scrollRef.current;
    if (el && stickToBottom.current) el.scrollTop = el.scrollHeight;
  }, [state.segments, state.phase, state.exit]);

  useEffect(() => {
    if (running) inputRef.current?.focus({ preventScroll: true });
    else setValue('');
  }, [running]);

  const submit = (text: string) => {
    onInput(text);
    setValue('');
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      submit(value + '\n');
    } else if (e.key === 'd' && (e.ctrlKey || e.metaKey) && !value) {
      e.preventDefault();
      onEof();
    }
  };

  const handlePaste = (e: ClipboardEvent<HTMLInputElement>) => {
    const pasted = e.clipboardData.getData('text');
    if (!pasted.includes('\n')) return;
    e.preventDefault();
    const text = (value + pasted).replace(/\r\n/g, '\n');
    submit(text.endsWith('\n') ? text : text + '\n');
  };

  const hint =
    state.exit?.status === 'TIMEOUT'
      ? 'Программа работала слишком долго и была остановлена. Проверьте, нет ли бесконечного цикла.'
      : state.exit?.status === 'OUTPUT_LIMIT'
        ? 'Вывод обрезан. Возможно, программа печатает в бесконечном цикле.'
        : state.exit && state.exit.status !== 'SUCCESS'
          ? runtimeHint(state.segments.filter((s) => s.kind === 'stderr').map((s) => s.text).join(''))
          : undefined;

  return (
    <div
      className="term"
      ref={scrollRef}
      onScroll={(e) => {
        const el = e.currentTarget;
        stickToBottom.current = el.scrollHeight - el.scrollTop - el.clientHeight < 24;
      }}
      onMouseUp={() => {
        if (running && !window.getSelection()?.toString()) inputRef.current?.focus({ preventScroll: true });
      }}
    >
      {state.error && <div className="console-error">{state.error}</div>}

      {state.phase === 'idle' && !state.error && (
        <p className="console-placeholder">
          Нажмите «Запустить». Если программа попросит ввести данные, наберите их прямо здесь и нажмите Enter.
        </p>
      )}

      {state.phase === 'compiling' && (
        <p className="term-system">
          <span className="spinner" /> Компиляция…
        </p>
      )}

      {state.compileErrors.length > 0 && (
        <div className="run-output">
          <div className="run-status fail">Ошибка компиляции</div>
          <DiagnosticList diagnostics={state.compileErrors} onClick={onDiagnosticClick} />
        </div>
      )}

      {(state.segments.length > 0 || running) && (
        <pre className="term-output" aria-live="polite">
          {state.segments.map((segment, i) => (
            <span key={i} className={`term-${segment.kind}`}>
              {segment.text}
            </span>
          ))}
          {running && (
            <input
              ref={inputRef}
              className="term-input"
              value={value}
              onChange={(e) => setValue(e.target.value)}
              onKeyDown={handleKeyDown}
              onPaste={handlePaste}
              spellCheck={false}
              autoComplete="off"
              aria-label="Ввод для программы"
              placeholder="ввод…"
            />
          )}
        </pre>
      )}

      {running && (
        <div className="term-footer">
          <span className="term-running">
            <span className="term-pulse" /> Программа работает
          </span>
          <button className="term-link" onClick={onEof} title="Сообщить программе, что ввод закончен (Ctrl + D)">
            Конец ввода
          </button>
        </div>
      )}

      {state.exit && (
        <div className={`run-status term-exit ${state.exit.status === 'SUCCESS' ? 'ok' : 'fail'}`}>
          <span>{statusText(state.exit.status)}</span>
          <span className="run-time">
            компиляция {state.exit.compileTimeMs} мс · выполнение {state.exit.timeMs} мс
            {state.exit.exitCode !== null && state.exit.exitCode !== 0 && <> · код выхода {state.exit.exitCode}</>}
          </span>
        </div>
      )}
      {state.exit?.status === 'SUCCESS' && state.segments.length === 0 && (
        <p className="console-placeholder">Программа ничего не вывела.</p>
      )}
      {hint && <p className="run-hint">{hint}</p>}
    </div>
  );
}
