import {
  useCallback,
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
  type FormEvent,
  type ReactNode,
  type RefObject,
} from 'react';
import { Link } from 'react-router-dom';
import { useTitle } from '../components/useTitle';

// Страница владельца сайта: сводка анонимной статистики. Доступ — по токену BYTE_ADMIN_TOKEN.

interface LessonStats {
  slug: string;
  order: number;
  title: string;
  opened: number;
  started: number;
  completed: number;
  checksPerCompletion: number | null;
  hints: number;
  solutions: number;
}

interface TopItem {
  lesson: string | null;
  item: string;
  count: number;
}

interface QuizStats {
  lesson: string;
  item: string;
  question: string;
  step: string | null;
  visitors: number;
  firstTryCorrect: number;
}

interface Stats {
  totals: { visitors30d: number; visitorsToday: number; completions: number; runs: number; checks: number };
  daily: { day: string; visitors: number }[];
  lessons: LessonStats[];
  failingTests: TopItem[];
  failingRequirements: TopItem[];
  compileErrors: TopItem[];
  quizzes: QuizStats[];
}

type LoadState =
  | { status: 'need-token'; error?: string }
  | { status: 'loading'; previous?: Stats }
  | { status: 'ready'; data: Stats }
  | { status: 'error'; error: string; previous?: Stats };

const TOKEN_KEY = 'byte:stats-token';

function storedToken(): string | null {
  try {
    return sessionStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

function storeToken(token: string | null) {
  try {
    if (token) sessionStorage.setItem(TOKEN_KEY, token);
    else sessionStorage.removeItem(TOKEN_KEY);
  } catch {
    // без sessionStorage токен придётся вводить при каждом открытии
  }
}

const number = new Intl.NumberFormat('ru-RU');
const fmt = (n: number) => number.format(n);

export default function StatsPage() {
  useTitle('Статистика — byte');
  const [token, setToken] = useState<string | null>(storedToken);
  const [state, setState] = useState<LoadState>(() => (token ? { status: 'loading' } : { status: 'need-token' }));

  const load = useCallback(async (current: string) => {
    setState((s) => ({ status: 'loading', previous: s.status === 'ready' ? s.data : undefined }));
    try {
      const response = await fetch('/api/admin/stats', { headers: { Authorization: `Bearer ${current}` } });
      if (response.status === 401 || response.status === 404) {
        storeToken(null);
        setToken(null);
        setState({
          status: 'need-token',
          error: response.status === 401 ? 'Неверный токен.' : 'Статистика не настроена: задайте BYTE_ADMIN_TOKEN на сервере.',
        });
        return;
      }
      if (!response.ok) throw new Error(`Ошибка сервера (${response.status})`);
      setState({ status: 'ready', data: (await response.json()) as Stats });
    } catch (e) {
      setState((s) => ({
        status: 'error',
        error: e instanceof Error && e.message.startsWith('Ошибка') ? e.message : 'Нет связи с сервером.',
        previous: s.status === 'loading' ? s.previous : undefined,
      }));
    }
  }, []);

  useEffect(() => {
    if (token) void load(token);
  }, [token, load]);

  if (state.status === 'need-token') {
    return (
      <TokenForm
        error={state.error}
        onSubmit={(value) => {
          storeToken(value);
          setToken(value);
        }}
      />
    );
  }

  const data = state.status === 'ready' ? state.data : state.previous;
  return (
    <main className="stats">
      <header className="stats-head">
        <div>
          <h1>Статистика</h1>
          <p className="muted small">Анонимные события за всё время хранения (до 180 дней). Посетители — уникальные браузеры.</p>
        </div>
        <div className="stats-actions">
          <button className="btn btn-secondary btn-sm" disabled={state.status === 'loading'} onClick={() => token && load(token)}>
            {state.status === 'loading' ? 'Обновляем…' : 'Обновить'}
          </button>
          <button
            className="btn btn-ghost btn-sm"
            onClick={() => {
              storeToken(null);
              setToken(null);
              setState({ status: 'need-token' });
            }}
          >
            Выйти
          </button>
        </div>
      </header>

      {state.status === 'error' && <p className="error-text">{state.error}</p>}
      {!data ? (
        state.status === 'loading' && <div className="page-state">Загружаем статистику…</div>
      ) : (
        <div className={state.status === 'loading' ? 'stats-body is-refreshing' : 'stats-body'}>
          <Dashboard data={data} />
        </div>
      )}
    </main>
  );
}

function TokenForm({ error, onSubmit }: { error?: string; onSubmit: (token: string) => void }) {
  const [value, setValue] = useState('');
  const submit = (e: FormEvent) => {
    e.preventDefault();
    if (value.trim()) onSubmit(value.trim());
  };
  return (
    <main className="page-state">
      <h1>Статистика</h1>
      <p className="muted">Страница для владельца сайта. Введите токен из переменной BYTE_ADMIN_TOKEN.</p>
      <form className="stats-login" onSubmit={submit}>
        <input
          type="password"
          autoComplete="current-password"
          aria-label="Токен"
          placeholder="Токен"
          value={value}
          onChange={(e) => setValue(e.target.value)}
        />
        <button className="btn btn-primary btn-sm" type="submit">
          Открыть
        </button>
      </form>
      {error && <p className="error-text small">{error}</p>}
      <Link to="/">← На главную</Link>
    </main>
  );
}

function Dashboard({ data }: { data: Stats }) {
  const titles = new Map(data.lessons.map((l) => [l.slug, `${l.order}. ${l.title}`]));
  const lessonName = (slug: string | null) => (slug ? (titles.get(slug) ?? slug) : 'Песочница');
  const empty = data.totals.visitors30d === 0 && data.lessons.every((l) => l.opened === 0);

  return (
    <>
      <section className="stats-kpis" aria-label="Главные показатели">
        <Tile label="Посетители за 30 дней" value={data.totals.visitors30d} />
        <Tile label="Сегодня" value={data.totals.visitorsToday} />
        <Tile label="Пройдено уроков" value={data.totals.completions} />
        <Tile label="Запусков кода" value={data.totals.runs} />
        <Tile label="Проверок решений" value={data.totals.checks} />
      </section>

      {empty && (
        <p className="stats-empty">
          Пока событий нет. Они появятся, когда ученики начнут открывать уроки; при включённом «Не отслеживать» браузер
          ничего не отправляет.
        </p>
      )}

      <Card title="Посетители по дням" subtitle="Уникальные браузеры за последние 30 дней">
        <DailyChart days={data.daily} />
        <details className="stats-table-toggle">
          <summary>Таблица</summary>
          <table className="stats-table">
            <thead>
              <tr>
                <th>День</th>
                <th className="num">Посетители</th>
              </tr>
            </thead>
            <tbody>
              {data.daily.map((d) => (
                <tr key={d.day}>
                  <td>{formatDay(d.day, true)}</td>
                  <td className="num">{fmt(d.visitors)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </details>
      </Card>

      <Card title="Воронка по урокам" subtitle="Сколько учеников открыли урок и сколько его прошли">
        <LessonChart lessons={data.lessons} />
        <div className="stats-scroll">
          <table className="stats-table">
            <thead>
              <tr>
                <th>Урок</th>
                <th className="num">Открыли</th>
                <th className="num">Начали</th>
                <th className="num">Прошли</th>
                <th>Доходимость</th>
                <th className="num" title="Сколько проверок в среднем нужно до прохождения">
                  Проверок
                </th>
                <th className="num">Подсказки</th>
                <th className="num">Решения</th>
              </tr>
            </thead>
            <tbody>
              {data.lessons.map((l) => (
                <tr key={l.slug}>
                  <td>
                    <Link to={`/lessons/${l.slug}`}>
                      {l.order}. {l.title}
                    </Link>
                  </td>
                  <td className="num">{fmt(l.opened)}</td>
                  <td className="num">{fmt(l.started)}</td>
                  <td className="num">{fmt(l.completed)}</td>
                  <td>{l.opened > 0 ? <Meter percent={Math.round((l.completed * 100) / l.opened)} /> : <span className="muted">—</span>}</td>
                  <td className="num">{l.checksPerCompletion === null ? '—' : l.checksPerCompletion.toLocaleString('ru-RU')}</td>
                  <td className="num">{fmt(l.hints)}</td>
                  <td className="num">{fmt(l.solutions)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>

      <Card title="Трудные вопросы" subtitle="Доля правильных ответов с первой попытки — самые трудные сверху">
        {data.quizzes.length === 0 ? (
          <p className="muted small">Ответов пока нет.</p>
        ) : (
          <div className="stats-scroll">
            <table className="stats-table">
              <thead>
                <tr>
                  <th>Вопрос</th>
                  <th>Урок</th>
                  <th className="num">Ответили</th>
                  <th>С первой попытки</th>
                </tr>
              </thead>
              <tbody>
                {data.quizzes.map((q) => (
                  <tr key={`${q.lesson}-${q.item}`}>
                    <td className="stats-question">
                      {q.question}
                      {q.step && <div className="muted small">{q.step}</div>}
                    </td>
                    <td className="muted">{lessonName(q.lesson)}</td>
                    <td className="num">{fmt(q.visitors)}</td>
                    <td>
                      <Meter percent={q.firstTryCorrect} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      <div className="stats-grid">
        <TopCard
          title="Непройденные тесты"
          subtitle="Число учеников, у которых тест не прошёл"
          items={data.failingTests}
          lessonName={lessonName}
        />
        <TopCard
          title="Невыполненные требования"
          subtitle="Число учеников, чьё решение не выполнило требование"
          items={data.failingRequirements}
          lessonName={lessonName}
        />
      </div>
      <TopCard
        title="Частые ошибки компиляции"
        subtitle="Коды ошибок javac: сколько раз встречались"
        items={data.compileErrors}
        lessonName={lessonName}
        mono
      />
    </>
  );
}

function Tile({ label, value }: { label: string; value: number }) {
  return (
    <div className="stats-tile">
      <div className="stats-tile-label">{label}</div>
      <div className="stats-tile-value">{fmt(value)}</div>
    </div>
  );
}

function Card({ title, subtitle, children }: { title: string; subtitle: string; children: ReactNode }) {
  return (
    <section className="stats-card">
      <h2>{title}</h2>
      <p className="muted small">{subtitle}</p>
      {children}
    </section>
  );
}

function Meter({ percent }: { percent: number }) {
  return (
    <span className="stats-meter" role="img" aria-label={`${percent} %`}>
      <span className="stats-meter-track">
        <span className="stats-meter-fill" style={{ width: `${Math.max(0, Math.min(100, percent))}%` }} />
      </span>
      <span className="stats-meter-value">{percent} %</span>
    </span>
  );
}

function TopCard({
  title,
  subtitle,
  items,
  lessonName,
  mono,
}: {
  title: string;
  subtitle: string;
  items: TopItem[];
  lessonName: (slug: string | null) => string;
  mono?: boolean;
}) {
  return (
    <Card title={title} subtitle={subtitle}>
      {items.length === 0 ? (
        <p className="muted small">Пока ничего.</p>
      ) : (
        <div className="stats-scroll">
          <table className="stats-table">
            <thead>
              <tr>
                <th>{mono ? 'Код' : 'Что'}</th>
                <th>Урок</th>
                <th className="num">Раз</th>
              </tr>
            </thead>
            <tbody>
              {items.map((item, i) => (
                <tr key={i}>
                  <td className={mono ? 'stats-code' : 'stats-item'}>{item.item}</td>
                  <td className="muted">{lessonName(item.lesson)}</td>
                  <td className="num">{fmt(item.count)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Card>
  );
}

// ——— Графики ———

const PLOT_H = 180;
const AXIS_H = 26;
const LEFT = 36;
const TOP = 12;

function useWidth<T extends HTMLElement>(): [RefObject<T | null>, number] {
  const ref = useRef<T>(null);
  const [width, setWidth] = useState(0);
  useLayoutEffect(() => {
    const element = ref.current;
    if (!element) return;
    setWidth(element.clientWidth);
    const observer = new ResizeObserver(() => setWidth(element.clientWidth));
    observer.observe(element);
    return () => observer.disconnect();
  }, []);
  return [ref, width];
}

/** Круглые отметки оси: шаг 1, 2 или 5 × 10ⁿ, 3–5 делений. */
function ticks(max: number): number[] {
  if (max <= 0) return [0, 1];
  const rough = max / 4;
  const power = 10 ** Math.floor(Math.log10(rough));
  const step = [1, 2, 5, 10].map((m) => m * power).find((s) => s >= rough) ?? 10 * power;
  const niceStep = Math.max(1, step);
  const result: number[] = [];
  for (let v = 0; v <= max + niceStep - 1e-9 && result.length < 7; v += niceStep) result.push(v);
  if (result[result.length - 1] < max) result.push(result[result.length - 1] + niceStep);
  return result;
}

/** Подпись под каждым every-м столбиком и под последним — но не вплотную к нему. */
function labelled(i: number, count: number, every: number): boolean {
  const last = count - 1;
  return i === last || (i % every === 0 && last - i >= every / 2);
}

/** Столбик: скруглённый конец 4px, квадратное основание. */
function barPath(x: number, y: number, w: number, h: number): string {
  if (h <= 0) return '';
  const r = Math.min(4, h, w / 2);
  const base = y + h;
  return `M${x},${base}V${y + r}Q${x},${y} ${x + r},${y}H${x + w - r}Q${x + w},${y} ${x + w},${y + r}V${base}Z`;
}

function clamp(value: number, min: number, max: number): number {
  return Math.max(min, Math.min(max, value));
}

function formatDay(day: string, long = false): string {
  const date = new Date(`${day}T00:00:00Z`);
  return date.toLocaleDateString('ru-RU', long ? { day: 'numeric', month: 'long', timeZone: 'UTC' } : { day: 'numeric', month: 'short', timeZone: 'UTC' });
}

interface TooltipState {
  x: number;
  y: number;
  title: string;
  rows: { label: string; value: string; className?: string }[];
}

function Tooltip({ tip }: { tip: TooltipState | null }) {
  if (!tip) return null;
  return (
    <div className="stats-tooltip" style={{ left: tip.x, top: tip.y }} role="status">
      <div className="stats-tooltip-title">{tip.title}</div>
      {tip.rows.map((row) => (
        <div key={row.label} className="stats-tooltip-row">
          <span className={`stats-key ${row.className ?? 'stats-key-blank'}`} />
          <strong>{row.value}</strong>
          <span>{row.label}</span>
        </div>
      ))}
    </div>
  );
}

function Axis({ width, scale, values }: { width: number; scale: (v: number) => number; values: number[] }) {
  return (
    <g className="stats-axis">
      {values.map((v) => (
        <g key={v}>
          <line x1={LEFT} x2={width} y1={scale(v)} y2={scale(v)} />
          <text x={LEFT - 8} y={scale(v)} dy="0.32em" textAnchor="end">
            {fmt(v)}
          </text>
        </g>
      ))}
    </g>
  );
}

function DailyChart({ days }: { days: { day: string; visitors: number }[] }) {
  const [ref, width] = useWidth<HTMLDivElement>();
  const [active, setActive] = useState<number | null>(null);
  const axis = ticks(Math.max(...days.map((d) => d.visitors), 1));
  const max = axis[axis.length - 1];
  const plotW = Math.max(0, width - LEFT);
  const band = plotW / Math.max(1, days.length);
  const barW = Math.max(2, Math.min(24, band - 2, band * 0.7));
  const scale = (v: number) => TOP + PLOT_H - (v / max) * PLOT_H;
  const labelEvery = width < 520 ? 10 : 5;

  const tip: TooltipState | null =
    active === null
      ? null
      : {
          x: clamp(LEFT + band * active + band / 2, 90, width - 90),
          y: scale(days[active].visitors),
          title: formatDay(days[active].day, true),
          rows: [{ label: 'посетителей', value: fmt(days[active].visitors) }],
        };

  return (
    <div className="stats-chart" ref={ref} onPointerLeave={() => setActive(null)}>
      {width > 0 && (
        <svg width={width} height={TOP + PLOT_H + AXIS_H} role="img" aria-label="Посетители по дням за 30 дней">
          <Axis width={width} scale={scale} values={axis} />
          {days.map((d, i) => {
            const x = LEFT + band * i + (band - barW) / 2;
            const y = scale(d.visitors);
            return (
              <g
                key={d.day}
                className={active === i ? 'stats-bar is-active' : 'stats-bar'}
                tabIndex={0}
                aria-label={`${formatDay(d.day, true)}: ${fmt(d.visitors)}`}
                onPointerEnter={() => setActive(i)}
                onFocus={() => setActive(i)}
                onBlur={() => setActive(null)}
              >
                <rect className="stats-hit" x={LEFT + band * i} y={TOP} width={band} height={PLOT_H} />
                <path className="fill-accent" d={barPath(x, y, barW, TOP + PLOT_H - y)} />
                {labelled(i, days.length, labelEvery) && (
                  <text className="stats-xlabel" x={LEFT + band * i + band / 2} y={TOP + PLOT_H + 18} textAnchor="middle">
                    {formatDay(d.day)}
                  </text>
                )}
              </g>
            );
          })}
        </svg>
      )}
      <Tooltip tip={tip} />
    </div>
  );
}

function LessonChart({ lessons }: { lessons: LessonStats[] }) {
  const [ref, width] = useWidth<HTMLDivElement>();
  const [active, setActive] = useState<number | null>(null);
  const [scrollLeft, setScrollLeft] = useState(0);
  const chartW = Math.max(width, 560);
  const axis = ticks(Math.max(...lessons.map((l) => l.opened), 1));
  const max = axis[axis.length - 1];
  const plotW = chartW - LEFT;
  const band = plotW / Math.max(1, lessons.length);
  const barW = Math.max(3, Math.min(12, (band - 6) / 2));
  const groupW = barW * 2 + 2;
  const scale = (v: number) => TOP + PLOT_H - (v / max) * PLOT_H;

  const lesson = active === null ? null : lessons[active];
  const tip: TooltipState | null =
    lesson === null || active === null
      ? null
      : {
          x: clamp(LEFT + band * active + band / 2 - scrollLeft, 90, width - 90),
          y: scale(lesson.opened),
          title: `${lesson.order}. ${lesson.title}`,
          rows: [
            { label: 'открыли', value: fmt(lesson.opened), className: 'fill-muted' },
            { label: 'начали решать', value: fmt(lesson.started) },
            { label: 'прошли', value: fmt(lesson.completed), className: 'fill-accent' },
          ],
        };

  return (
    <>
      <div className="stats-legend" aria-hidden="true">
        <span>
          <span className="stats-swatch fill-muted" /> Открыли
        </span>
        <span>
          <span className="stats-swatch fill-accent" /> Прошли
        </span>
      </div>
      <div className="stats-chart" ref={ref} onPointerLeave={() => setActive(null)}>
        <div className="stats-chart-wide" onScroll={(e) => setScrollLeft(e.currentTarget.scrollLeft)}>
          {width > 0 && (
            <svg width={chartW} height={TOP + PLOT_H + AXIS_H} role="img" aria-label="Открыли и прошли каждый урок">
              <Axis width={chartW} scale={scale} values={axis} />
              {lessons.map((l, i) => {
                const x = LEFT + band * i + (band - groupW) / 2;
                const yOpened = scale(l.opened);
                const yCompleted = scale(l.completed);
                return (
                  <g
                    key={l.slug}
                    className={active === i ? 'stats-bar is-active' : 'stats-bar'}
                    tabIndex={0}
                    aria-label={`Урок ${l.order}: открыли ${l.opened}, прошли ${l.completed}`}
                    onPointerEnter={() => setActive(i)}
                    onFocus={() => setActive(i)}
                    onBlur={() => setActive(null)}
                  >
                    <rect className="stats-hit" x={LEFT + band * i} y={TOP} width={band} height={PLOT_H} />
                    <path className="fill-muted" d={barPath(x, yOpened, barW, TOP + PLOT_H - yOpened)} />
                    <path className="fill-accent" d={barPath(x + barW + 2, yCompleted, barW, TOP + PLOT_H - yCompleted)} />
                    {labelled(i, lessons.length, 5) && (
                      <text className="stats-xlabel" x={LEFT + band * i + band / 2} y={TOP + PLOT_H + 18} textAnchor="middle">
                        {l.order}
                      </text>
                    )}
                  </g>
                );
              })}
            </svg>
          )}
        </div>
        <Tooltip tip={tip} />
      </div>
    </>
  );
}
