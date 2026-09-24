interface StepperProps {
  titles: string[];
  current: number;
  visited: number[];
  onSelect: (index: number) => void;
}

/** Полоса шагов урока: пройденные заполнены, текущий подсвечен, по любому можно перейти. */
export function Stepper({ titles, current, visited, onSelect }: StepperProps) {
  return (
    <nav className="stepper" aria-label="Шаги урока">
      <ol className="stepper-bars">
        {titles.map((title, i) => (
          <li key={i}>
            <button
              className={`stepper-bar ${i === current ? 'current' : ''} ${visited.includes(i) ? 'visited' : ''} ${
                i === titles.length - 1 ? 'task' : ''
              }`}
              onClick={() => onSelect(i)}
              aria-current={i === current ? 'step' : undefined}
              aria-label={`Шаг ${i + 1}: ${title}`}
              title={title}
            />
          </li>
        ))}
      </ol>
      <div className="stepper-caption">
        <span>
          {current === titles.length - 1 ? 'Последний шаг' : `Шаг ${current + 1} из ${titles.length - 1}`}
        </span>
        <span className="stepper-title">{titles[current]}</span>
      </div>
    </nav>
  );
}
