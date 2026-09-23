import type { Diagnostic, RunResult, RunStatus } from '../types';

const STATUS_TEXT: Record<RunStatus, string> = {
  SUCCESS: 'Программа завершилась успешно',
  COMPILATION_ERROR: 'Ошибка компиляции',
  RUNTIME_ERROR: 'Ошибка во время выполнения',
  TIMEOUT: 'Превышено время выполнения',
  OUTPUT_LIMIT: 'Программа вывела слишком много текста',
};

const STATUS_HINT: Partial<Record<RunStatus, string>> = {
  TIMEOUT:
    'Программа работала слишком долго и была остановлена. Проверьте, нет ли бесконечного цикла или ожидания ввода, которого нет во вкладке «Ввод».',
  OUTPUT_LIMIT: 'Вывод обрезан. Возможно, программа печатает в бесконечном цикле.',
};

export function statusText(status: RunStatus): string {
  return STATUS_TEXT[status];
}

export function RunOutput({
  result,
  onDiagnosticClick,
}: {
  result: RunResult;
  onDiagnosticClick: (line: number, column: number) => void;
}) {
  const ok = result.status === 'SUCCESS';
  const hint = STATUS_HINT[result.status] ?? runtimeHint(result.stderr);
  const errors = result.diagnostics.filter((d) => d.severity === 'ERROR');
  const warnings = result.diagnostics.filter((d) => d.severity === 'WARNING');

  return (
    <div className="run-output">
      <div className={`run-status ${ok ? 'ok' : 'fail'}`}>
        <span>{STATUS_TEXT[result.status]}</span>
        <span className="run-time">
          компиляция {result.compileTimeMs} мс
          {result.status !== 'COMPILATION_ERROR' && <> · выполнение {result.runTimeMs} мс</>}
          {result.exitCode !== null && result.exitCode !== 0 && <> · код выхода {result.exitCode}</>}
        </span>
      </div>

      {errors.length > 0 && <DiagnosticList diagnostics={errors} onClick={onDiagnosticClick} />}
      {result.status !== 'COMPILATION_ERROR' && warnings.length > 0 && (
        <DiagnosticList diagnostics={warnings} onClick={onDiagnosticClick} />
      )}

      {result.stdout && <pre className="stdout">{result.stdout}</pre>}
      {result.stderr && <pre className="stderr">{result.stderr}</pre>}
      {ok && !result.stdout && !result.stderr && <p className="console-placeholder">Программа ничего не вывела.</p>}
      {hint && <p className="run-hint">💡 {hint}</p>}
    </div>
  );
}

export function DiagnosticList({
  diagnostics,
  onClick,
}: {
  diagnostics: Diagnostic[];
  onClick: (line: number, column: number) => void;
}) {
  return (
    <ul className="diagnostics">
      {diagnostics.map((d, i) => (
        <li key={i} className={`diagnostic ${d.severity.toLowerCase()}`}>
          <button className="diagnostic-pos" onClick={() => onClick(d.line, d.column)} disabled={d.line <= 0}>
            {d.line > 0 ? `строка ${d.line}` : 'программа'}
          </button>
          <div>
            <code className="diagnostic-message">{d.message}</code>
            {d.hint && <p className="diagnostic-hint">{d.hint}</p>}
          </div>
        </li>
      ))}
    </ul>
  );
}

const RUNTIME_HINTS: [RegExp, string][] = [
  [/ArrayIndexOutOfBoundsException/, 'Обращение к элементу массива за его границами. Индексы начинаются с 0 и заканчиваются на length - 1.'],
  [/StringIndexOutOfBoundsException/, 'Обращение к символу строки за её границами. Проверьте индексы в charAt и substring.'],
  [/NullPointerException/, 'Обращение к объекту, которого нет (null). Проверьте, что переменной присвоено значение.'],
  [/ArithmeticException: \/ by zero/, 'Деление целого числа на ноль.'],
  [/NoSuchElementException/, 'Программа пытается прочитать данные, которых нет. Заполните вкладку «Ввод».'],
  [/InputMismatchException/, 'Во вводе встретились данные не того типа — например, текст вместо числа.'],
  [/NumberFormatException/, 'Строку не удалось преобразовать в число.'],
  [/StackOverflowError/, 'Слишком глубокая рекурсия — вероятно, метод вызывает сам себя бесконечно.'],
  [/OutOfMemoryError/, 'Программе не хватило памяти.'],
];

function runtimeHint(stderr: string): string | undefined {
  return RUNTIME_HINTS.find(([pattern]) => pattern.test(stderr))?.[1];
}
