export function applySignalUpdate(current, update) {
  const existing = current.get(update.id);
  if (!existing || update.version <= existing.version) return current;
  const next = new Map(current);
  next.set(update.id, { ...existing, ...update });
  return next;
}
