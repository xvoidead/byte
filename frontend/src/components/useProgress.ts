import { useSyncExternalStore } from 'react';
import { completedLessons, subscribeProgress } from '../storage';

let cache = completedLessons();
let cacheKey = [...cache].sort().join(',');

function snapshot(): Set<string> {
  const fresh = completedLessons();
  const key = [...fresh].sort().join(',');
  if (key !== cacheKey) {
    cache = fresh;
    cacheKey = key;
  }
  return cache;
}

/** Множество slug-ов пройденных уроков; обновляется при прохождении нового урока. */
export function useProgress(): Set<string> {
  return useSyncExternalStore(subscribeProgress, snapshot, snapshot);
}
