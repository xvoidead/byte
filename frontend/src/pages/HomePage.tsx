import type { CSSProperties, ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api';
import { ConfigShowcase } from '../components/ConfigShowcase';
import { Reveal } from '../components/motion';
import { ShowcaseDemo } from '../components/ShowcaseDemo';
import { useAsync } from '../components/useAsync';
import { useProgress } from '../components/useProgress';
import { useTitle } from '../components/useTitle';
import type { LessonSummary } from '../types';

export function HomePage() {
  useTitle(null);
  const lessons = useAsync(api.lessons, []);
  const done = useProgress();

  const list = lessons.status === 'ready' ? lessons.data : [];
  const nextLesson = list.find((l) => !done.has(l.slug)) ?? list[0];
  const completed = list.filter((l) => done.has(l.slug)).length;
  const track = list.filter((l) => l.module === CONFIG_MODULE);
  const nextInTrack = track.find((l) => !done.has(l.slug)) ?? track[0];

  return (
    <main className="home">
      <section className="hero">
        <div className="hero-glow" aria-hidden="true" />
        <span className="pill intro" style={delay(0)}>
          <span className="pill-dot" />
          Java 21 · без установки
        </span>
        <h1 className="hero-title">
          <span className="hero-line" style={delay(120)}>
            Изучайте Java.
          </span>
          <span className="hero-line gradient-accent" style={delay(300)}>
            Прямо в браузере.
          </span>
        </h1>
        <p className="hero-lead intro" style={delay(520)}>
          Короткие уроки, настоящий компилятор и задания с мгновенной проверкой — от первой строки кода до проектов с
          конфигами и JSON. Откройте урок и пишите код.
        </p>
        <div className="hero-actions intro" style={delay(660)}>
          <Link className="btn btn-primary btn-lg" to={nextLesson ? `/lessons/${nextLesson.slug}` : '/lessons/hello-world'}>
            {completed === 0 ? 'Начать обучение' : 'Продолжить обучение'}
            <ArrowIcon />
          </Link>
          <Link className="btn btn-secondary btn-lg" to="/playground">
            Открыть песочницу
          </Link>
        </div>
      </section>

      <section className="showcase intro" style={delay(820)} aria-label="Пример работы редактора">
        <ShowcaseDemo />
      </section>

      <section className="features">
        <Feature delay={0} icon={<ChipIcon />} title="Настоящая Java">
          Код компилируется javac и выполняется на JVM — так же, как на вашем компьютере.
        </Feature>
        <Feature delay={90} icon={<CursorIcon />} title="Редактор уровня IDE">
          Подсветка, автодополнение с import, сниппеты sout и psvm, ошибки прямо в коде.
        </Feature>
        <Feature delay={180} icon={<ShieldIcon />} title="Мгновенная проверка">
          Каждое задание проверяется тестами, а ошибки компилятора объясняются по-русски.
        </Feature>
      </section>

      <section className="track" aria-labelledby="track-title">
        <Reveal className="track-text">
          <span className="pill">
            <span className="pill-dot" />
            Новый трек
          </span>
          <h2 className="section-title" id="track-title">
            Конфиги и данные
          </h2>
          <p className="section-lead">
            Настоящие программы хранят настройки и данные в файлах. В пяти уроках-проектах вы прочитаете YAML, создадите
            конфиг из настроек по умолчанию, проверите значения, сохраните игроков в JSON и перенесёте старый конфиг на
            новую версию.
          </p>
          <ul className="track-points">
            <li>Несколько классов и файлов во вкладках, как в IDE</li>
            <li>Программа читает и пишет файлы — новые сразу видны во вкладках</li>
            <li>SnakeYAML и Gson уже подключены</li>
          </ul>
          {track.length > 0 && (
            <ol className="track-lessons">
              {track.map((lesson) => (
                <li key={lesson.slug}>
                  <Link to={`/lessons/${lesson.slug}`} className={done.has(lesson.slug) ? 'done' : ''}>
                    <span className="track-lesson-num">{String(lesson.order).padStart(2, '0')}</span>
                    {lesson.title}
                    {done.has(lesson.slug) && <CheckIcon />}
                  </Link>
                </li>
              ))}
            </ol>
          )}
          <Link className="btn btn-secondary" to={`/lessons/${nextInTrack?.slug ?? 'yaml-config'}`}>
            {track.some((l) => done.has(l.slug)) ? 'Продолжить трек' : 'Начать трек'}
            <ArrowIcon />
          </Link>
        </Reveal>
        <Reveal className="track-demo" delay={120}>
          <ConfigShowcase />
        </Reveal>
      </section>

      <section className="program">
        <Reveal className="section-head">
          <div>
            <h2 className="section-title">Программа курса</h2>
            <p className="section-lead">37 уроков: от первой строки кода до Stream API, собственных проектов и работы с конфигами.</p>
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
        </Reveal>

        {lessons.status === 'loading' && <p className="muted">Загружаем уроки…</p>}
        {lessons.status === 'error' && <p className="error-text">{lessons.error}</p>}

        <div className="modules">
          {groupByModule(list).map(([module, items]) => (
            <Reveal key={module} className="module">
              <h3 className="module-title">{module}</h3>
              <div className="module-list">
                {items.map((lesson, index) => {
                  const isDone = done.has(lesson.slug);
                  return (
                    <Link
                      key={lesson.slug}
                      to={`/lessons/${lesson.slug}`}
                      className="lesson-row"
                      style={{ '--i': index } as CSSProperties}
                    >
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
            </Reveal>
          ))}
        </div>
      </section>

      <footer className="footer">
        <span>byte · учимся писать на Java</span>
        <nav className="footer-links">
          <Link to="/playground">Песочница</Link>
          <Link to="/privacy">Конфиденциальность</Link>
          <Link to="/terms">Правила</Link>
        </nav>
      </footer>
    </main>
  );
}

const CONFIG_MODULE = 'Конфиги';

function Feature({ icon, title, delay, children }: { icon: ReactNode; title: string; delay: number; children: ReactNode }) {
  return (
    <Reveal className="feature" delay={delay}>
      <div className="feature-icon">{icon}</div>
      <h3>{title}</h3>
      <p>{children}</p>
    </Reveal>
  );
}

function delay(ms: number): CSSProperties {
  return { '--intro-delay': `${ms}ms` } as CSSProperties;
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
      <path className="check-draw" d="M5 12.5l4.5 4.5L19 7.5" />
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
