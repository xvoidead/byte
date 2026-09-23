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
    { token: '', foreground: 'd6d9e0' },
    { token: 'keyword', foreground: 'a8b5ff' },
    { token: 'string', foreground: '9fdcb6' },
    { token: 'number', foreground: 'f2c490' },
    { token: 'comment', foreground: '5d626d', fontStyle: 'italic' },
    { token: 'annotation', foreground: 'd4b0ff' },
    { token: 'type.identifier', foreground: 'f2f3f5' },
    { token: 'delimiter', foreground: '8a8f98' },
  ],
  colors: {
    'editor.background': '#0b0c0e',
    'editor.foreground': '#d6d9e0',
    'editor.lineHighlightBackground': '#ffffff08',
    'editor.lineHighlightBorder': '#00000000',
    'editorLineNumber.foreground': '#3a3d44',
    'editorLineNumber.activeForeground': '#8a8f98',
    'editorGutter.background': '#0b0c0e',
    'editor.selectionBackground': '#a8b5ff33',
    'editor.inactiveSelectionBackground': '#a8b5ff1a',
    'editorCursor.foreground': '#a8b5ff',
    'editorIndentGuide.background1': '#ffffff0d',
    'editorIndentGuide.activeBackground1': '#ffffff24',
    'editorBracketMatch.background': '#a8b5ff1f',
    'editorBracketMatch.border': '#a8b5ff55',
    'editorWidget.background': '#141518',
    'editorWidget.border': '#ffffff14',
    'editorSuggestWidget.background': '#141518',
    'editorSuggestWidget.border': '#ffffff14',
    'editorSuggestWidget.selectedBackground': '#a8b5ff26',
    'editorHoverWidget.background': '#141518',
    'editorHoverWidget.border': '#ffffff14',
    'scrollbarSlider.background': '#ffffff12',
    'scrollbarSlider.hoverBackground': '#ffffff1f',
    'scrollbarSlider.activeBackground': '#ffffff2e',
    'editorError.foreground': '#ff7a85',
    'editorWarning.foreground': '#f5c56e',
  },
});

// Веб-шрифт может догрузиться после создания редактора — пересчитываем ширину символов.
document.fonts?.ready.then(() => monaco.editor.remeasureFonts());

registerJavaCompletions(monaco);

export { monaco };
