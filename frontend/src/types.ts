export type RunStatus = 'SUCCESS' | 'COMPILATION_ERROR' | 'RUNTIME_ERROR' | 'TIMEOUT' | 'OUTPUT_LIMIT' | 'STOPPED';

export interface Diagnostic {
  severity: 'ERROR' | 'WARNING';
  line: number;
  column: number;
  endColumn: number;
  message: string;
  hint: string | null;
  code: string | null;
}

export interface RunResult {
  status: RunStatus;
  stdout: string;
  stderr: string;
  exitCode: number | null;
  diagnostics: Diagnostic[];
  compileTimeMs: number;
  runTimeMs: number;
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
  expectedOutput: string;
}

export interface LessonDetails extends LessonSummary {
  theory: string;
  task: string;
  starterCode: string;
  examples: Example[];
  testCount: number;
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
}

export interface CheckResult {
  passed: boolean;
  compiled: boolean;
  diagnostics: Diagnostic[];
  tests: TestOutcome[];
}
