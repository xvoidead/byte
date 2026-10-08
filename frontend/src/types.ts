export type RunStatus =
  | 'SUCCESS'
  | 'COMPILATION_ERROR'
  | 'RUNTIME_ERROR'
  | 'TIMEOUT'
  | 'OUTPUT_LIMIT'
  | 'STOPPED'
  | 'FILES_LIMIT';

/** Файл проекта: исходник .java, ресурс resources/… или файл рабочей папки (config.yml и т.п.). */
export interface ProjectFile {
  name: string;
  content: string;
}

/** Файл рабочей папки после запуска; content = null — двоичный или слишком большой. */
export interface OutputFile {
  name: string;
  content: string | null;
  size: number;
}

export interface Diagnostic {
  severity: 'ERROR' | 'WARNING';
  line: number;
  column: number;
  endColumn: number;
  message: string;
  hint: string | null;
  code: string | null;
  /** Файл проекта, к которому относится сообщение; null — к проекту целиком. */
  file: string | null;
}

export interface RunResult {
  status: RunStatus;
  stdout: string;
  stderr: string;
  exitCode: number | null;
  diagnostics: Diagnostic[];
  compileTimeMs: number;
  runTimeMs: number;
  files: OutputFile[];
}

export interface LessonSummary {
  slug: string;
  order: number;
  module: string;
  title: string;
  summary: string;
}

export interface Example {
  name: string;
  stdin: string;
  /** null — вывод не проверяется, только файлы. */
  expectedOutput: string | null;
  files: Record<string, string> | null;
  expectedFiles: Record<string, string> | null;
}

export interface Quiz {
  question: string;
  code: string | null;
  options: string[];
  answer: number;
  explanation: string;
}

export type StepBlock = { type: 'text'; markdown: string } | { type: 'quiz'; quiz: Quiz };

export interface Step {
  title: string;
  blocks: StepBlock[];
}

export interface LessonDetails extends LessonSummary {
  steps: Step[];
  task: string;
  starterCode: string;
  starterFiles: ProjectFile[];
  /** Файл, который редактор выбирает при первом открытии урока. */
  activeFile: string | null;
  examples: Example[];
  testCount: number;
  hints: string[];
  requirements: string[];
  prev: string | null;
  next: string | null;
}

export interface TestOutcome {
  name: string;
  passed: boolean;
  hidden: boolean;
  status: RunStatus;
  stdin: string | null;
  expectedOutput: string | null;
  actualOutput: string | null;
  stderr: string | null;
  files: FileOutcome[] | null;
}

export interface FileOutcome {
  name: string;
  passed: boolean;
  expected: string;
  actual: string | null;
}

export interface RequirementOutcome {
  message: string;
  passed: boolean;
}

export interface CheckResult {
  passed: boolean;
  compiled: boolean;
  diagnostics: Diagnostic[];
  tests: TestOutcome[];
  requirements: RequirementOutcome[];
}
