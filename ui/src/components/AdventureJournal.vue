<script setup>
import { computed } from 'vue';
import SidePanel from './SidePanel.vue';
import GameIcon from './GameIcon.vue';
import StateDeltas from './StateDeltas.vue';
const props = defineProps({ open: Boolean, history: { type: Array, required: true } });
defineEmits(['toggle']);
const events = computed(() => [...props.history].reverse());
</script>

<template>
  <SidePanel id="adventure-journal" side="right" title="Adventure Journal" subtitle="The chronicle of your deeds" icon="book" :open="open" :count="history.length" @toggle="$emit('toggle')">
    <div v-if="!events.length" class="journal-empty"><GameIcon name="feather" /><h3>A story yet unwritten</h3><p>Your quests, victories, and visits to the apothecary will be remembered here.</p></div>
    <ol v-else class="journal-events">
      <li v-for="event in events" :key="event.id" class="journal-event" :class="{ failed: !event.success }">
        <div class="event-top"><span>Turn {{ event.after.turn }}</span><GameIcon :name="event.type === 'purchase' ? 'flask' : event.success ? 'check' : 'close'" /></div>
        <h3>{{ event.type === 'purchase' ? (event.success ? 'A useful acquisition' : 'Purchase declined') : (event.success ? 'Bounty claimed' : 'A quest gone awry') }}</h3>
        <p class="event-subject">{{ event.type === 'solve' ? event.subject.adId : event.subject.name }}</p>
        <p class="event-description">{{ event.type === 'solve' ? event.subject.message : `${event.subject.cost} gold · ${event.subject.features.livesDiff ? '+1 life' : `+${event.subject.features.levelDiff} level`}` }}</p>
        <p class="event-result">{{ event.result.message || (event.success ? 'The purchase was successful.' : 'The purchase was unsuccessful.') }}</p>
        <StateDeltas :differences="event.differences" />
        <span class="event-outcome">{{ event.success ? 'Success' : 'Failure' }} · {{ event.differences.turn }} turn{{ event.differences.turn === 1 ? '' : 's' }} elapsed</span>
      </li>
    </ol>
  </SidePanel>
</template>
