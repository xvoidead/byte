export type MascotMood = 'idle' | 'happy' | 'sad';

interface MascotProps {
  mood?: MascotMood;
  className?: string;
  /** Смена значения перезапускает анимацию реакции (прыжок или вздох). */
  reactKey?: number | string;
}

/**
 * Персонаж byte. Моргает в покое, подпрыгивает от радости и огорчается при ошибке.
 * Рисунок совпадает с public/mascot.svg, но собран в React, чтобы анимировать части.
 */
export function Mascot({ mood = 'idle', className = '', reactKey }: MascotProps) {
  return (
    <span key={reactKey} className={`mascot mascot--${mood} ${className}`} aria-hidden="true">
      <svg viewBox="0 0 64 64" fill="none">
        <g className="mascot-body">
          <path
            d="M29 4.5c-8 .6-17.5 15.5-20.5 27C5.5 43 10 51 32 51s27.5-7 24-18.5C53.5 24 44 15 38 9c-3-3-6-4.7-9-4.5z"
            fill="#f4f5ff"
            stroke="#0b0c14"
            strokeWidth="2.4"
            strokeLinejoin="round"
          />
          <path d="M11 38c3 7 10 10 21 10s18-3 21-9" stroke="#dfe3ff" strokeWidth="4" strokeLinecap="round" />
          <path
            d="M29.5 14C23 14.5 16 27 15.5 35.5 15 42 21 46 31.5 46S48.5 42.5 48 36.5C47.5 29 38 13.5 29.5 14z"
            fill="#0b0c14"
          />
          <g className="mascot-eyes" stroke="#a8b5ff" strokeWidth="3" strokeLinecap="round">
            {mood === 'sad' ? (
              <>
                <path d="M22 37.5l8-1.5" />
                <path d="M34 36l8 1.5" />
              </>
            ) : (
              <>
                <path d="M21.5 37.5q4-5.5 8.5-.5" />
                <path d="M34 35q4.5-5.5 8.5-.5" />
              </>
            )}
          </g>
          <g transform="rotate(-18 50 22)" className="mascot-tag">
            <rect x="45" y="17" width="10" height="10" rx="2.4" fill="#0b0c14" />
            <path
              d="M48.6 20.2l-1.6 1.8 1.6 1.8M51.4 20.2l1.6 1.8-1.6 1.8"
              stroke="#a8b5ff"
              strokeWidth="1.1"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </g>
        </g>
        <ellipse cx="20" cy="51.5" rx="10.5" ry="6.5" fill="#f4f5ff" stroke="#0b0c14" strokeWidth="2.4" />
        <ellipse cx="44" cy="51.5" rx="10.5" ry="6.5" fill="#f4f5ff" stroke="#0b0c14" strokeWidth="2.4" />
        <ellipse cx="26.5" cy="52" rx="5.2" ry="4.4" fill="#0b0c14" />
        <ellipse cx="37.5" cy="52" rx="5.2" ry="4.4" fill="#0b0c14" />
      </svg>
      {mood === 'happy' && (
        <span className="mascot-sparkles">
          <i />
          <i />
          <i />
        </span>
      )}
    </span>
  );
}
