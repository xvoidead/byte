import type { Diagnostic, RunStatus } from '../types';

const STATUS_TEXT: Record<RunStatus, string> = {
  SUCCESS: 'Программа завершилась успешно',
  COMPILATION_ERROR: 'Ошибка компиляции',
  RUNTIME_ERROR: 'Ошибка во время выполнения',
  TIMEOUT: 'Превышено время выполнения',
  OUTPUT_LIMIT: 'Программа вывела слишком много текста',
  STOPPED: 'Программа остановлена',
};

export function statusText(status: RunStatus): string {
  return STATUS_TEXT[status];
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
  [/NoSuchElementException/, 'Программа пытается прочитать данные, которых нет: ввод закончился раньше, чем она ожидала.'],
  [/InputMismatchException/, 'Во вводе встретились данные не того типа — например, текст вместо числа.'],
  [/NumberFormatException/, 'Строку не удалось преобразовать в число.'],
  [/StackOverflowError/, 'Слишком глубокая рекурсия — вероятно, метод вызывает сам себя бесконечно.'],
  [/OutOfMemoryError/, 'Программе не хватило памяти.'],
  [/Слишком много потоков/, 'Программа создаёт слишком много потоков — в песочнице их число ограничено.'],
  [/AccessControlException|SecurityException/, 'Эта операция запрещена в песочнице: программы не могут работать с файлами, сетью, процессами и переменными окружения.'],
];

export function runtimeHint(stderr: string): string | undefined {
  return RUNTIME_HINTS.find(([pattern]) => pattern.test(stderr))?.[1];
}
