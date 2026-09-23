import { useEffect, useRef, useState, useSyncExternalStore, type CSSProperties, type ReactNode } from 'react';

const REDUCED_QUERY = '(prefers-reduced-motion: reduce)';

function subscribeReduced(callback: () => void): () => void {
  const media = window.matchMedia?.(REDUCED_QUERY);
  media?.addEventListener('change', callback);
  return () => media?.removeEventListener('change', callback);
}

/** true, если пользователь попросил систему уменьшить движение. */
export function useReducedMotion(): boolean {
  return useSyncExternalStore(
    subscribeReduced,
    () => window.matchMedia?.(REDUCED_QUERY).matches ?? false,
    () => false,
  );
}

/**
 * Следит, виден ли элемент на экране.
 * С {@code once} перестаёт следить после первого появления — для анимаций «въезда».
 */
export function useInView<T extends Element>(options: { once?: boolean; threshold?: number; rootMargin?: string } = {}) {
  const { once = false, threshold = 0.2, rootMargin = '0px 0px -8% 0px' } = options;
  const ref = useRef<T | null>(null);
  const [inView, setInView] = useState(false);

  useEffect(() => {
    const element = ref.current;
    if (!element) return;
    if (typeof IntersectionObserver === 'undefined') {
      setInView(true);
      return;
    }
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setInView(true);
          if (once) observer.disconnect();
        } else if (!once) {
          setInView(false);
        }
      },
      { threshold, rootMargin },
    );
    observer.observe(element);
    return () => observer.disconnect();
  }, [once, threshold, rootMargin]);

  return [ref, inView] as const;
}

interface RevealProps {
  children: ReactNode;
  className?: string;
  /** Задержка появления в миллисекундах — для последовательного появления соседних блоков. */
  delay?: number;
  as?: 'div' | 'section';
}

/** Плавно проявляет содержимое, когда оно впервые попадает на экран. */
export function Reveal({ children, className = '', delay = 0, as: Tag = 'div' }: RevealProps) {
  const [ref, inView] = useInView<HTMLDivElement>({ once: true, threshold: 0.12 });
  const style = { '--reveal-delay': `${delay}ms` } as CSSProperties;
  return (
    <Tag ref={ref} className={`reveal ${inView ? 'is-visible' : ''} ${className}`} style={style}>
      {children}
    </Tag>
  );
}
