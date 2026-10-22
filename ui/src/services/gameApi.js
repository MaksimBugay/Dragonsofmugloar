export const API_BASE = 'https://dragonsofmugloar.com/api/v2';

export class ApiError extends Error {
  constructor(message, { status = 0, uncertain = false } = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.uncertain = uncertain;
  }
}

async function readResponse(response, mutating) {
  if (!response.ok) {
    const descriptions = {
      400: 'The kingdom could not accept this action. Refresh the board before trying again.',
      401: 'This adventure is no longer available. Begin a new adventure.',
      404: 'This quest or adventure is no longer available. Refresh the board.',
      429: 'The kingdom is busy. Please wait a moment before trying again.',
    };
    throw new ApiError(descriptions[response.status] ?? `The kingdom is unavailable (HTTP ${response.status}).`, {
      status: response.status,
      uncertain: mutating && response.status >= 500,
    });
  }
  try {
    return await response.json();
  } catch {
    throw new ApiError('The kingdom returned an unreadable response.', { uncertain: mutating });
  }
}

export function createGameApi({ baseUrl = API_BASE, fetcher = globalThis.fetch, timeout = 20000 } = {}) {
  async function request(path, method = 'GET') {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeout);
    const mutating = method === 'POST';
    try {
      const response = await fetcher(`${baseUrl}${path}`, {
        method,
        headers: { Accept: 'application/json' },
        signal: controller.signal,
        cache: 'no-store',
        credentials: 'omit',
      });
      return await readResponse(response, mutating);
    } catch (error) {
      if (error instanceof ApiError) throw error;
      throw new ApiError(controller.signal.aborted
        ? 'The request timed out. Check your connection.'
        : 'Unable to reach the kingdom. Check your connection.', { uncertain: mutating });
    } finally {
      clearTimeout(timer);
    }
  }
  const segment = encodeURIComponent;
  return {
    startGame: () => request('/game/start', 'POST'),
    getAds: (gameId) => request(`/${segment(gameId)}/messages`),
    solveAd: (gameId, adId) => request(`/${segment(gameId)}/solve/${segment(adId)}`, 'POST'),
    getShop: (gameId) => request(`/${segment(gameId)}/shop`),
    buyItem: (gameId, itemId) => request(`/${segment(gameId)}/shop/buy/${segment(itemId)}`, 'POST'),
  };
}
