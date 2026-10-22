<script setup>
import { computed } from 'vue';
import { adStatus, adReputation } from '../domain/ads.js';
import GameIcon from './GameIcon.vue';
const props = defineProps({
  ad: { type: Object, required: true },
  disabled: Boolean,
  pending: Boolean,
});
defineEmits(['solve']);
const reputation = computed(() => adReputation(props.ad));
const reputationIcons = { positive: 'thumbUp', negative: 'thumbDown', neutral: 'neutral' };
const reputationLabels = { positive: 'Positive', negative: 'Negative', neutral: 'Neutral' };
const status = computed(() => adStatus(props.ad));
const statusLabel = { easy: 'Favourable', risky: 'Dangerous', normal: 'Uncertain', trap: 'Trap', expired: 'Expired' };
</script>

<template>
  <article class="ad-notice parchment" :class="status" :aria-labelledby="`quest-${ad.adId}`" :data-status="status">
    <header class="notice-header">
      <div><span class="notice-kicker">Royal bounty · {{ statusLabel[status] }}</span><h3 :id="`quest-${ad.adId}`">{{ ad.adId }}</h3></div>
      <span class="wax-seal" :title="statusLabel[status]"><GameIcon :name="status === 'trap' ? 'flame' : status === 'risky' ? 'sword' : status === 'expired' ? 'clock' : 'dragon'" /></span>
    </header>
    <p class="quest-description">{{ ad.message }}</p>
    <div class="notice-bottom">
      <button class="button solve-button" :disabled="disabled || status === 'expired'" :aria-label="`Solve Ad ${ad.adId}`" @click="$emit('solve', ad)">
        <GameIcon :name="pending ? 'refresh' : 'sword'" :class="{ spinning: pending }" />
        {{ pending ? 'Questing…' : 'Solve Ad' }}
      </button>
      <dl class="quest-attributes">
        <div class="reward"><dt><GameIcon name="coin" /><span class="sr-only">Reward</span></dt><dd>{{ ad.reward }} <span>gold</span></dd></div>
        <div><dt><GameIcon name="clock" /><span class="sr-only">Expires in</span></dt><dd>{{ ad.expiresIn <= 0 ? 'Expired' : `${ad.expiresIn} turns left` }}</dd></div>
        <div class="probability"><dt><GameIcon name="shield" /><span class="sr-only">Probability</span></dt><dd>{{ ad.probability.label }}</dd></div>
        <div class="reputation-badge" :class="reputation" :title="`Reputation: ${reputationLabels[reputation].toLowerCase()}`"><dt class="sr-only">Reputation</dt><dd><GameIcon :name="reputationIcons[reputation]" /><span class="sr-only">{{ reputationLabels[reputation] }}</span></dd></div>
      </dl>
    </div>
  </article>
</template>
