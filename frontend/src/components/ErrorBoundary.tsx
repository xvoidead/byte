import { Component, type ErrorInfo, type ReactNode } from 'react';

const RELOAD_KEY = 'byte:chunk-reload';

/** Файл сборки не загрузился — обычно потому, что сайт обновился, пока страница была открыта. */
function isChunkLoadError(error: unknown): boolean {
  const message = error instanceof Error ? error.message : String(error);
  return /dynamically imported module|Importing a module script failed|error loading dynamically imported module|Loading chunk/i.test(
    message,
  );
}

/** Перезагружаем страницу один раз: новая версия index.html ссылается на новые файлы. */
function reloadOnce(): boolean {
  try {
    const last = Number(sessionStorage.getItem(RELOAD_KEY) ?? 0);
    if (Date.now() - last < 30_000) return false;
    sessionStorage.setItem(RELOAD_KEY, String(Date.now()));
  } catch {
    return false;
  }
  window.location.reload();
  return true;
}

interface Props {
  children: ReactNode;
}

interface State {
  error: unknown;
}

/** Вместо белого экрана при сбое показывает понятное сообщение и кнопку обновления. */
export class ErrorBoundary extends Component<Props, State> {
  state: State = { error: null };

  static getDerivedStateFromError(error: unknown): State {
    return { error };
  }

  componentDidCatch(error: unknown, info: ErrorInfo) {
    if (isChunkLoadError(error) && reloadOnce()) return;
    console.error('byte: ошибка интерфейса', error, info.componentStack);
  }

  render() {
    if (this.state.error === null) return this.props.children;
    const chunk = isChunkLoadError(this.state.error);
    return (
      <div className="page-state" role="alert">
        <h1>{chunk ? 'Сайт обновился' : 'Что-то пошло не так'}</h1>
        <p className="muted">
          {chunk
            ? 'Пока страница была открыта, вышла новая версия. Обновите страницу, чтобы продолжить.'
            : 'Произошла ошибка в интерфейсе. Ваш код и прогресс сохранены в браузере.'}
        </p>
        <button className="btn btn-primary" onClick={() => window.location.reload()}>
          Обновить страницу
        </button>
      </div>
    );
  }
}
