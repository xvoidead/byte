import * as monaco from 'monaco-editor';
import EditorWorker from 'monaco-editor/editor/editor.worker?worker';
import { loader } from '@monaco-editor/react';
import { registerJavaCompletions } from './javaCompletions';

// Monaco подключается из node_modules, а не с CDN: сайт работает без внешних запросов.
self.MonacoEnvironment = {
  getWorker: () => new EditorWorker(),
};

loader.config({ monaco });

monaco.editor.defineTheme('byte-dark', {
  base: 'vs-dark',
  inherit: true,
  rules: [
    { token: 'keyword', foreground: 'cc7832' },
    { token: 'string', foreground: '6aab73' },
    { token: 'number', foreground: '2aacb8' },
    { token: 'comment', foreground: '7a7e85', fontStyle: 'italic' },
    { token: 'annotation', foreground: 'b3ae60' },
    { token: 'type.identifier', foreground: 'e0e2e8' },
  ],
  colors: {
    'editor.background': '#15171f',
    'editor.lineHighlightBackground': '#1d2029',
    'editorLineNumber.foreground': '#4b5060',
    'editorLineNumber.activeForeground': '#a9aebb',
    'editorGutter.background': '#15171f',
    'editor.selectionBackground': '#2b4a7a',
    'editorCursor.foreground': '#f89820',
  },
});

registerJavaCompletions(monaco);

export { monaco };
