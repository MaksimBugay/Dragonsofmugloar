<script setup>
import SidePanel from './SidePanel.vue';
import GameIcon from './GameIcon.vue';
defineProps({
  open: Boolean, loading: Boolean, disabled: Boolean,
  items: { type: Array, required: true },
  gold: { type: Number, required: true },
  error: { type: String, default: '' },
  pending: { type: Object, default: null },
});
defineEmits(['toggle', 'refresh', 'buy']);
</script>

<template>
  <SidePanel id="potion-shop" side="left" title="Potion Shop" subtitle="The Gilded Flask" icon="flask" :open="open" @toggle="$emit('toggle')">
    <div class="shop-banner"><img src="/assets/items/hpot.svg" alt="" /><div><h3>A little magic.<br />A mighty difference.</h3><p>Elixirs & curios for your dragon.</p></div></div>
    <div class="shop-tools"><span><GameIcon name="coin" />{{ gold }} gold</span><button class="text-button" :disabled="loading || Boolean(pending)" @click="$emit('refresh')"><GameIcon name="refresh" :class="{ spinning: loading }" />Refresh</button></div>
    <p v-if="error" class="error-message" role="alert">{{ error }}</p>
    <p v-if="loading && !items.length" class="panel-empty" role="status">The apothecary is checking the shelves…</p>
    <div v-else-if="items.length" class="shop-grid" :aria-busy="loading">
      <div v-for="item in items" :key="item.id" class="shop-item" :class="{ unaffordable: item.cost > gold }">
        <button class="item-image" :title="item.name" :aria-label="`Buy ${item.name} for ${item.cost} gold`" :disabled="disabled || loading || Boolean(error) || item.cost > gold" @click="$emit('buy', item)">
          <img :src="`/assets/items/${item.id}.svg`" :alt="item.name" width="88" height="88" />
          <span class="item-effect" :title="item.features.livesDiff ? '+1 life' : `+${item.features.levelDiff} level`"><GameIcon :name="item.features.livesDiff ? 'heart' : 'dragon'" />+{{ item.features.livesDiff || item.features.levelDiff }}<span class="sr-only">{{ item.features.livesDiff ? ' life' : ' level' }}</span></span>
          <span v-if="pending?.type === 'purchase' && pending.id === item.id" class="item-pending"><GameIcon name="refresh" class="spinning" /></span>
        </button>
        <span class="item-name">{{ item.name }}</span>
        <span class="item-price"><GameIcon name="coin" />{{ item.cost }}<span v-if="item.cost > gold" class="sr-only"> · Not enough gold</span></span>
      </div>
    </div>
    <p v-else-if="!error" class="panel-empty">The shelves are empty. Refresh to check again.</p>
    <p class="shop-note">Choose an item to purchase it.<br />Purchases use one turn.</p>
  </SidePanel>
</template>
