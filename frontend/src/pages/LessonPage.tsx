import { lazy, Suspense, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { track } from '../analytics';
import { api, ApiError } from '../api';
import { Markdown } from '../components/Markdown';
import { QuizCard } from '../components/QuizCard';
import { Stepper } from '../components/Stepper';
import { highlightJava } from '../components/highlightJava';
import { useAsync } from '../components/useAsync';
import { useProgress } from '../components/useProgress';
import { useTitle } from '../components/useTitle';
import {
  completedLessons,
  loadLessonState,
  markCompleted,
  saveLessonState,
  saveProject,
  type LessonState,
} from '../storage';
import type { CheckResult, Example, LessonDetails, ProjectFile } from '../types';

const Ide = lazy(() => import('../ide/Ide'));

/** Сколько неудачных проверок нужно, чтобы разбор решения открылся без решения задачи. */
const CHECKS_BEFORE_SOLUTION = 3;

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
  const [searchParams, setSearchParams] = useSearchParams();
  const titles = [...lesson.steps.map((s) => s.title), 'Задание'];
  const taskIndex = titles.length - 1;
  // Шаг из ссылки (?step=3) важнее сохранённого — чтобы работали ссылки на конкретный шаг.
  const [state, setState] = useState<LessonState>(() => {
    const saved = loadLessonState(lesson.slug);
    const fromUrl = Number(searchParams.get('step'));
    if (!Number.isInteger(fromUrl) || fromUrl < 1 || fromUrl > titles.length) return saved;
    const step = fromUrl - 1;
    return { ...saved, step, visited: saved.visited.includes(step) ? saved.visited : [...saved.visited, step] };
  });
  const [direction, setDirection] = useState<'forward' | 'back'>('forward');
  const [ideVersion, setIdeVersion] = useState(0);
  const contentRef = useRef<HTMLElement>(null);

  const current = Math.min(state.step, taskIndex);
  // Номер шага для обработчиков: при быстрых нажатиях стрелок React ещё не успел перерисовать страницу.
  const currentRef = useRef(current);
  currentRef.current = current;

  const update = useCallback(
    (change: (s: LessonState) => LessonState) =>
      setState((s) => {
        const next = change(s);
        saveLessonState(lesson.slug, next);
        return next;
      }),
    [lesson.slug],
  );

  const goTo = useCallback(
    (index: number) => {
      const target = Math.max(0, Math.min(taskIndex, index));
      setDirection(target >= currentRef.current ? 'forward' : 'back');
      currentRef.current = target;
      update((s) => ({ ...s, step: target, visited: s.visited.includes(target) ? s.visited : [...s.visited, target] }));
      contentRef.current?.scrollTo({ top: 0, behavior: 'smooth' });
    },
    [taskIndex, update],
  );

  useEffect(() => track('lesson_open', lesson.slug), [lesson.slug]);
  useEffect(() => {
    track('step_view', lesson.slug, current === taskIndex ? 'task' : String(current));
  }, [lesson.slug, current, taskIndex]);

  // Адрес страницы следует за шагом, чтобы ссылкой можно было поделиться.
  useEffect(() => {
    if (searchParams.get('step') !== String(current + 1)) {
      setSearchParams({ step: String(current + 1) }, { replace: true });
    }
  }, [current, searchParams, setSearchParams]);

  // Стрелки ← → листают шаги, если фокус не в редакторе и не в поле ввода.
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const target = e.target as HTMLElement;
      if (target.closest('.monaco-editor, input, textarea, .term') || e.altKey || e.ctrlKey || e.metaKey) return;
      if (e.key === 'ArrowRight') goTo(currentRef.current + 1);
      if (e.key === 'ArrowLeft') goTo(currentRef.current - 1);
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [goTo]);

  const onPassed = useCallback(() => {
    if (!completedLessons().has(lesson.slug)) track('lesson_complete', lesson.slug);
    markCompleted(lesson.slug);
  }, [lesson.slug]);
  const onChecked = useCallback(
    (result: CheckResult) => {
      track('check', lesson.slug, null, result.passed ? 1 : 0);
      if (!result.compiled) {
        const error = result.diagnostics.find((d) => d.severity === 'ERROR');
        if (error?.code) track('compile_error', lesson.slug, error.code);
      }
      new Set(result.tests.filter((t) => !t.passed).map((t) => t.name)).forEach((name) =>
        track('test_failed', lesson.slug, name),
      );
      result.requirements.filter((r) => !r.passed).forEach((r) => track('requirement_failed', lesson.slug, r.message));
      if (!result.passed) update((s) => ({ ...s, failedChecks: s.failedChecks + 1 }));
    },
    [lesson.slug, update],
  );
  const onRun = useCallback(
    (status: string, errorCode: string | null) => {
      track('run', lesson.slug, status);
      if (errorCode) track('compile_error', lesson.slug, errorCode);
    },
    [lesson.slug],
  );

  const step = current < taskIndex ? lesson.steps[current] : null;
  const initialFiles = useMemo<ProjectFile[]>(
    () =>
      lesson.starterFiles?.length ? lesson.starterFiles : [{ name: 'Main.java', content: lesson.starterCode }],
    [lesson],
  );

  return (
    <main className="lesson-layout">
      <article className="lesson-content" ref={contentRef}>
        <div className="lesson-meta">
          <Link to="/">Курс</Link> · {lesson.module} · Урок {lesson.order}
          {completed && <span className="badge-done">Пройден</span>}
        </div>
        <h1>{lesson.title}</h1>
        <Stepper titles={titles} current={current} visited={state.visited} onSelect={goTo} />

        <div className={`step step-${direction}`} key={current}>
          {step ? (
            <>
              <h2 className="step-title">{step.title}</h2>
              {step.blocks.map((block, i) =>
                block.type === 'text' ? (
                  <Markdown key={i}>{block.markdown}</Markdown>
                ) : (
                  <QuizCard
                    key={i}
                    quiz={block.quiz}
                    state={state.quizzes[`${current}-${i}`]}
                    onAnswer={(choice) => {
                      track('quiz_answer', lesson.slug, `${current}-${i}`, choice === block.quiz.answer ? 1 : 0);
                      update((s) => {
                        const key = `${current}-${i}`;
                        const previous = s.quizzes[key];
                        return {
                          ...s,
                          quizzes: {
                            ...s.quizzes,
                            [key]: {
                              choice,
                              correct: choice === block.quiz.answer,
                              attempts: (previous?.attempts ?? 0) + 1,
                            },
                          },
                        };
                      });
                    }}
                  />
                ),
              )}
            </>
          ) : (
            <TaskStep
              lesson={lesson}
              state={state}
              completed={completed}
              onRevealHint={() => {
                track('hint_open', lesson.slug, null, Math.min(lesson.hints.length, state.hints + 1));
                update((s) => ({ ...s, hints: Math.min(lesson.hints.length, s.hints + 1) }));
              }}
              onSolutionShown={() => {
                track('solution_open', lesson.slug, null, completed ? 1 : 0);
                update((s) => ({ ...s, solutionShown: true }));
              }}
              onInsertSolution={(files) => {
                saveProject(`lesson:${lesson.slug}`, { files, active: lesson.activeFile ?? 'Main.java' });
                setIdeVersion((v) => v + 1);
              }}
            />
          )}
        </div>

        <nav className="step-nav">
          {current > 0 ? (
            <button className="btn btn-ghost btn-sm" onClick={() => goTo(current - 1)}>
              ← Назад
            </button>
          ) : lesson.prev ? (
            <Link className="btn btn-ghost btn-sm" to={`/lessons/${lesson.prev}`}>
              ← Предыдущий урок
            </Link>
          ) : (
            <span />
          )}
          {current < taskIndex ? (
            <button className="btn btn-primary btn-sm" onClick={() => goTo(current + 1)}>
              {current === taskIndex - 1 ? 'К заданию' : 'Далее'} →
            </button>
          ) : lesson.next ? (
            <Link className={`btn btn-sm ${completed ? 'btn-primary' : 'btn-secondary'}`} to={`/lessons/${lesson.next}`}>
              Следующий урок →
            </Link>
          ) : (
            <Link className="btn btn-secondary btn-sm" to="/playground">
              В песочницу →
            </Link>
          )}
        </nav>
      </article>

      <section className="lesson-ide" aria-label="Редактор кода">
        <Suspense fallback={<div className="ide-loading">Загружаем редактор…</div>}>
          <Ide
            key={ideVersion}
            storageKey={`lesson:${lesson.slug}`}
            initialFiles={initialFiles}
            initialActiveFile={lesson.activeFile ?? undefined}
            lessonSlug={lesson.slug}
            onPassed={onPassed}
            onChecked={onChecked}
            onRun={onRun}
          />
        </Suspense>
      </section>
    </main>
  );
}

interface TaskStepProps {
  lesson: LessonDetails;
  state: LessonState;
  completed: boolean;
  onRevealHint: () => void;
  onSolutionShown: () => void;
  onInsertSolution: (files: ProjectFile[]) => void;
}

function TaskStep({ lesson, state, completed, onRevealHint, onSolutionShown, onInsertSolution }: TaskStepProps) {
  const [solution, setSolution] = useState<ProjectFile[] | null>(null);
  const [solutionError, setSolutionError] = useState<string | null>(null);
  const allHints = state.hints >= lesson.hints.length;
  const solutionAvailable = completed || (allHints && state.failedChecks >= CHECKS_BEFORE_SOLUTION);

  const showSolution = async () => {
    if (!completed && !window.confirm('Посмотреть решение? Попробуйте сначала решить сами — так запомнится лучше.')) return;
    try {
      const loaded = await api.solution(lesson.slug);
      setSolution(loaded.files?.length ? loaded.files : [{ name: 'Main.java', content: loaded.code }]);
      setSolutionError(null);
      onSolutionShown();
    } catch (e) {
      setSolutionError(e instanceof ApiError ? e.message : 'Не удалось загрузить решение.');
    }
  };

  return (
    <section className="task" id="task">
      <h2>Задание</h2>
      <Markdown>{lesson.task}</Markdown>

      {lesson.requirements.length > 0 && (
        <div className="task-requirements">
          <div className="task-subtitle">Решение должно</div>
          <ul>
            {lesson.requirements.map((r, i) => (
              <li key={i}>{r}</li>
            ))}
          </ul>
        </div>
      )}

      {lesson.examples.map((example, i) => (
        <ExampleView key={i} example={example} index={i + 1} />
      ))}
      <p className="muted small">
        Всего тестов: {lesson.testCount}
        {lesson.testCount > lesson.examples.length && ', часть из них скрыта и меняется от проверки к проверке'}.
      </p>

      {lesson.hints.length > 0 && (
        <div className="hints">
          {lesson.hints.slice(0, state.hints).map((hint, i) => (
            <div key={i} className="hint">
              <div className="hint-label">Подсказка {i + 1}</div>
              <Markdown>{hint}</Markdown>
            </div>
          ))}
          {!allHints && (
            <button className="btn btn-secondary btn-sm" onClick={onRevealHint}>
              <BulbIcon />
              {state.hints === 0 ? 'Нужна подсказка' : `Ещё подсказка (${state.hints + 1} из ${lesson.hints.length})`}
            </button>
          )}
        </div>
      )}

      <div className="solution">
        {solution ? (
          <>
            <div className="task-subtitle">Разбор: эталонное решение</div>
            {solution.map((file) => (
              <div key={file.name} className="solution-file">
                {solution.length > 1 && <div className="solution-file-name">{file.name}</div>}
                <pre className="solution-code">
                  <code>
                    {file.name.endsWith('.java')
                      ? highlightJava(file.content).map((t, i) =>
                          t.kind ? (
                            <span key={i} className={`tok-${t.kind}`}>
                              {t.text}
                            </span>
                          ) : (
                            t.text
                          ),
                        )
                      : file.content}
                  </code>
                </pre>
              </div>
            ))}
            <button
              className="btn btn-ghost btn-sm"
              onClick={() => {
                if (window.confirm('Заменить код в редакторе эталонным решением?')) onInsertSolution(solution);
              }}
            >
              Вставить в редактор
            </button>
          </>
        ) : solutionAvailable ? (
          <button className="btn btn-ghost btn-sm" onClick={showSolution}>
            {completed ? 'Сравнить с эталонным решением' : 'Показать решение'}
          </button>
        ) : (
          <p className="muted small">
            Разбор решения откроется, когда вы решите задачу
            {lesson.hints.length > 0 ? ' — или после всех подсказок и нескольких попыток' : ''}.
          </p>
        )}
        {solutionError && <p className="error-text small">{solutionError}</p>}
      </div>
    </section>
  );
}

function BulbIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M9 18h6M10 21h4M12 3a6 6 0 0 0-3.6 10.8c.6.5 1 1.2 1.1 2h5c.1-.8.5-1.5 1.1-2A6 6 0 0 0 12 3z" />
    </svg>
  );
}

/** Открытый тест: ввод и вывод, а для заданий с файлами — файлы до и после запуска. */
function ExampleView({ example, index }: { example: Example; index: number }) {
  const before = Object.entries(example.files ?? {});
  const after = Object.entries(example.expectedFiles ?? {});
  return (
    <div className="example">
      <div className="example-title">
        Пример {index}
        {example.name && !/^Пример/.test(example.name) ? `: ${example.name}` : ''}
      </div>
      <div className="example-io">
        <div>
          <div className="example-label">Ввод</div>
          <pre>{example.stdin || <em className="muted">(нет)</em>}</pre>
          {example.files && (
            <>
              <div className="example-label">Файлы до запуска</div>
              {before.length === 0 ? (
                <pre>
                  <em className="muted">(папка пуста)</em>
                </pre>
              ) : (
                before.map(([name, content]) => <FileBlock key={name} name={name} content={content} />)
              )}
            </>
          )}
        </div>
        <div>
          {example.expectedOutput !== null && (
            <>
              <div className="example-label">Вывод</div>
              <pre>{example.expectedOutput}</pre>
            </>
          )}
          {after.length > 0 && (
            <>
              <div className="example-label">Файлы после запуска</div>
              {after.map(([name, content]) => (
                <FileBlock key={name} name={name} content={content} />
              ))}
            </>
          )}
        </div>
      </div>
    </div>
  );
}

function FileBlock({ name, content }: { name: string; content: string }) {
  return (
    <div className="example-file">
      <div className="example-file-name">{name}</div>
      <pre>{content || <em className="muted">(пустой файл)</em>}</pre>
    </div>
  );
}
