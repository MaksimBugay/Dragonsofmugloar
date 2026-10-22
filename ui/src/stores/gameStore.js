import { computed, readonly, ref, toRaw } from 'vue';
import { ageAds, decodeAd, rankAds } from '../domain/ads.js';
import { adventureEvent, applyAction, applyReputation, initialGame } from '../domain/game.js';
import { shopItem } from '../domain/shop.js';

function decodeListing(response, decode, message) {
  if (!Array.isArray(response)) throw new Error(message);
  return response.map(decode);
}

export function createGameStore(api) {
  const game = ref(null);
  const ads = ref([]);
  const items = ref([]);
  const history = ref([]);
  const phase = ref('intro');
  const pending = ref(null);
  const adsLoading = ref(false);
  const shopLoading = ref(false);
  const adsError = ref('');
  const shopError = ref('');
  const actionError = ref('');
  const reputationError = ref('');
  const adsStale = ref(true);
  const uncertain = ref(false);
  let adsRequest = 0;
  let shopRequest = 0;

  const busy = computed(() => pending.value !== null);
  const gameOver = computed(() => Boolean(game.value && game.value.lives <= 0));
  const canAct = computed(() => Boolean(game.value) && !busy.value && !gameOver.value && !uncertain.value);
  const sortedAds = computed(() => rankAds(ads.value));
  const latestEvent = computed(() => history.value.at(-1) ?? null);

  async function refreshAds() {
    if (!game.value || gameOver.value) return;
    const request = ++adsRequest;
    const { gameId, turn } = game.value;
    adsLoading.value = true;
    adsError.value = '';
    try {
      const response = await api.getAds(gameId);
      if (request !== adsRequest || game.value?.gameId !== gameId || game.value.turn !== turn) return;
      ads.value = decodeListing(response, decodeAd, 'The noticeboard returned an invalid listing.');
      adsStale.value = false;
    } catch (error) {
      if (request !== adsRequest || game.value?.gameId !== gameId || game.value.turn !== turn) return;
      adsError.value = error.message;
      adsStale.value = true;
    } finally {
      if (request === adsRequest) adsLoading.value = false;
    }
  }

  async function refreshShop() {
    if (!game.value) return;
    const request = ++shopRequest;
    const { gameId } = game.value;
    shopLoading.value = true;
    shopError.value = '';
    try {
      const response = await api.getShop(gameId);
      if (request !== shopRequest || game.value?.gameId !== gameId) return;
      items.value = decodeListing(response, shopItem, 'The apothecary returned an invalid listing.');
    } catch (error) {
      if (request === shopRequest && game.value?.gameId === gameId) shopError.value = error.message;
    } finally {
      if (request === shopRequest) shopLoading.value = false;
    }
  }

  async function startGame() {
    if (busy.value) return;
    const previousPhase = phase.value;
    phase.value = 'starting';
    pending.value = { type: 'start' };
    actionError.value = '';
    try {
      const next = initialGame(await api.startGame());
      ++adsRequest;
      ++shopRequest;
      game.value = next;
      ads.value = [];
      items.value = [];
      history.value = [];
      shopLoading.value = false;
      shopError.value = '';
      reputationError.value = '';
      uncertain.value = false;
      adsStale.value = true;
      phase.value = 'active';
      await refreshAds();
    } catch (error) {
      actionError.value = error.message;
      phase.value = previousPhase;
    } finally {
      pending.value = null;
    }
  }

  async function refreshReputation() {
    if (!canAct.value) return;
    pending.value = { type: 'reputation' };
    reputationError.value = '';
    actionError.value = '';
    const before = structuredClone(toRaw(game.value));
    let receivedResult = false;
    try {
      const result = await api.investigateReputation(before.gameId);
      receivedResult = true;
      const after = applyReputation(before, result);
      const reputation = after.lastInvestigatedReputation;
      const message = `People: ${reputation.people} · State: ${reputation.state} · Underworld: ${reputation.underworld}`;
      const event = adventureEvent('reputation', { name: 'Reputation investigation', message: 'A report from the kingdom’s informants.' },
        { ...result, message }, before, after);
      game.value = after;
      history.value.push(event);
      ads.value = ageAds(ads.value, after.turn - before.turn);
      adsStale.value = true;
      await refreshAds();
    } catch (error) {
      uncertain.value = Boolean(error.uncertain || receivedResult);
      reputationError.value = error.message;
      if (uncertain.value) {
        actionError.value = `${error.message} The investigation may have reached the server and used a turn. Begin a new adventure to continue safely.`;
      }
    } finally {
      pending.value = null;
    }
  }

  async function perform(type, subject) {
    if (!canAct.value) return;
    // Use the current listing, never a stale card or arbitrary caller-supplied cost.
    const current = type === 'solve'
      ? ads.value.find((ad) => ad.adId === subject.adId)
      : items.value.find((item) => item.id === subject.id);
    if (!current) return;
    if (type === 'solve' && (adsStale.value || adsLoading.value || current.expiresIn <= 0)) return;
    if (type === 'purchase' && (shopLoading.value || shopError.value || current.cost > game.value.gold)) return;

    pending.value = { type, id: type === 'solve' ? current.adId : current.id };
    actionError.value = '';
    const before = structuredClone(toRaw(game.value));
    const selected = structuredClone(toRaw(current));
    let receivedResult = false;
    try {
      const result = type === 'solve'
        ? await api.solveAd(before.gameId, selected.adId)
        : await api.buyItem(before.gameId, selected.id);
      receivedResult = true;
      const after = applyAction(before, type, result);
      const event = adventureEvent(type, selected, result, before, after);
      game.value = after;
      history.value.push(event);
      if (after.turn !== before.turn) {
        ads.value = ageAds(ads.value, after.turn - before.turn, type === 'solve' ? selected.adId : undefined);
        adsStale.value = true;
        // The action is already committed. A failed read must never roll it back or retry it.
        await refreshAds();
      }
      if (type === 'purchase' && !gameOver.value) await refreshShop();
    } catch (error) {
      uncertain.value = Boolean(error.uncertain || receivedResult);
      actionError.value = uncertain.value
        ? `${error.message} The action may have reached the server. Its outcome cannot be recovered; begin a new adventure to continue safely.`
        : error.message;
    } finally {
      pending.value = null;
    }
  }

  return {
    game: readonly(game), ads: sortedAds, items: readonly(items), history: readonly(history),
    phase: readonly(phase), pending: readonly(pending), busy, gameOver, canAct, latestEvent,
    adsLoading: readonly(adsLoading), shopLoading: readonly(shopLoading),
    adsError: readonly(adsError), shopError: readonly(shopError), actionError: readonly(actionError),
    reputationError: readonly(reputationError),
    adsStale: readonly(adsStale), uncertain: readonly(uncertain),
    startGame, refreshAds, refreshShop, refreshReputation,
    solveAd: (ad) => perform('solve', ad),
    buyItem: (item) => perform('purchase', item),
  };
}
