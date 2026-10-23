<script setup>
import { ref, watch } from 'vue';
import { createGameApi } from './services/gameApi.js';
import { createGameStore } from './stores/gameStore.js';
import GameIcon from './components/GameIcon.vue';
import IntroStory from './components/IntroStory.vue';
import TurnIndicator from './components/TurnIndicator.vue';
import CurrentGameState from './components/CurrentGameState.vue';
import AdsList from './components/AdsList.vue';
import ShopPanel from './components/ShopPanel.vue';
import AdventureJournal from './components/AdventureJournal.vue';
import ActionFeedback from './components/ActionFeedback.vue';
import ReputationBanner from './components/ReputationBanner.vue';
import { manageInactivityReload } from './presentation/inactivity.js';

const store = createGameStore(createGameApi());
const { game, ads, items, history, phase, pending, busy, gameOver, canAct, latestEvent,
  adsLoading, shopLoading, adsError, shopError, actionError, adsStale, uncertain, reputationError } = store;
const shopOpen = ref(false);
const journalOpen = ref(false);

watch(() => phase.value === 'active' ? game.value?.gameId : null, (gameId, previousGameId, onCleanup) => {
  if (gameId) onCleanup(manageInactivityReload(() => window.location.reload()));
}, { immediate: true });

function closePanels() {
  shopOpen.value = false;
  journalOpen.value = false;
}
function toggleShop() {
  shopOpen.value = !shopOpen.value;
  if (shopOpen.value) {
    journalOpen.value = false;
    store.refreshShop();
  }
}
function toggleJournal() {
  journalOpen.value = !journalOpen.value;
  if (journalOpen.value) shopOpen.value = false;
}
watch(() => game.value?.gameId, () => {
  shopOpen.value = false;
  journalOpen.value = false;
});
</script>

<template>
  <div class="game-shell" :class="{ 'in-adventure': phase === 'active' }">
    <a class="skip-link" href="#main-content">Skip to adventure</a>
    <div class="world-backdrop" aria-hidden="true"></div>
    <div class="ambient-motes" aria-hidden="true"><i v-for="n in 7" :key="n" :style="{ '--i': n }"></i></div>
    <header class="masthead">
      <div class="brand"><span class="brand-crest"><GameIcon name="dragon" /></span><div><strong>Dragons <span>of</span> Mugloar</strong><span class="brand-tagline">A dragon’s fortune</span></div></div>
      <div v-if="game && phase === 'active'" class="adventure-banners">
        <TurnIndicator :turn="game.turn" />
        <ReputationBanner :game="game" :loading="pending?.type === 'reputation'" :disabled="!canAct" :error="reputationError" @open="closePanels" @refresh="store.refreshReputation" />
      </div>
      <span v-else class="header-ornament" aria-hidden="true">✦</span>
      <div v-if="!game || phase !== 'active'" class="realm-status"><span class="realm-dot"></span>Welcome to the kingdom</div>
    </header>

    <IntroStory v-if="phase === 'intro'" :error="actionError" @start="store.startGame" />
    <main v-else-if="phase === 'starting'" id="main-content" class="starting-view" aria-live="polite"><div class="loading-crest"><GameIcon name="dragon" /></div><p class="eyebrow">A new chapter begins</p><h1>Opening the gates of Mugloar…</h1></main>
    <template v-else-if="game">
      <ShopPanel :open="shopOpen" :items="items" :gold="game.gold" :loading="shopLoading" :disabled="!canAct" :error="shopError" :pending="pending" @toggle="toggleShop" @refresh="store.refreshShop" @buy="store.buyItem" />
      <main id="main-content" class="adventure-main">
        <div class="adventure-heading"><span class="eyebrow"><GameIcon name="spark" /> Fortune favours the brave</span><span class="game-id" :title="`Game ${game.gameId}`">Adventure {{ game.gameId }}</span></div>
        <ActionFeedback :event="latestEvent" :error="actionError" :uncertain="uncertain" @restart="store.startGame" />
        <section v-if="gameOver" class="game-over parchment" aria-labelledby="game-over-title" role="status"><GameIcon name="crown" /><div><p class="eyebrow">Every legend has its ending</p><h2 id="game-over-title">Your adventure is complete</h2><p>You earned {{ game.score }} points in {{ game.turn }} turns. Your deeds live on in the journal.</p></div><button class="button button-primary" :disabled="busy" @click="store.startGame"><GameIcon name="dragon" />New adventure</button></section>
        <AdsList :ads="ads" :loading="adsLoading" :disabled="!canAct" :stale="adsStale" :error="adsError" :pending="pending" @solve="store.solveAd" @refresh="store.refreshAds" />
        <div class="adventure-caption"><GameIcon name="feather" /><span>Spend wisely. Choose bravely. Leave a legend.</span><button class="text-button restart-adventure" :disabled="busy" title="Start a fresh game and clear this adventure’s journal" @click="store.startGame">New adventure <GameIcon name="arrow" /></button></div>
      </main>
      <AdventureJournal :open="journalOpen" :history="history" @toggle="toggleJournal" />
      <CurrentGameState :game="game" />
    </template>
  </div>
</template>
