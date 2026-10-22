<script setup>
import { ref, watch } from 'vue';
import GameIcon from './GameIcon.vue';
import StateDeltas from './StateDeltas.vue';
const props = defineProps({ event: { type: Object, default: null }, error: { type: String, default: '' }, uncertain: Boolean });
defineEmits(['journal', 'restart']);

const feedbackVisible = ref(false);

watch(() => props.event, (event) => {
  feedbackVisible.value = Boolean(event);
}, { immediate: true });
</script>

<template>
  <div v-if="error" class="action-feedback error-feedback" role="alert"><GameIcon name="warning" /><div><strong>The messenger brings a warning</strong><p>{{ error }}</p><button v-if="uncertain" class="text-button" @click="$emit('restart')">Begin a new adventure <GameIcon name="arrow" /></button></div></div>
  <div v-else-if="event && feedbackVisible" :key="event.id" class="action-feedback" :class="{ 'failed-feedback': !event.success }" role="status" aria-live="polite">
    <GameIcon :name="event.success ? 'check' : 'shield'" />
    <div><strong>{{ event.type === 'reputation' ? 'Reputation investigated' : event.type === 'purchase' ? (event.success ? `${event.subject.name} purchased` : 'Purchase unsuccessful') : (event.success ? 'Bounty claimed!' : 'The quest was unsuccessful') }}</strong><p v-if="event.result.message">{{ event.result.message }}</p><StateDeltas :differences="event.differences" /></div>
    <span class="feedback-actions">
      <button class="icon-button" aria-label="View event in Adventure Journal" title="Read your journal" @click="$emit('journal')"><GameIcon name="book" /></button>
      <button class="text-button" aria-label="Hide feedback" @click="feedbackVisible = false"><GameIcon name="close" />Hide</button>
    </span>
  </div>
</template>
