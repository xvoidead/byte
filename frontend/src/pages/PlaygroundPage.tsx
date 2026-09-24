import { lazy, Suspense, useState } from 'react';
import { track } from '../analytics';
import { useTitle } from '../components/useTitle';
import { saveProject } from '../storage';
import type { ProjectFile } from '../types';

const Ide = lazy(() => import('../ide/Ide'));

const SAMPLE = `import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        System.out.println("Как тебя зовут?");
        String name = in.nextLine();
        System.out.println("Привет, " + name + "!");

        for (int i = 1; i <= 5; i++) {
            System.out.println(i + " в квадрате = " + i * i);
        }
    }
}
`;

const CONSOLE: ProjectFile[] = [{ name: 'Main.java', content: SAMPLE }];

interface Template {
  title: string;
  files: ProjectFile[];
}

const TEMPLATES: Template[] = [
  { title: 'Консольная программа', files: CONSOLE },
  {
    title: 'Конфиг на YAML',
    files: [
      {
        name: 'Main.java',
        content: `import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

public class Main {
    public static void main(String[] args) throws Exception {
        Map<String, Object> config = new Yaml().load(Files.readString(Path.of("config.yml")));

        String motd = (String) config.get("motd");
        int maxPlayers = (Integer) config.get("max-players");
        System.out.println(motd);
        System.out.println("Мест на сервере: " + maxPlayers);

        // Программа может записывать файлы в свою папку — они появятся во вкладках.
        Files.writeString(Path.of("last-start.txt"), "Сервер запускался, мест: " + maxPlayers + "\\n");
    }
}
`,
      },
      { name: 'config.yml', content: 'motd: Добро пожаловать на сервер!\nmax-players: 20\n' },
    ],
  },
  {
    title: 'JSON с Gson',
    files: [
      {
        name: 'Main.java',
        content: `import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class Main {
    public static void main(String[] args) throws Exception {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        List<Player> players = gson.fromJson(Files.readString(Path.of("players.json")),
                new TypeToken<List<Player>>() {}.getType());

        for (Player p : players) {
            System.out.println(p.name() + ": " + p.score());
        }

        players.add(new Player("Новичок", 0));
        Files.writeString(Path.of("players.json"), gson.toJson(players));
        System.out.println("Добавлен игрок — загляните во вкладку players.json");
    }
}
`,
      },
      { name: 'Player.java', content: 'public record Player(String name, int score) {\n}\n' },
      {
        name: 'players.json',
        content: '[\n  { "name": "Ада", "score": 120 },\n  { "name": "Алан", "score": 95 }\n]\n',
      },
    ],
  },
];

export function PlaygroundPage() {
  useTitle('Песочница — byte');
  const [version, setVersion] = useState(0);

  const openTemplate = (index: number) => {
    const template = TEMPLATES[index];
    if (!template || !window.confirm(`Открыть шаблон «${template.title}»? Текущий код в песочнице будет заменён.`)) {
      return;
    }
    saveProject('playground', { files: template.files, active: 'Main.java' });
    setVersion((v) => v + 1);
    track('page_view', undefined, `/playground#template-${index}`);
  };

  return (
    <main className="playground">
      <div className="playground-head">
        <div>
          <h1>Песочница</h1>
          <p className="muted">
            Пишите любой код на Java и запускайте его. Код сохраняется в этом браузере. Файлы добавляются кнопкой «+»
            над редактором; программа может читать и писать их, а ещё ей доступны Gson и SnakeYAML.
          </p>
        </div>
        <label className="template-picker">
          <span className="muted small">Шаблон</span>
          <select
            value=""
            onChange={(e) => {
              openTemplate(Number(e.target.value));
              e.target.value = '';
            }}
          >
            <option value="" disabled>
              Выбрать…
            </option>
            {TEMPLATES.map((t, i) => (
              <option key={t.title} value={i}>
                {t.title}
              </option>
            ))}
          </select>
        </label>
      </div>
      <div className="playground-ide">
        <Suspense fallback={<div className="ide-loading">Загружаем редактор…</div>}>
          <Ide key={version} storageKey="playground" initialFiles={CONSOLE} onRun={onPlaygroundRun} />
        </Suspense>
      </div>
    </main>
  );
}

function onPlaygroundRun(status: string, errorCode: string | null) {
  track('run', undefined, status);
  if (errorCode) track('compile_error', undefined, errorCode);
}
