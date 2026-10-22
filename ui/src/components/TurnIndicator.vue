<script setup>
import GameIcon from './GameIcon.vue';
defineProps({ turn: { type: Number, required: true } });
</script>

<template>
  <div class="turn-indicator" aria-label="Current turn" aria-live="polite" aria-atomic="true">
    <span class="turn-wing" aria-hidden="true"></span>
    <div class="turn-medallion"><GameIcon name="clock" /><span>TURN</span><strong :key="turn">{{ turn }}</strong></div>
    <span class="turn-wing" aria-hidden="true"></span>
  </div>
</template>
