import { useState } from 'react';
import { highlightJava, type Token } from './highlightJava';

interface DemoFile {
  name: string;
  lines: Token[][];
  console: string[];
  files: string;
}

/** Строка YAML: ключ, значение и комментарий — хватает для витрины. */
function highlightYaml(line: string): Token[] {
  const comment = line.indexOf('#');
  if (comment === 0 || (comment > 0 && line.slice(0, comment).trim() === '')) {
    return [{ text: line, kind: 'comment' }];
  }
  const match = /^(\s*-?\s*)([\w-]+)(:)(.*)$/.exec(line);
  if (!match) return [{ text: line, kind: 'string' }];
  const [, indent, key, colon, rest] = match;
  const value = rest.trim();
  const kind = /^-?\d+(\.\d+)?$/.test(value) ? 'number' : /^(true|false)$/.test(value) ? 'keyword' : 'string';
  return [{ text: indent }, { text: key, kind: 'type' }, { text: colon }, { text: rest, kind }];
}

function highlightJson(line: string): Token[] {
  const tokens: Token[] = [];
  const re = /("[^"]*")(\s*:)?|(-?\d+)|([^"\d-]+|-)/g;
  let m: RegExpExecArray | null;
  while ((m = re.exec(line))) {
    if (m[1] && m[2]) tokens.push({ text: m[1], kind: 'type' }, { text: m[2] });
    else if (m[1]) tokens.push({ text: m[1], kind: 'string' });
    else if (m[3]) tokens.push({ text: m[3], kind: 'number' });
    else tokens.push({ text: m[0] });
  }
  return tokens;
}

const FILES: DemoFile[] = [
  {
    name: 'config.yml',
    lines: `# Настройки сервера
server-name: Byte Craft
max-players: 20
pvp: true
spawn:
  x: 100
  y: 64`
      .split('\n')
      .map(highlightYaml),
    console: ['Сервер: Byte Craft', 'Игроков: до 20', 'PvP: включён', 'Спавн: 100, 64'],
    files: 'config.yml прочитан',
  },
  {
    name: 'players.json',
    lines: `[
  {
    "name": "Ада",
    "score": 120
  },
  {
    "name": "Боб",
    "score": 110
  }
]`
      .split('\n')
      .map(highlightJson),
    console: ['> score Боб 70', 'Боб: 110', '> top', '1. Ада — 120', '2. Боб — 110'],
    files: 'изменён players.json',
  },
  {
    name: 'Main.java',
    lines: `Path file = Path.of("config.yml");
if (!Files.exists(file)) {
    Files.copy(defaults(), file);
    System.out.println("Новый конфиг");
}
String text = Files.readString(file);
Map<String, Object> config =
        new Yaml().load(text);
System.out.println("Сервер: "
        + config.get("server-name"));`
      .split('\n')
      .map(highlightJava),
    console: ['Новый конфиг', 'Сервер: Byte Craft'],
    files: 'создан config.yml',
  },
];

/** Витрина трека «Конфиги»: проект из нескольких файлов, вкладки переключаются. */
export function ConfigShowcase() {
  const [active, setActive] = useState(0);
  const file = FILES[active];
  return (
    <div className="window config-window">
      <div className="window-bar config-window-bar" role="tablist" aria-label="Файлы проекта">
        <span className="window-dots">
          <i />
          <i />
          <i />
        </span>
        {FILES.map((f, i) => (
          <button
            key={f.name}
            type="button"
            role="tab"
            aria-selected={i === active}
            className={`config-tab ${i === active ? 'active' : ''}`}
            onClick={() => setActive(i)}
          >
            {f.name}
          </button>
        ))}
      </div>
      <div className="window-body config-window-body" role="tabpanel" key={file.name}>
        <pre className="window-code config-code">
          {file.lines.map((line, i) => (
            <div key={i} className="code-line">
              <span className="code-ln">{i + 1}</span>
              <code>
                {line.map((t, j) =>
                  t.kind ? (
                    <span key={j} className={`tok-${t.kind}`}>
                      {t.text}
                    </span>
                  ) : (
                    t.text
                  ),
                )}
              </code>
            </div>
          ))}
        </pre>
        <div className="window-console">
          <div className="window-console-head">Консоль</div>
          {file.console.map((line, i) => (
            <div
              key={i}
              className={`window-console-line demo-line-in ${line.startsWith('>') ? 'muted' : ''}`}
              style={{ animationDelay: `${i * 70}ms` }}
            >
              {line}
            </div>
          ))}
          <div className="config-files-note demo-line-in" style={{ animationDelay: `${file.console.length * 70}ms` }}>
            Файлы: {file.files}
          </div>
        </div>
      </div>
    </div>
  );
}
