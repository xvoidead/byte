// Предсжатие файлов сборки: сервер отдаёт .br или .gz, если браузер их поддерживает.
import { readdir, readFile, writeFile, stat } from 'node:fs/promises';
import { join, extname } from 'node:path';
import { brotliCompressSync, gzipSync, constants } from 'node:zlib';

const DIST = new URL('../dist/assets/', import.meta.url).pathname;
const TYPES = new Set(['.js', '.css', '.svg', '.json', '.ttf']);
const MIN_SIZE = 1024;

let saved = 0;
let count = 0;
for (const name of await readdir(DIST)) {
  const file = join(DIST, name);
  if (!TYPES.has(extname(name)) || (await stat(file)).size < MIN_SIZE) continue;
  const data = await readFile(file);
  const br = brotliCompressSync(data, { params: { [constants.BROTLI_PARAM_QUALITY]: 11 } });
  const gz = gzipSync(data, { level: 9 });
  await writeFile(file + '.br', br);
  await writeFile(file + '.gz', gz);
  saved += data.length - br.length;
  count++;
}
console.log(`Сжато файлов: ${count}, экономия (brotli): ${(saved / 1024 / 1024).toFixed(1)} МБ`);
