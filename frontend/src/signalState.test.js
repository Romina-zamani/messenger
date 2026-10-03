import { applySignalUpdate } from './signalState';

test('patches only the changed signal and preserves unchanged objects', () => {
  const unchanged = { id: 1, value: 10, version: 1 };
  const changed = { id: 2, value: 20, version: 1 };
  const current = new Map([[1, unchanged], [2, changed]]);

  const next = applySignalUpdate(current, { id: 2, value: 25, status: 'WARNING', version: 2 });

  expect(next).not.toBe(current);
  expect(next.get(1)).toBe(unchanged);
  expect(next.get(2)).toEqual({ id: 2, value: 25, status: 'WARNING', version: 2 });
});

test('ignores stale and duplicate signal versions', () => {
  const current = new Map([[2, { id: 2, value: 20, version: 3 }]]);
  expect(applySignalUpdate(current, { id: 2, value: 99, version: 3 })).toBe(current);
  expect(applySignalUpdate(current, { id: 2, value: 99, version: 2 })).toBe(current);
});
