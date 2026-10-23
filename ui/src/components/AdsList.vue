<script setup>
import { computed, ref } from 'vue';
import { adStatus } from '../domain/ads.js';
import AdNotice from './AdNotice.vue';
import GameIcon from './GameIcon.vue';
import DragonCode from './DragonCode.vue';
const props = defineProps({
  ads: { type: Array, required: true },
  loading: Boolean, disabled: Boolean, stale: Boolean,
  error: { type: String, default: '' },
  pending: { type: Object, default: null },
});
defineEmits(['solve', 'refresh']);
const filter = ref('all');
const filters = [{ id: 'all', label: 'All bounties' }, { id: 'easy', label: 'Easy' }, { id: 'normal', label: 'Uncertain' }, { id: 'risky', label: 'Risky' }, { id: 'trap', label: 'Trap' }, { id: 'expired', label: 'Expired' }];
const visibleAds = computed(() => props.ads.filter((ad) => filter.value === 'all' || adStatus(ad) === filter.value));
const filterLabel = computed(() => filters.find((option) => option.id === filter.value).label.toLowerCase());
const count = (id) => id === 'all' ? props.ads.length : props.ads.filter((ad) => adStatus(ad) === id).length;
</script>

<template>
  <section class="bounty-board" aria-labelledby="board-title" :aria-busy="loading">
    <header class="board-heading">
      <div class="board-heading-symbol"><GameIcon name="scroll" /></div>
      <div><p class="eyebrow">By decree of the kingdom</p><h1 id="board-title">The Bounty Board</h1><p>Choose a quest. Claim your fortune.</p></div>
      <div class="board-heading-actions">
        <DragonCode />
        <button class="icon-button board-refresh" :disabled="loading || Boolean(pending)" aria-label="Refresh bounty board" title="Refresh bounties · does not use a turn" @click="$emit('refresh')"><GameIcon name="refresh" :class="{ spinning: loading }" /></button>
      </div>
    </header>
    <div class="board-tools">
      <div class="bounty-filters" role="group" aria-label="Filter bounties">
        <button v-for="option in filters" :key="option.id" :aria-pressed="filter === option.id" :class="{ selected: filter === option.id }" @click="filter = option.id">{{ option.label }} <span>{{ count(option.id) }}</span></button>
      </div>
      <span class="rank-note" title="Easy, Uncertain, Risky, then Expired; traps always last. Within each group: expected reward minus healing cost, then probability and sooner expiry.">Best prospects first</span>
    </div>
    <div v-if="error" class="board-error error-message" role="alert"><GameIcon name="warning" /><span>{{ error }} The board must refresh before you can solve another ad.</span><button class="text-button" :disabled="loading || Boolean(pending)" @click="$emit('refresh')">Try again</button></div>
    <div class="noticeboard-scroll" tabindex="0" aria-label="Available bounties">
      <div v-if="loading && !ads.length" class="board-empty" role="status"><GameIcon name="refresh" class="spinning" /><h2>Gathering the kingdom’s bounties…</h2><p>The royal messenger is on the way.</p></div>
      <div v-else-if="!visibleAds.length" class="board-empty"><GameIcon name="scroll" /><h2>No {{ filter === 'all' ? '' : filterLabel }} bounties here</h2><p>{{ filter === 'all' ? 'Refresh the noticeboard to check for available quests.' : 'Choose another category to explore the noticeboard.' }}</p></div>
      <div v-else class="notices-grid">
        <AdNotice v-for="ad in visibleAds" :key="ad.adId" :ad="ad" :disabled="disabled || loading || stale" :pending="pending?.type === 'solve' && pending.id === ad.adId" @solve="$emit('solve', $event)" />
      </div>
    </div>
    <footer class="board-footer"><span><i class="legend-dot easy"></i>Easy</span><span><i class="legend-dot normal"></i>Uncertain</span><span><i class="legend-dot risky"></i>Risky</span><span><i class="legend-dot trap"></i>Trap</span><span><i class="legend-dot expired"></i>Expired</span><span class="board-footer-note">Every action writes your story.</span></footer>
  </section>
</template>
