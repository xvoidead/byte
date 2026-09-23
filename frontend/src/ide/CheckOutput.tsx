import type { CheckResult, TestOutcome } from '../types';
import { DiagnosticList, statusText } from './RunOutput';

export function CheckOutput({
  result,
  onDiagnosticClick,
}: {
  result: CheckResult;
  onDiagnosticClick: (line: number, column: number) => void;
}) {
  if (!result.compiled) {
    return (
      <div className="run-output">
        <div className="run-status fail">Ошибка компиляции — тесты не запускались</div>
        <DiagnosticList
          diagnostics={result.diagnostics.filter((d) => d.severity === 'ERROR')}
          onClick={onDiagnosticClick}
        />
      </div>
    );
  }

  const passed = result.tests.filter((t) => t.passed).length;
  return (
    <div className="run-output">
      <div className={`run-status ${result.passed ? 'ok' : 'fail'}`}>
        {result.passed ? 'Все тесты пройдены — задание выполнено' : `Пройдено тестов: ${passed} из ${result.tests.length}`}
      </div>
      <ul className="tests">
        {result.tests.map((test, i) => (
          <TestItem key={i} test={test} index={i + 1} />
        ))}
      </ul>
    </div>
  );
}

function TestItem({ test, index }: { test: TestOutcome; index: number }) {
  return (
    <li className={`test ${test.passed ? 'ok' : 'fail'}`}>
      <div className="test-head">
        <span className="test-icon">{test.passed ? '✓' : '✗'}</span>
        <span>
          Тест {index}: {test.name}
          {test.hidden && <span className="test-hidden"> · скрытый</span>}
        </span>
        {!test.passed && test.status !== 'SUCCESS' && <span className="test-status">{statusText(test.status)}</span>}
      </div>
      {!test.passed && !test.hidden && (
        <div className="test-details">
          {test.stdin ? (
            <Block title="Ввод" text={test.stdin} />
          ) : null}
          <Block title="Ожидалось" text={test.expectedOutput ?? ''} />
          <Block title="Получено" text={test.actualOutput ?? ''} />
          {test.stderr && <Block title="Ошибки" text={test.stderr} error />}
        </div>
      )}
      {!test.passed && test.hidden && (
        <p className="test-hidden-note">
          Данные скрытого теста не показываются. Подумайте о граничных случаях: минимальные и максимальные значения,
          отрицательные числа, пустой ввод.
        </p>
      )}
    </li>
  );
}

function Block({ title, text, error }: { title: string; text: string; error?: boolean }) {
  return (
    <div className="test-block">
      <div className="test-block-title">{title}</div>
      <pre className={error ? 'stderr' : undefined}>{text === '' ? <em className="muted">(пусто)</em> : text}</pre>
    </div>
  );
}
