import type { OutputFile, ProjectFile } from '../types';

export const MAIN_FILE = 'Main.java';
export const MAX_FILES = 30;

const SEGMENT = /^[A-Za-z0-9_][A-Za-z0-9_.-]{0,63}$/;

/** Язык подсветки по расширению файла. */
export function languageOf(name: string): string {
  const ext = name.slice(name.lastIndexOf('.') + 1).toLowerCase();
  switch (ext) {
    case 'java':
      return 'java';
    case 'yml':
    case 'yaml':
      return 'yaml';
    case 'json':
      return 'json';
    case 'properties':
    case 'ini':
    case 'toml':
      return 'ini';
    case 'md':
      return 'markdown';
    case 'xml':
      return 'xml';
    case 'sql':
      return 'sql';
    default:
      return 'plaintext';
  }
}

export const isSource = (name: string) => name.endsWith('.java') && !name.startsWith('resources/');

/** Ошибка в имени нового файла или null. Правила те же, что на сервере. */
export function nameProblem(name: string, files: ProjectFile[], except?: string): string | null {
  const trimmed = name.trim();
  if (!trimmed) return 'Введите имя файла';
  const segments = trimmed.split('/');
  if (segments.length > 3 || segments.some((s) => !SEGMENT.test(s)) || trimmed.includes('..')) {
    return 'Латиница, цифры, «_», «-» и «.»; папки через «/»';
  }
  if (files.some((f) => f.name !== except && f.name.toLowerCase() === trimmed.toLowerCase())) {
    return 'Такой файл уже есть';
  }
  if (!except && files.length >= MAX_FILES) return `Не больше ${MAX_FILES} файлов`;
  return null;
}

/** Содержимое нового файла: для класса — заготовка с его именем. */
export function newFileContent(name: string): string {
  if (!isSource(name)) return '';
  const base = name.slice(name.lastIndexOf('/') + 1, -'.java'.length);
  return /^[A-Za-z_$][\w$]*$/.test(base) ? `public class ${base} {\n    \n}\n` : '';
}

/** Порядок вкладок: Main.java, остальные исходники, файлы рабочей папки, ресурсы. */
export function sortFiles(files: ProjectFile[]): ProjectFile[] {
  const rank = (name: string) =>
    name === MAIN_FILE ? 0 : isSource(name) ? 1 : name.startsWith('resources/') ? 3 : 2;
  return [...files].sort((a, b) => rank(a.name) - rank(b.name) || a.name.localeCompare(b.name));
}

export interface FileChanges {
  created: string[];
  changed: string[];
  deleted: string[];
  /** Двоичные или большие файлы: их нельзя показать в редакторе. */
  binary: OutputFile[];
}

/**
 * Переносит в проект то, что программа сделала с файлами рабочей папки: как настоящий диск,
 * созданные файлы появляются во вкладках, изменённые обновляются, удалённые исчезают.
 */
export function applyOutputFiles(
  files: ProjectFile[],
  outputs: OutputFile[],
): { files: ProjectFile[]; changes: FileChanges } {
  const changes: FileChanges = { created: [], changed: [], deleted: [], binary: [] };
  const workNames = new Set(files.filter((f) => !isSource(f.name) && !f.name.startsWith('resources/')).map((f) => f.name));
  let next = files.filter((f) => {
    const keep = !workNames.has(f.name) || outputs.some((o) => o.name === f.name);
    if (!keep) changes.deleted.push(f.name);
    return keep;
  });
  for (const output of outputs) {
    if (output.content === null) {
      changes.binary.push(output);
      continue;
    }
    const existing = next.find((f) => f.name === output.name);
    if (!existing) {
      if (next.length >= MAX_FILES || isSource(output.name) || output.name.startsWith('resources/')) continue;
      next = [...next, { name: output.name, content: output.content }];
      changes.created.push(output.name);
    } else if (existing.content !== output.content) {
      next = next.map((f) => (f.name === output.name ? { ...f, content: output.content as string } : f));
      changes.changed.push(output.name);
    }
  }
  return { files: sortFiles(next), changes };
}

export function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} Б`;
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} КБ`;
  return `${(bytes / 1024 / 1024).toFixed(1)} МБ`;
}
