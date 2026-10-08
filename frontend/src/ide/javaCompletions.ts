import type * as Monaco from 'monaco-editor';
import { receiverBefore, resolveReceiver, signature, type Member } from './javaMembers';

interface Snippet {
  label: string;
  insert: string;
  detail: string;
}

// Сокращения в духе IntelliJ IDEA: набрали «sout» — получили System.out.println().
const SNIPPETS: Snippet[] = [
  { label: 'sout', insert: 'System.out.println(${1});', detail: 'System.out.println()' },
  { label: 'souf', insert: 'System.out.printf("${1}%n"${2});', detail: 'System.out.printf()' },
  { label: 'psvm', insert: 'public static void main(String[] args) {\n\t$0\n}', detail: 'метод main' },
  { label: 'main', insert: 'public static void main(String[] args) {\n\t$0\n}', detail: 'метод main' },
  { label: 'fori', insert: 'for (int ${1:i} = 0; ${1:i} < ${2:n}; ${1:i}++) {\n\t$0\n}', detail: 'цикл for по индексу' },
  { label: 'foreach', insert: 'for (${1:int} ${2:item} : ${3:items}) {\n\t$0\n}', detail: 'цикл for-each' },
  { label: 'while', insert: 'while (${1:condition}) {\n\t$0\n}', detail: 'цикл while' },
  { label: 'if', insert: 'if (${1:condition}) {\n\t$0\n}', detail: 'условие if' },
  { label: 'ifelse', insert: 'if (${1:condition}) {\n\t$2\n} else {\n\t$0\n}', detail: 'условие if / else' },
  { label: 'switch', insert: 'switch (${1:value}) {\n\tcase ${2:1} -> ${3};\n\tdefault -> ${0};\n}', detail: 'switch со стрелками' },
  { label: 'trycatch', insert: 'try {\n\t$1\n} catch (${2:Exception} e) {\n\t$0\n}', detail: 'try / catch' },
  { label: 'scanner', insert: 'Scanner ${1:in} = new Scanner(System.in);', detail: 'чтение ввода' },
  { label: 'method', insert: 'static ${1:void} ${2:name}(${3}) {\n\t$0\n}', detail: 'статический метод' },
  { label: 'class', insert: 'class ${1:Name} {\n\t$0\n}', detail: 'класс' },
];

const KEYWORDS = [
  'abstract', 'boolean', 'break', 'byte', 'case', 'catch', 'char', 'class', 'continue', 'default', 'do',
  'double', 'else', 'enum', 'extends', 'final', 'finally', 'float', 'for', 'if', 'implements', 'import',
  'instanceof', 'int', 'interface', 'long', 'new', 'null', 'private', 'protected', 'public', 'record',
  'return', 'short', 'static', 'super', 'switch', 'this', 'throw', 'throws', 'true', 'false', 'try', 'var',
  'void', 'while',
];

const CLASSES: Record<string, string> = {
  String: 'java.lang',
  StringBuilder: 'java.lang',
  Math: 'java.lang',
  Integer: 'java.lang',
  Long: 'java.lang',
  Double: 'java.lang',
  Boolean: 'java.lang',
  Character: 'java.lang',
  System: 'java.lang',
  Object: 'java.lang',
  Scanner: 'java.util',
  Arrays: 'java.util',
  List: 'java.util',
  ArrayList: 'java.util',
  LinkedList: 'java.util',
  Map: 'java.util',
  HashMap: 'java.util',
  TreeMap: 'java.util',
  LinkedHashMap: 'java.util',
  Set: 'java.util',
  HashSet: 'java.util',
  TreeSet: 'java.util',
  Collections: 'java.util',
  Random: 'java.util',
  Locale: 'java.util',
  UUID: 'java.util',
  // Файлы
  Files: 'java.nio.file',
  Path: 'java.nio.file',
  Paths: 'java.nio.file',
  File: 'java.io',
  IOException: 'java.io',
  InputStream: 'java.io',
  FileWriter: 'java.io',
  FileReader: 'java.io',
  BufferedReader: 'java.io',
  BufferedWriter: 'java.io',
  UncheckedIOException: 'java.io',
  StandardCharsets: 'java.nio.charset',
  // Библиотеки: JSON (Gson) и YAML (SnakeYAML)
  Gson: 'com.google.gson',
  GsonBuilder: 'com.google.gson',
  JsonObject: 'com.google.gson',
  JsonArray: 'com.google.gson',
  JsonElement: 'com.google.gson',
  JsonParser: 'com.google.gson',
  JsonPrimitive: 'com.google.gson',
  JsonSyntaxException: 'com.google.gson',
  TypeToken: 'com.google.gson.reflect',
  SerializedName: 'com.google.gson.annotations',
  Yaml: 'org.yaml.snakeyaml',
  DumperOptions: 'org.yaml.snakeyaml',
  LoaderOptions: 'org.yaml.snakeyaml',
  YAMLException: 'org.yaml.snakeyaml.error',
  // Bukkit / MockBukkit для плагинов Minecraft
  JavaPlugin: 'org.bukkit.plugin.java',
  Listener: 'org.bukkit.event',
  EventHandler: 'org.bukkit.event',
  PlayerJoinEvent: 'org.bukkit.event.player',
  PlayerQuitEvent: 'org.bukkit.event.player',
  BlockBreakEvent: 'org.bukkit.event.block',
  BlockPlaceEvent: 'org.bukkit.event.block',
  CommandExecutor: 'org.bukkit.command',
  CommandSender: 'org.bukkit.command',
  Command: 'org.bukkit.command',
  BukkitRunnable: 'org.bukkit.scheduler',
  Player: 'org.bukkit.entity',
  Permission: 'org.bukkit.permissions',
  Material: 'org.bukkit',
  Location: 'org.bukkit',
  MockBukkit: 'org.mockbukkit.mockbukkit',
  ServerMock: 'org.mockbukkit.mockbukkit',
  PlayerMock: 'org.mockbukkit.mockbukkit.entity',
  PlayerSimulation: 'org.mockbukkit.mockbukkit.simulate.entity',
  // Fabric API: небольшой учебный runtime доступен в песочнице Byte
  ModInitializer: 'net.fabricmc.api',
  ClientModInitializer: 'net.fabricmc.api',
  ServerPlayConnectionEvents: 'net.fabricmc.fabric.api.networking.v1',
  ServerTickEvents: 'net.fabricmc.fabric.api.event.lifecycle.v1',
  ServerLifecycleEvents: 'net.fabricmc.fabric.api.event.lifecycle.v1',
  PlayerBlockBreakEvents: 'net.fabricmc.fabric.api.event.player',
  ClientTickEvents: 'net.fabricmc.fabric.api.client.event.lifecycle.v1',
  KeyBindingHelper: 'net.fabricmc.fabric.api.client.keybinding.v1',
  FabricLoader: 'net.fabricmc.loader.api',
  MinecraftServer: 'net.minecraft.server',
  ServerPlayerEntity: 'net.minecraft.server.network',
  Text: 'net.minecraft.text',
  BlockPos: 'net.minecraft.util.math',
  Block: 'org.bukkit.block',
  BlockState: 'net.minecraft.block',
  Blocks: 'net.minecraft.block',
  MinecraftClient: 'net.minecraft.client',
  KeyBinding: 'net.minecraft.client.option',
  InputUtil: 'net.minecraft.client.util',
};

let registered = false;

export function registerJavaCompletions(monaco: typeof Monaco): void {
  if (registered) return;
  registered = true;

  monaco.languages.registerCompletionItemProvider('java', {
    triggerCharacters: ['.'],
    provideCompletionItems(model, position) {
      const word = model.getWordUntilPosition(position);
      const range = new monaco.Range(position.lineNumber, word.startColumn, position.lineNumber, word.endColumn);
      const { CompletionItemKind, CompletionItemInsertTextRule } = monaco.languages;
      const source = model.getValue();

      // После точки — только методы и поля объекта слева от неё.
      const line = model.getLineContent(position.lineNumber);
      const dotIndex = word.startColumn - 2;
      if (dotIndex >= 0 && line[dotIndex] === '.') {
        const before = model.getValueInRange(
          new monaco.Range(1, 1, position.lineNumber, word.startColumn),
        );
        const resolved = resolveReceiver(before, receiverBefore(line, dotIndex));
        if (!resolved) return { suggestions: [] };
        const seen = new Set<string>();
        const suggestions: Monaco.languages.CompletionItem[] = [];
        for (const member of resolved.members) {
          const isField = member.params === null;
          suggestions.push({
            label: { label: member.name, detail: isField ? '' : `(${member.params})`, description: member.returns },
            kind: isField ? CompletionItemKind.Field : CompletionItemKind.Method,
            insertText: isField ? member.name : member.params ? `${member.name}($0)` : `${member.name}()`,
            insertTextRules: CompletionItemInsertTextRule.InsertAsSnippet,
            documentation: { value: memberDoc(member, resolved.typeName) },
            range,
            sortText: (seen.has(member.name) ? '1' : '0') + member.name,
          });
          seen.add(member.name);
        }
        return { suggestions };
      }

      const suggestions: Monaco.languages.CompletionItem[] = [
        ...SNIPPETS.map((s) => ({
          label: s.label,
          kind: CompletionItemKind.Snippet,
          insertText: s.insert,
          insertTextRules: CompletionItemInsertTextRule.InsertAsSnippet,
          detail: s.detail,
          range,
          sortText: '0' + s.label,
        })),
        ...KEYWORDS.map((k) => ({
          label: k,
          kind: CompletionItemKind.Keyword,
          insertText: k,
          range,
          sortText: '2' + k,
        })),
        ...Object.entries(CLASSES).map(([name, pkg]) => {
          const needsImport = pkg !== 'java.lang' && !hasImport(source, pkg, name);
          return {
            label: name,
            kind: CompletionItemKind.Class,
            insertText: name,
            detail: needsImport ? `${pkg}.${name} — добавит import` : `${pkg}.${name}`,
            range,
            sortText: '1' + name,
            additionalTextEdits: needsImport
              ? [{ range: new monaco.Range(1, 1, 1, 1), text: `import ${pkg}.${name};\n` }]
              : undefined,
          };
        }),
      ];
      return { suggestions };
    },
  });

  // Подсказка при наведении на метод или поле: описание на русском.
  monaco.languages.registerHoverProvider('java', {
    provideHover(model, position) {
      const word = model.getWordAtPosition(position);
      if (!word) return null;
      const line = model.getLineContent(position.lineNumber);
      const dotIndex = word.startColumn - 2;
      if (dotIndex < 0 || line[dotIndex] !== '.') return null;
      const before = model.getValueInRange(new monaco.Range(1, 1, position.lineNumber, word.startColumn));
      const resolved = resolveReceiver(before, receiverBefore(line, dotIndex));
      const after = line.slice(word.endColumn - 1).trimStart();
      const isCall = after.startsWith('(');
      const member = resolved?.members.find((x) => x.name === word.word && (x.params !== null) === isCall);
      if (!member || !resolved) return null;
      return {
        range: new monaco.Range(position.lineNumber, word.startColumn, position.lineNumber, word.endColumn),
        contents: [{ value: memberDoc(member, resolved.typeName) }],
      };
    },
  });
}

function memberDoc(member: Member, typeName: string): string {
  return ['```java', signature(member), '```', member.doc, typeName ? `\n_${typeName}_` : ''].join('\n');
}

function hasImport(source: string, pkg: string, name: string): boolean {
  return source.includes(`import ${pkg}.${name};`) || source.includes(`import ${pkg}.*;`);
}
