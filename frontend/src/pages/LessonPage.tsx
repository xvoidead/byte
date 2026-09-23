import { lazy, Suspense, useCallback } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api';
import { Markdown } from '../components/Markdown';
import { useAsync } from '../components/useAsync';
import { useProgress } from '../components/useProgress';
import { useTitle } from '../components/useTitle';
import { markCompleted } from '../storage';
import type { LessonDetails } from '../types';

const Ide = lazy(() => import('../ide/Ide'));

export function LessonPage() {
  const { slug = '' } = useParams();
  const lesson = useAsync(() => api.lesson(slug), [slug]);
  useTitle(lesson.status === 'ready' ? `${lesson.data.title} — урок ${lesson.data.order} · byte` : null);

  if (lesson.status === 'loading') return <div className="page-state">Загружаем урок…</div>;
  if (lesson.status === 'error') {
    return (
      <div className="page-state">
        <p className="error-text">{lesson.error}</p>
        <Link to="/">← К списку уроков</Link>
      </div>
    );
  }
  return <Lesson key={slug} lesson={lesson.data} />;
}

function Lesson({ lesson }: { lesson: LessonDetails }) {
  const done = useProgress();
  const completed = done.has(lesson.slug);
  const onPassed = useCallback(() => markCompleted(lesson.slug), [lesson.slug]);

  return (
    <main className="lesson-layout">
      <article className="lesson-content">
        <div className="lesson-meta">
          <Link to="/">Курс</Link> · {lesson.module} · Урок {lesson.order}
          {completed && <span className="badge-done">Пройден</span>}
        </div>
        <h1>{lesson.title}</h1>
        <a className="jump-to-task" href="#task">
          К заданию ↓
        </a>
        <Markdown>{lesson.theory}</Markdown>

        <section id="task" className="task">
          <h2>Задание</h2>
          <Markdown>{lesson.task}</Markdown>
          {lesson.examples.map((example, i) => (
            <div key={i} className="example">
              <div className="example-title">Пример {i + 1}</div>
              <div className="example-io">
                <div>
                  <div className="example-label">Ввод</div>
                  <pre>{example.stdin || <em className="muted">(нет)</em>}</pre>
                </div>
                <div>
                  <div className="example-label">Вывод</div>
                  <pre>{example.expectedOutput}</pre>
                </div>
              </div>
            </div>
          ))}
          <p className="muted small">
            Всего тестов: {lesson.testCount}
            {lesson.testCount > lesson.examples.length && ', часть из них скрыта'}.
          </p>
        </section>

        <nav className="lesson-nav">
          {lesson.prev ? <Link to={`/lessons/${lesson.prev}`}>← Предыдущий урок</Link> : <span />}
          {lesson.next ? (
            <Link to={`/lessons/${lesson.next}`} className={completed ? 'btn btn-primary btn-sm' : undefined}>
              Следующий урок →
            </Link>
          ) : (
            <Link to="/playground">В песочницу →</Link>
          )}
        </nav>
      </article>

      <section className="lesson-ide" aria-label="Редактор кода">
        <Suspense fallback={<div className="ide-loading">Загружаем редактор…</div>}>
          <Ide
            storageKey={`lesson:${lesson.slug}`}
            initialCode={lesson.starterCode}
            initialStdin={lesson.examples[0]?.stdin ?? ''}
            lessonSlug={lesson.slug}
            onPassed={onPassed}
          />
        </Suspense>
      </section>
    </main>
  );
}
