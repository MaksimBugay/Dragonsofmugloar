const stateFields = ['lives', 'gold', 'level', 'score', 'highScore', 'turn'];
const solveFields = ['lives', 'gold', 'score', 'highScore', 'turn'];
const purchaseFields = ['lives', 'gold', 'level', 'turn'];

function numericFields(result, fields) {
  return Object.fromEntries(fields.map((field) => {
    if (!Number.isSafeInteger(result[field]) || result[field] < 0) {
      throw new Error(`Invalid game response: ${field}`);
    }
    return [field, result[field]];
  }));
}

export function initialGame(result) {
  if (typeof result.gameId !== 'string' || !result.gameId) throw new Error('Missing game ID');
  return {
    gameId: result.gameId,
    ...numericFields(result, stateFields),
    lastInvestigatedReputation: { people: 0, state: 0, underworld: 0 },
    lastReputationInvestigationTurn: result.turn,
    startedAt: Date.now(),
    lastAction: 'START_GAME',
  };
}

export function applyAction(before, type, result) {
  const success = type === 'solve' ? result.success : result.shoppingSuccess;
  if (typeof success !== 'boolean') throw new Error('Missing action outcome');
  const updates = numericFields(result, type === 'solve' ? solveFields : purchaseFields);
  if (updates.turn < before.turn) throw new Error('The action returned an unexpected turn');
  return { ...before, ...updates, lastAction: type === 'solve' ? 'SOLVE_AD' : 'BUY_ITEM' };
}

export function stateDifferences(before, after) {
  return Object.fromEntries(stateFields.map((field) => [field, after[field] - before[field]]));
}

export function adventureEvent(type, subject, result, before, after) {
  return {
    id: `${before.gameId}-${after.turn}`,
    type,
    subject: structuredClone(subject),
    result: { ...result },
    success: type === 'solve' ? result.success : result.shoppingSuccess,
    before: structuredClone(before),
    after: structuredClone(after),
    differences: stateDifferences(before, after),
    createdAt: Date.now(),
  };
}
