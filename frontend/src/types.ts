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
