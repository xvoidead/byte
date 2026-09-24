import { Markdown } from './Markdown';
import { highlightJava } from './highlightJava';
import type { Quiz } from '../types';
import type { QuizState } from '../storage';

const REVEAL_AFTER_WRONG = 2;

interface QuizCardProps {
  quiz: Quiz;
  state: QuizState | undefined;
  onAnswer: (choice: number) => void;
}

/**
 * Мини-вопрос. Правильный ответ подсвечивается и открывает объяснение;
 * после двух ошибок правильный вариант показывается сам, чтобы не застрять.
 */
export function QuizCard({ quiz, state, onAnswer }: QuizCardProps) {
  const solved = state?.correct ?? false;
  const attempts = state?.attempts ?? 0;
  const revealed = solved || attempts >= REVEAL_AFTER_WRONG;
  const lastWrong = state && !state.correct ? state.choice : null;

  return (
    <div className={`quiz ${solved ? 'is-solved' : revealed ? 'is-revealed' : ''}`}>
      <div className="quiz-label">
        <QuestionIcon /> Вопрос
      </div>
      <div className="quiz-question">
        <Markdown>{quiz.question}</Markdown>
      </div>
      {quiz.code && (
        <pre className="quiz-code">
          <code>
            {highlightJava(quiz.code).map((t, i) =>
              t.kind ? (
                <span key={i} className={`tok-${t.kind}`}>
                  {t.text}
                </span>
              ) : (
                t.text
              ),
            )}
          </code>
        </pre>
      )}
      <div className={`quiz-options ${quiz.code ? 'mono' : ''}`} role="group" aria-label="Варианты ответа">
        {quiz.options.map((option, i) => {
          const isAnswer = i === quiz.answer;
          const state =
            revealed && isAnswer ? 'correct' : !revealed && lastWrong === i ? 'wrong' : revealed && lastWrong === i ? 'wrong' : '';
          return (
            <button
              key={`${i}-${attempts}`}
              className={`quiz-option ${state}`}
              onClick={() => !revealed && onAnswer(i)}
              disabled={revealed}
              aria-pressed={state !== ''}
            >
              <span className="quiz-option-mark">{String.fromCharCode(65 + i)}</span>
              <span className="quiz-option-text">{option}</span>
            </button>
          );
        })}
      </div>
      {!revealed && lastWrong !== null && <p className="quiz-feedback wrong">Не совсем. Попробуйте ещё раз.</p>}
      {revealed && (
        <div className={`quiz-feedback ${solved ? 'correct' : 'revealed'}`}>
          <strong>{solved ? (attempts === 1 ? 'Верно с первой попытки!' : 'Верно!') : 'Правильный ответ отмечен.'}</strong>
          {quiz.explanation && <Markdown>{quiz.explanation}</Markdown>}
        </div>
      )}
    </div>
  );
}

function QuestionIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
      <circle cx="12" cy="12" r="9.5" />
      <path d="M9.5 9.2a2.6 2.6 0 0 1 5 .9c0 1.8-2.5 2.2-2.5 3.9" />
      <circle cx="12" cy="17.2" r="0.6" fill="currentColor" />
    </svg>
  );
}
