<script setup>
import { computed, ref } from 'vue';
import GameDialog from './GameDialog.vue';
import GameIcon from './GameIcon.vue';
import ReputationWidget from './ReputationWidget.vue';
import { formatReputation } from '../presentation/reputation.js';

const props = defineProps({
  game: { type: Object, required: true },
  loading: Boolean,
  disabled: Boolean,
  error: { type: String, default: '' },
});
const emit = defineEmits(['open', 'refresh']);
const popup = ref(null);
const open = computed(() => popup.value?.open ?? false);
const reputation = computed(() => props.game.lastInvestigatedReputation);
const value = computed(() => reputation.value == null ? '—/—'
  : `${formatReputation(reputation.value.people)}/${formatReputation(reputation.value.state)}`);
const description = computed(() => reputation.value == null
  ? 'Open reputation. People and State reputation have not been investigated.'
  : `Open reputation. People: ${formatReputation(reputation.value.people)}. State: ${formatReputation(reputation.value.state)}. Last checked on turn ${props.game.lastReputationInvestigationTurn}.`);

function showReputation() {
  emit('open');
  popup.value.show();
}
</script>

<template>
  <button class="turn-medallion reputation-banner" :style="{ '--reputation-digits': value.length }"
    :aria-label="description" :title="description" aria-haspopup="dialog"
    aria-controls="reputation-dialog" :aria-expanded="open" @click="showReputation">
    <GameIcon name="reputation" /><span>REPUTATION</span><strong aria-live="polite" aria-atomic="true">{{ value }}</strong>
  </button>
  <GameDialog id="reputation-dialog" ref="popup" class="reputation-dialog" title="Standing in the kingdom" icon="reputation" aria-label="Reputation" close-label="Close reputation">
    <ReputationWidget :game="game" :loading="loading" :disabled="disabled" :error="error" @refresh="$emit('refresh')" />
    <p class="reputation-dialog-note">Investigating your reputation uses one turn.</p>
  </GameDialog>
</template>
