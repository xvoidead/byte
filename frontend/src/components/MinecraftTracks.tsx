import { Link } from 'react-router-dom';
import { Reveal } from './motion';
import type { LessonSummary } from '../types';

interface MinecraftTracksProps {
  lessons: LessonSummary[];
  completed: Set<string>;
}

const TRACKS = [
  {
    module: 'Плагины Minecraft — Bukkit',
    name: 'Bukkit / Paper',
    badge: 'Сервер',
    description: 'События игроков и блоков, команды, права доступа, YAML-конфиг, планировщик и экономика.',
    note: 'MockBukkit запускает плагины и сценарии прямо в песочнице.',
  },
  {
    module: 'Моды Minecraft — Fabric',
    name: 'Fabric',
    badge: 'Сервер + клиент',
    description: 'Entry point мода, подключения, server ticks, блок-события, JSON-конфиг и клиентская клавиша.',
    note: 'Для проверок используется учебная симуляция API — без запуска самой игры.',
  },
];

export function MinecraftTracks({ lessons, completed }: MinecraftTracksProps) {
  return (
    <section className="minecraft-tracks" aria-labelledby="minecraft-tracks-title">
      <Reveal className="section-head">
        <div>
          <span className="pill">
            <span className="pill-dot" />
            Практика на Java
          </span>
          <h2 className="section-title" id="minecraft-tracks-title">
            Разработка для Minecraft
          </h2>
          <p className="section-lead">
            Два направления после основ Java: серверные плагины Bukkit/Paper и моды на Fabric для сервера и клиента.
          </p>
        </div>
      </Reveal>

      <div className="minecraft-track-grid">
        {TRACKS.map((track) => {
          const items = lessons.filter((lesson) => lesson.module === track.module);
          const next = items.find((lesson) => !completed.has(lesson.slug)) ?? items[0];
          if (!items.length) return null;
          return (
            <Reveal className="minecraft-track-card" key={track.module}>
              <div className="minecraft-track-title-row">
                <span className="minecraft-track-badge">{track.badge}</span>
                <h3>{track.name}</h3>
              </div>
              <p className="minecraft-track-description">{track.description}</p>
              <p className="minecraft-track-note">{track.note}</p>
              <ol className="minecraft-track-lessons">
                {items.map((lesson) => (
                  <li key={lesson.slug}>
                    <Link to={`/lessons/${lesson.slug}`} className={completed.has(lesson.slug) ? 'done' : ''}>
                      <span className="track-lesson-num">{String(lesson.order).padStart(2, '0')}</span>
                      {lesson.title}
                      {completed.has(lesson.slug) && <CheckIcon />}
                    </Link>
                  </li>
                ))}
              </ol>
              <Link className="btn btn-secondary" to={`/lessons/${next?.slug ?? items[0].slug}`}>
                {items.some((lesson) => completed.has(lesson.slug)) ? 'Продолжить трек' : 'Начать трек'}
                <ArrowIcon />
              </Link>
            </Reveal>
          );
        })}
      </div>
    </section>
  );
}

const iconProps = {
  width: 16,
  height: 16,
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.7,
  strokeLinecap: 'round' as const,
  strokeLinejoin: 'round' as const,
  'aria-hidden': true,
};

function ArrowIcon() {
  return (
    <svg {...iconProps}>
      <path d="M5 12h14M13 6l6 6-6 6" />
    </svg>
  );
}

function CheckIcon() {
  return (
    <svg {...iconProps}>
      <path d="M5 12.5l4.5 4.5L19 7.5" />
    </svg>
  );
}
