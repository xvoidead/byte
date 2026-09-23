// Лёгкая подсветка Java для примеров в теории, чтобы не загружать Monaco ради статичного текста.

const KEYWORDS = new Set(
  (
    'abstract assert boolean break byte case catch char class const continue default do double else enum extends ' +
    'final finally float for goto if implements import instanceof int interface long native new package private ' +
    'protected public return short static strictfp super switch synchronized this throw throws transient try var ' +
    'void volatile while record sealed permits yield true false null'
  ).split(' '),
);

const TOKEN =
  /(\/\/[^\n]*|\/\*[\s\S]*?\*\/)|("(?:\\.|[^"\\\n])*"|'(?:\\.|[^'\\\n])*')|(@\w+)|\b(\d[\d_]*(?:\.\d+)?[LlFfDd]?)\b|\b([A-Za-z_$][\w$]*)\b/g;

export interface Token {
  text: string;
  kind?: 'comment' | 'string' | 'annotation' | 'number' | 'keyword' | 'type';
}

export function highlightJava(code: string): Token[] {
  const tokens: Token[] = [];
  let last = 0;
  for (const match of code.matchAll(TOKEN)) {
    const index = match.index ?? 0;
    if (index > last) tokens.push({ text: code.slice(last, index) });
    const [text, comment, string, annotation, number, word] = match;
    if (comment) tokens.push({ text, kind: 'comment' });
    else if (string) tokens.push({ text, kind: 'string' });
    else if (annotation) tokens.push({ text, kind: 'annotation' });
    else if (number) tokens.push({ text, kind: 'number' });
    else if (word && KEYWORDS.has(word)) tokens.push({ text, kind: 'keyword' });
    else if (word && /^[A-Z]/.test(word)) tokens.push({ text, kind: 'type' });
    else tokens.push({ text });
    last = index + text.length;
  }
  if (last < code.length) tokens.push({ text: code.slice(last) });
  return tokens;
}
