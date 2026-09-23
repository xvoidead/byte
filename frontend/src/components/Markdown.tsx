import ReactMarkdown, { type Components } from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { highlightJava } from './highlightJava';

const components: Components = {
  code({ className, children }) {
    const text = String(children ?? '');
    const language = /language-(\w+)/.exec(className ?? '')?.[1];
    if (language === 'java') {
      return (
        <code className={className}>
          {highlightJava(text.replace(/\n$/, '')).map((token, i) =>
            token.kind ? (
              <span key={i} className={`tok-${token.kind}`}>
                {token.text}
              </span>
            ) : (
              token.text
            ),
          )}
        </code>
      );
    }
    return <code className={className}>{children}</code>;
  },
  table({ children }) {
    return (
      <div className="table-wrap">
        <table>{children}</table>
      </div>
    );
  },
};

export function Markdown({ children }: { children: string }) {
  return (
    <div className="markdown">
      <ReactMarkdown remarkPlugins={[remarkGfm]} components={components}>
        {children}
      </ReactMarkdown>
    </div>
  );
}
