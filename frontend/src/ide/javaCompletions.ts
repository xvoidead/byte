import type * as Monaco from 'monaco-editor';

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
};

let registered = false;

export function registerJavaCompletions(monaco: typeof Monaco): void {
  if (registered) return;
  registered = true;

  monaco.languages.registerCompletionItemProvider('java', {
    provideCompletionItems(model, position) {
      const word = model.getWordUntilPosition(position);
      const range = new monaco.Range(position.lineNumber, word.startColumn, position.lineNumber, word.endColumn);
      const { CompletionItemKind, CompletionItemInsertTextRule } = monaco.languages;
      const source = model.getValue();

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
}

function hasImport(source: string, pkg: string, name: string): boolean {
  return source.includes(`import ${pkg}.${name};`) || source.includes(`import ${pkg}.*;`);
}
