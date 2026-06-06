import { useEffect, useRef } from 'react';

/**
 * Calls `fn` immediately and then every `intervalMs` milliseconds.
 * Pauses automatically when the browser tab is hidden, resumes on focus.
 * This gives the UI real-time updates without manual refresh.
 */
export function usePolling(fn: () => void | Promise<void>, intervalMs = 5000) {
  const saved = useRef(fn);
  saved.current = fn;

  useEffect(() => {
    let active = true;
    const tick = () => { if (active && !document.hidden) saved.current(); };

    // run once right away
    saved.current();
    const id = setInterval(tick, intervalMs);

    const onVis = () => { if (!document.hidden) saved.current(); };
    document.addEventListener('visibilitychange', onVis);

    return () => { active = false; clearInterval(id); document.removeEventListener('visibilitychange', onVis); };
  }, [intervalMs]);
}
