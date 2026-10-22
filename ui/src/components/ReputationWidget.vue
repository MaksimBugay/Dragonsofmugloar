<script setup>
import { computed } from 'vue';
import GameIcon from './GameIcon.vue';
const props = defineProps({
  game: { type: Object, required: true },
  loading: Boolean,
  disabled: Boolean,
  error: { type: String, default: '' },
});
defineEmits(['refresh']);
const factions = [
  { id: 'people', label: 'People', icon: 'people' },
  { id: 'state', label: 'State', icon: 'crown' },
  { id: 'underworld', label: 'Underworld', icon: 'mask' },
];
const number = new Intl.NumberFormat('en', { maximumFractionDigits: 2, signDisplay: 'exceptZero' });
const reputation = computed(() => props.game.lastInvestigatedReputation);
const lastChecked = computed(() => props.game.lastReputationInvestigationTurn);
const freshness = computed(() => lastChecked.value == null ? 'Not checked' : `Checked · turn ${lastChecked.value}`);
</script>

<template>
  <section class="reputation-widget" aria-labelledby="reputation-title" :aria-busy="loading">
    <div class="reputation-heading">
      <span id="reputation-title" class="reputation-title"><GameIcon name="shield" />Reputation</span>
      <span class="reputation-checked" :title="lastChecked == null ? 'Refresh to investigate your standing with the three factions.' : `Last investigated on turn ${lastChecked}; current turn ${game.turn}.`">{{ freshness }}</span>
      <button class="reputation-refresh" :disabled="disabled || loading" aria-label="Refresh reputation · uses 1 turn" title="Investigate reputation · uses 1 turn" @click="$emit('refresh')"><GameIcon name="refresh" :class="{ spinning: loading }" /><span>1 turn</span></button>
    </div>
    <dl class="reputation-factions" aria-live="polite" aria-atomic="true">
      <div v-for="faction in factions" :key="faction.id" class="reputation-faction" :class="{ positive: reputation?.[faction.id] > 0, negative: reputation?.[faction.id] < 0 }" :title="`${faction.label}: ${reputation ? reputation[faction.id] : 'not investigated'}`">
        <dt><GameIcon :name="faction.icon" />{{ faction.label }}</dt>
        <dd>{{ reputation ? number.format(reputation[faction.id]) : '—' }}</dd>
      </div>
    </dl>
    <p v-if="error" class="reputation-error" role="alert"><GameIcon name="warning" />{{ error }}</p>
  </section>
</template>
