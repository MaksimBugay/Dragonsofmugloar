<script setup>
import GameIcon from './GameIcon.vue';
defineProps({ differences: { type: Object, required: true } });
const fields = [{ id: 'lives', icon: 'heart', label: 'lives' }, { id: 'gold', icon: 'coin', label: 'gold' }, { id: 'score', icon: 'star', label: 'score' }, { id: 'level', icon: 'dragon', label: 'level' }];
</script>

<template>
  <dl class="state-deltas">
    <div v-for="field in fields" :key="field.id" :class="{ positive: differences[field.id] > 0, negative: differences[field.id] < 0 }">
      <dt><GameIcon :name="field.icon" /><span class="sr-only">{{ field.label }}</span></dt><dd>{{ differences[field.id] > 0 ? '+' : '' }}{{ differences[field.id] }} <span>{{ field.label }}</span></dd>
    </div>
  </dl>
</template>
