import { useEffect } from 'react';

export const SITE_TITLE = 'byte — Java в браузере';

/** Заголовок вкладки браузера для текущей страницы. */
export function useTitle(title: string | null | undefined) {
  useEffect(() => {
    document.title = title ?? SITE_TITLE;
  }, [title]);
}
