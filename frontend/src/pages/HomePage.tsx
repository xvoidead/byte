import { Link } from 'react-router-dom';
import { api } from '../api';
import { useAsync } from '../components/useAsync';
import { useProgress } from '../components/useProgress';
import type { LessonSummary } from '../types';

export function HomePage() {
  const lessons = useAsync(api.lessons, []);
  const done = useProgress();

  const list = lessons.status === 'ready' ? lessons.data : [];
  const nextLesson = list.find((l) => !done.has(l.slug)) ?? list[0];
  const completed = list.filter((l) => done.has(l.slug)).length;

  return (
    <main className="home">
      <section className="hero">
        <h1>
          Изучайте Java <span className="accent">прямо в браузере</span>
        </h1>
        <p className="hero-lead">
          Короткие уроки, настоящий компилятор и задания с мгновенной проверкой. Ничего не нужно устанавливать —
          откройте урок и пишите код.
        </p>
        <div className="hero-actions">
          {nextLesson && (
            <Link className="btn btn-run btn-lg" to={`/lessons/${nextLesson.slug}`}>
              {completed === 0 ? 'Начать обучение' : 'Продолжить обучение'}
            </Link>
          )}
          <Link className="btn btn-ghost btn-lg" to="/playground">
            Открыть песочницу
          </Link>
        </div>
      </section>

      <section className="features">
        <Feature title="Настоящая Java" text="Код компилируется javac и выполняется на JVM — как на вашем компьютере." />
        <Feature
          title="Редактор как в IDE"
          text="Подсветка синтаксиса, автодополнение, сниппеты sout и psvm, ошибки прямо в коде."
        />
        <Feature
          title="Автопроверка"
          text="Каждое задание проверяется тестами, а ошибки компиляции объясняются по-русски."
        />
      </section>

      <section className="program">
        <div className="program-head">
          <h2>Программа курса</h2>
          {list.length > 0 && (
            <div className="progress" aria-label={`Пройдено ${completed} из ${list.length}`}>
              <div className="progress-bar">
                <div className="progress-fill" style={{ width: `${(completed / list.length) * 100}%` }} />
              </div>
              <span>
                {completed} / {list.length}
              </span>
            </div>
          )}
        </div>
        {lessons.status === 'loading' && <p className="muted">Загружаем уроки…</p>}
        {lessons.status === 'error' && <p className="error-text">{lessons.error}</p>}
        {groupByModule(list).map(([module, items]) => (
          <div key={module} className="module">
            <h3 className="module-title">{module}</h3>
            <div className="lesson-grid">
              {items.map((lesson) => (
                <Link key={lesson.slug} to={`/lessons/${lesson.slug}`} className="lesson-card">
                  <span className={`lesson-num ${done.has(lesson.slug) ? 'done' : ''}`}>
                    {done.has(lesson.slug) ? '✓' : lesson.order}
                  </span>
                  <span>
                    <span className="lesson-card-title">{lesson.title}</span>
                    <span className="lesson-card-summary">{lesson.summary}</span>
                  </span>
                </Link>
              ))}
            </div>
          </div>
        ))}
      </section>

      <footer className="footer">byte · учебный проект · код выполняется в изолированном процессе с ограничением по времени</footer>
    </main>
  );
}

function Feature({ title, text }: { title: string; text: string }) {
  return (
    <div className="feature">
      <h3>{title}</h3>
      <p>{text}</p>
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
