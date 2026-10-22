<script setup>
import GameIcon from './GameIcon.vue';
defineProps({ game: { type: Object, required: true } });
const stats = [
  { field: 'lives', name: 'Lives', icon: 'heart' },
  { field: 'gold', name: 'Gold', icon: 'coin' },
  { field: 'level', name: 'Dragon level', icon: 'dragon' },
  { field: 'score', name: 'Score', icon: 'star' },
  { field: 'highScore', name: 'High score', icon: 'crown' },
];
const number = new Intl.NumberFormat('en');
</script>

<template>
  <footer class="state-bar" aria-label="Current game state">
    <div class="trainer-mark"><GameIcon name="shield" /><div><span class="eyebrow">Your dragon</span><strong>A legend in the making</strong></div></div>
    <dl class="state-stats" aria-live="polite" aria-atomic="true">
      <div v-for="stat in stats" :key="stat.field" class="state-stat" :class="[stat.field, { critical: stat.field === 'lives' && game.lives <= 1 }]">
        <GameIcon :name="stat.icon" />
        <div><dt>{{ stat.name }}</dt><dd :key="game[stat.field]">{{ number.format(game[stat.field]) }}</dd></div>
      </div>
    </dl>
  </footer>
</template>
