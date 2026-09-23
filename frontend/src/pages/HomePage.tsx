import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api';
import { highlightJava } from '../components/highlightJava';
import { useAsync } from '../components/useAsync';
import { useProgress } from '../components/useProgress';
import type { LessonSummary } from '../types';

const SHOWCASE_CODE = `import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        String name = in.nextLine();
        System.out.println("Привет, " + name + "!");
    }
}`;

export function HomePage() {
  const lessons = useAsync(api.lessons, []);
  const done = useProgress();

  const list = lessons.status === 'ready' ? lessons.data : [];
  const nextLesson = list.find((l) => !done.has(l.slug)) ?? list[0];
  const completed = list.filter((l) => done.has(l.slug)).length;

  return (
    <main className="home">
      <section className="hero">
        <div className="hero-glow" aria-hidden="true" />
        <span className="pill">
          <span className="pill-dot" />
          Java 21 · без установки
        </span>
        <h1 className="hero-title">
          Изучайте Java.
          <br />
          <span className="gradient-accent">Прямо в браузере.</span>
        </h1>
        <p className="hero-lead">
          Короткие уроки, настоящий компилятор и задания с мгновенной проверкой. Откройте урок — и пишите код.
        </p>
        <div className="hero-actions">
          <Link className="btn btn-primary btn-lg" to={nextLesson ? `/lessons/${nextLesson.slug}` : '/lessons/hello-world'}>
            {completed === 0 ? 'Начать обучение' : 'Продолжить обучение'}
            <ArrowIcon />
          </Link>
          <Link className="btn btn-secondary btn-lg" to="/playground">
            Открыть песочницу
          </Link>
        </div>
      </section>

      <section className="showcase" aria-label="Пример работы редактора">
        <img className="showcase-mascot" src="/mascot.svg" alt="" />
        <div className="window">
          <div className="window-bar">
            <span className="window-dots">
              <i />
              <i />
              <i />
            </span>
            <span className="window-tab">Main.java</span>
            <span className="window-run">
              <PlayIcon /> Запустить
            </span>
          </div>
          <div className="window-body">
            <pre className="window-code">
              {SHOWCASE_CODE.split('\n').map((line, i) => (
                <div key={i} className="code-line">
                  <span className="code-ln">{i + 1}</span>
                  <code>
                    {highlightJava(line).map((t, j) =>
                      t.kind ? (
                        <span key={j} className={`tok-${t.kind}`}>
                          {t.text}
                        </span>
                      ) : (
                        t.text
                      ),
                    )}
                  </code>
                </div>
              ))}
            </pre>
            <div className="window-console">
              <div className="window-console-head">Консоль</div>
              <div className="window-console-line muted">&gt; Ада</div>
              <div className="window-console-line">Привет, Ада!</div>
              <div className="window-console-ok">
                <CheckIcon /> Все тесты пройдены · 3 из 3
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="features">
        <Feature icon={<ChipIcon />} title="Настоящая Java">
          Код компилируется javac и выполняется на JVM — так же, как на вашем компьютере.
        </Feature>
        <Feature icon={<CursorIcon />} title="Редактор уровня IDE">
          Подсветка, автодополнение с import, сниппеты sout и psvm, ошибки прямо в коде.
        </Feature>
        <Feature icon={<ShieldIcon />} title="Мгновенная проверка">
          Каждое задание проверяется тестами, а ошибки компилятора объясняются по-русски.
        </Feature>
      </section>

      <section className="program">
        <div className="section-head">
          <div>
            <h2 className="section-title">Программа курса</h2>
            <p className="section-lead">От первой строки кода до классов и коллекций.</p>
          </div>
          {list.length > 0 && (
            <div className="progress" aria-label={`Пройдено ${completed} из ${list.length}`}>
              <span className="progress-label">
                {completed} из {list.length}
              </span>
              <div className="progress-bar">
                <div className="progress-fill" style={{ width: `${(completed / list.length) * 100}%` }} />
              </div>
            </div>
          )}
        </div>

        {lessons.status === 'loading' && <p className="muted">Загружаем уроки…</p>}
        {lessons.status === 'error' && <p className="error-text">{lessons.error}</p>}

        <div className="modules">
          {groupByModule(list).map(([module, items]) => (
            <div key={module} className="module">
              <h3 className="module-title">{module}</h3>
              <div className="module-list">
                {items.map((lesson) => {
                  const isDone = done.has(lesson.slug);
                  return (
                    <Link key={lesson.slug} to={`/lessons/${lesson.slug}`} className="lesson-row">
                      <span className="lesson-row-num">{String(lesson.order).padStart(2, '0')}</span>
                      <span className="lesson-row-text">
                        <span className="lesson-row-title">{lesson.title}</span>
                        <span className="lesson-row-summary">{lesson.summary}</span>
                      </span>
                      <span className={`lesson-row-status ${isDone ? 'done' : ''}`}>
                        {isDone ? <CheckIcon /> : <ArrowIcon />}
                      </span>
                    </Link>
                  );
                })}
              </div>
            </div>
          ))}
        </div>
      </section>

      <footer className="footer">
        <span>byte</span>
        <span className="muted">Код выполняется в изолированном процессе с ограничением по времени.</span>
      </footer>
    </main>
  );
}

function Feature({ icon, title, children }: { icon: ReactNode; title: string; children: ReactNode }) {
  return (
    <div className="feature">
      <div className="feature-icon">{icon}</div>
      <h3>{title}</h3>
      <p>{children}</p>
    </div>
  );
}

function groupByModule(lessons: LessonSummary[]): [string, LessonSummary[]][] {
  const groups = new Map<string, LessonSummary[]>();
  for (const lesson of lessons) {
    const group = groups.get(lesson.module) ?? [];
    group.push(lesson);
    groups.set(lesson.module, group);
  }
  return [...groups.entries()];
}

const iconProps = {
  width: 18,
  height: 18,
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.6,
  strokeLinecap: 'round' as const,
  strokeLinejoin: 'round' as const,
  'aria-hidden': true,
};

function ArrowIcon() {
  return (
    <svg {...iconProps} width={16} height={16}>
      <path d="M5 12h14M13 6l6 6-6 6" />
    </svg>
  );
}

function CheckIcon() {
  return (
    <svg {...iconProps} width={16} height={16}>
      <path d="M5 12.5l4.5 4.5L19 7.5" />
    </svg>
  );
}

function PlayIcon() {
  return (
    <svg width="10" height="10" viewBox="0 0 16 16" aria-hidden="true">
      <path d="M4 2.5v11l9-5.5z" fill="currentColor" />
    </svg>
  );
}

function ChipIcon() {
  return (
    <svg {...iconProps}>
      <rect x="6" y="6" width="12" height="12" rx="2" />
      <path d="M9 2v4M15 2v4M9 18v4M15 18v4M2 9h4M2 15h4M18 9h4M18 15h4" />
    </svg>
  );
}

function CursorIcon() {
  return (
    <svg {...iconProps}>
      <path d="M8 5l-5 7 5 7M16 5l5 7-5 7M13.5 4l-3 16" />
    </svg>
  );
}

function ShieldIcon() {
  return (
    <svg {...iconProps}>
      <path d="M12 3l8 3v6c0 4.5-3.4 8.3-8 9-4.6-.7-8-4.5-8-9V6z" />
      <path d="M8.5 12l2.5 2.5 4.5-5" />
    </svg>
  );
}
