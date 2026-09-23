import { useEffect, useMemo, useState } from 'react';
import { highlightJava, type Token } from './highlightJava';
import { Mascot } from './Mascot';
import { useInView, useReducedMotion } from './motion';

const PREFIX = `import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        `;
const TYPED = `Scanner in = new Scanner(System.in);
        String name = in.nextLine();
        System.out.println("Привет, " + name + "!");`;
const SUFFIX = `
    }
}`;
const TOTAL_LINES = (PREFIX + TYPED + SUFFIX).split('\n').length;

type Phase = 'idle' | 'typing' | 'compiling' | 'output' | 'success' | 'fading';

interface DemoState {
  typed: number;
  phase: Phase;
  pressed: boolean;
  outputLines: number;
  loop: number;
}

const FINAL: DemoState = { typed: TYPED.length, phase: 'success', pressed: false, outputLines: 2, loop: 0 };
const START: DemoState = { typed: 0, phase: 'idle', pressed: false, outputLines: 0, loop: 0 };

/** Позиции, до которых «набирается» код: после перевода строки отступ появляется сразу, как в IDE. */
function typingFrames(): number[] {
  const frames: number[] = [];
  let i = 0;
  while (i < TYPED.length) {
    i++;
    if (TYPED[i - 1] === '\n') {
      while (TYPED[i] === ' ') i++;
    }
    frames.push(i);
  }
  return frames;
}

function keystrokeDelay(char: string): number {
  if (char === '\n') return 200;
  if (char === ' ') return 16;
  if ('.;(),"'.includes(char)) return 45 + Math.random() * 40;
  return 18 + Math.random() * 26;
}

export function ShowcaseDemo() {
  const reduced = useReducedMotion();
  const [ref, inView] = useInView<HTMLDivElement>({ threshold: 0.35, rootMargin: '0px' });
  const [state, setState] = useState<DemoState>(START);
  const frames = useMemo(typingFrames, []);

  useEffect(() => {
    if (reduced || !inView) return;
    let cancelled = false;
    const timers: number[] = [];
    const sleep = (ms: number) =>
      new Promise<void>((resolve) => {
        timers.push(window.setTimeout(resolve, ms));
      });
    const update = (patch: Partial<DemoState>) => !cancelled && setState((s) => ({ ...s, ...patch }));

    (async () => {
      for (let loop = 1; !cancelled; loop++) {
        update({ ...START, loop, phase: 'typing' });
        await sleep(500);
        for (const frame of frames) {
          if (cancelled) return;
          update({ typed: frame });
          await sleep(keystrokeDelay(TYPED[frame - 1]));
        }
        await sleep(650);
        update({ pressed: true });
        await sleep(200);
        update({ pressed: false, phase: 'compiling' });
        await sleep(950);
        update({ phase: 'output', outputLines: 1 });
        await sleep(420);
        update({ outputLines: 2 });
        await sleep(520);
        update({ phase: 'success' });
        await sleep(4200);
        update({ phase: 'fading' });
        await sleep(650);
      }
    })();

    return () => {
      cancelled = true;
      timers.forEach(clearTimeout);
    };
  }, [inView, reduced, frames]);

  const view = reduced ? FINAL : state;
  const typedText = PREFIX + TYPED.slice(0, view.typed);
  const lines = (typedText + SUFFIX).split('\n');
  const caretLine = typedText.split('\n').length - 1;
  const showCaret = view.phase === 'idle' || view.phase === 'typing';
  const consoleVisible = view.phase === 'output' || view.phase === 'success';

  return (
    <div ref={ref} className={`showcase-stage phase-${view.phase}`}>
      <Mascot
        className="showcase-mascot"
        mood={view.phase === 'success' ? 'happy' : 'idle'}
        reactKey={view.phase === 'success' ? `happy-${view.loop}` : 'idle'}
      />
      <div className="window">
        <div className="window-bar">
          <span className="window-dots">
            <i />
            <i />
            <i />
          </span>
          <span className="window-tab">Main.java</span>
          <span className={`window-run ${view.pressed ? 'is-pressed' : ''} ${view.phase === 'compiling' ? 'is-busy' : ''}`}>
            {view.phase === 'compiling' ? <span className="spinner" /> : <PlayIcon />}
            Запустить
          </span>
        </div>
        <div className="window-body">
          <pre className="window-code" style={{ minHeight: `calc(${TOTAL_LINES} * 1.75em + 40px)` }}>
            {lines.map((line, i) => (
              <div key={i} className={`code-line ${showCaret && i === caretLine ? 'is-active' : ''}`}>
                <span className="code-ln">{i + 1}</span>
                <code>
                  <Tokens tokens={highlightJava(line)} />
                  {showCaret && i === caretLine && <span className="demo-caret" />}
                </code>
              </div>
            ))}
          </pre>
          <div className="window-console">
            <div className="window-console-head">Консоль</div>
            {view.phase === 'compiling' && (
              <div className="window-console-line muted demo-fade-in">Компиляция…</div>
            )}
            {(view.phase === 'idle' || view.phase === 'typing') && (
              <div className="window-console-line faint">Нажмите «Запустить»</div>
            )}
            <div className={`demo-output ${view.phase === 'fading' ? 'is-fading' : ''}`}>
              {(consoleVisible || view.phase === 'fading') && view.outputLines >= 1 && (
                <div className="window-console-line muted demo-line-in">&gt; Ада</div>
              )}
              {(consoleVisible || view.phase === 'fading') && view.outputLines >= 2 && (
                <div className="window-console-line demo-line-in">Привет, Ада!</div>
              )}
              {(view.phase === 'success' || view.phase === 'fading') && (
                <div className="window-console-ok demo-pop">
                  <CheckIcon /> Все тесты пройдены · 3 из 3
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function Tokens({ tokens }: { tokens: Token[] }) {
  return (
    <>
      {tokens.map((t, j) =>
        t.kind ? (
          <span key={j} className={`tok-${t.kind}`}>
            {t.text}
          </span>
        ) : (
          t.text
        ),
      )}
    </>
  );
}

function PlayIcon() {
  return (
    <svg width="10" height="10" viewBox="0 0 16 16" aria-hidden="true">
      <path d="M4 2.5v11l9-5.5z" fill="currentColor" />
    </svg>
  );
}

function CheckIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path className="check-draw" d="M5 12.5l4.5 4.5L19 7.5" />
    </svg>
  );
}
