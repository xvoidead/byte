import { useEffect, useRef, useState, type KeyboardEvent } from 'react';
import type { ProjectFile } from '../types';
import { isSource, languageOf, nameProblem } from './project';

interface FileTabsProps {
  files: ProjectFile[];
  active: string;
  /** Сколько ошибок компиляции в каждом файле. */
  errors: Record<string, number>;
  onSelect: (name: string) => void;
  onCreate: (name: string) => void;
  onRename: (from: string, to: string) => void;
  onDelete: (name: string) => void;
}

/** Вкладки файлов проекта: переключение, создание («+»), переименование (двойной щелчок) и удаление. */
export function FileTabs({ files, active, errors, onSelect, onCreate, onRename, onDelete }: FileTabsProps) {
  const [editing, setEditing] = useState<{ mode: 'create' } | { mode: 'rename'; name: string } | null>(null);
  const activeRef = useRef<HTMLButtonElement>(null);
  const sources = files.filter((f) => isSource(f.name)).length;

  useEffect(() => {
    activeRef.current?.scrollIntoView({ block: 'nearest', inline: 'nearest' });
  }, [active]);

  const remove = (name: string) => {
    if (isSource(name) && sources <= 1) return;
    const file = files.find((f) => f.name === name);
    if (file?.content.trim() && !window.confirm(`Удалить файл ${name}?`)) return;
    onDelete(name);
  };

  return (
    <div className="file-tabs" role="tablist" aria-label="Файлы проекта">
      {files.map((file) =>
        editing?.mode === 'rename' && editing.name === file.name ? (
          <NameInput
            key={file.name}
            initial={file.name}
            files={files}
            except={file.name}
            onDone={(name) => {
              setEditing(null);
              if (name && name !== file.name) onRename(file.name, name);
            }}
          />
        ) : (
          <div key={file.name} className={`file-tab ${file.name === active ? 'active' : ''}`}>
            <button
              ref={file.name === active ? activeRef : undefined}
              role="tab"
              aria-selected={file.name === active}
              className="file-tab-name"
              onClick={() => onSelect(file.name)}
              onDoubleClick={() => setEditing({ mode: 'rename', name: file.name })}
              title={`${file.name} — двойной щелчок, чтобы переименовать`}
            >
              <FileIcon name={file.name} />
              {file.name}
              {errors[file.name] > 0 && <span className="file-tab-errors" aria-label={`ошибок: ${errors[file.name]}`} />}
            </button>
            {!(isSource(file.name) && sources <= 1) && (
              <button className="file-tab-close" onClick={() => remove(file.name)} aria-label={`Удалить ${file.name}`}>
                ×
              </button>
            )}
          </div>
        ),
      )}
      {editing?.mode === 'create' ? (
        <NameInput
          initial=""
          files={files}
          onDone={(name) => {
            setEditing(null);
            if (name) onCreate(name);
          }}
        />
      ) : (
        <button className="file-tab-add" onClick={() => setEditing({ mode: 'create' })} title="Новый файл" aria-label="Новый файл">
          +
        </button>
      )}
    </div>
  );
}

function NameInput({
  initial,
  files,
  except,
  onDone,
}: {
  initial: string;
  files: ProjectFile[];
  except?: string;
  onDone: (name: string | null) => void;
}) {
  const [value, setValue] = useState(initial);
  const [touched, setTouched] = useState(false);
  const problem = nameProblem(value, files, except);
  const ref = useRef<HTMLInputElement>(null);

  useEffect(() => {
    const input = ref.current;
    if (!input) return;
    input.focus();
    // При переименовании выделяем имя без расширения, как в IDE.
    const dot = initial.lastIndexOf('.');
    input.setSelectionRange(0, dot > 0 ? dot : initial.length);
  }, [initial]);

  const submit = () => {
    setTouched(true);
    if (!problem) onDone(value.trim());
  };

  const handleKey = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      submit();
    } else if (e.key === 'Escape') {
      e.preventDefault();
      onDone(null);
    }
  };

  return (
    <span className="file-tab-input">
      <input
        ref={ref}
        value={value}
        placeholder="Config.java"
        spellCheck={false}
        autoComplete="off"
        aria-label="Имя файла"
        aria-invalid={touched && !!problem}
        onChange={(e) => setValue(e.target.value)}
        onKeyDown={handleKey}
        onBlur={() => (problem ? onDone(null) : submit())}
      />
      {touched && problem && <span className="file-tab-problem">{problem}</span>}
    </span>
  );
}

function FileIcon({ name }: { name: string }) {
  const language = languageOf(name);
  const label =
    language === 'java' ? 'J' : language === 'yaml' ? 'Y' : language === 'json' ? '{}' : language === 'ini' ? '=' : '·';
  return (
    <span className={`file-icon file-icon-${language}`} aria-hidden="true">
      {label}
    </span>
  );
}
