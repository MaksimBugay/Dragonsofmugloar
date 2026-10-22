<script setup>
import { nextTick, onBeforeUnmount, ref, watch } from 'vue';
import { managePanelFocus } from '../presentation/panelFocus.js';
import GameIcon from './GameIcon.vue';
const props = defineProps({
  open: Boolean,
  id: { type: String, required: true },
  side: { type: String, required: true },
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  icon: { type: String, required: true },
  count: { type: Number, default: 0 },
});
const emit = defineEmits(['toggle']);
const panel = ref(null);
let cleanup;
watch(() => props.open, async (open) => {
  cleanup?.();
  cleanup = undefined;
  if (open) {
    await nextTick();
    if (props.open && panel.value) cleanup = managePanelFocus(panel.value, () => emit('toggle'));
  }
}, { immediate: true });
onBeforeUnmount(() => cleanup?.());
</script>

<template>
  <div class="side-panel-host" :class="[side, { 'is-open': open }]">
    <button class="panel-tab" :aria-expanded="open" :aria-controls="id" @click="$emit('toggle')">
      <GameIcon :name="icon" /><span>{{ title }}</span><span v-if="count" class="panel-count">{{ count }}</span>
    </button>
    <Transition name="backdrop"><button v-if="open" class="panel-backdrop" tabindex="-1" :aria-label="`Close ${title}`" @click="$emit('toggle')"></button></Transition>
    <Transition :name="`panel-${side}`">
      <aside v-if="open" :id="id" ref="panel" class="side-panel" :aria-labelledby="`${id}-title`">
        <header class="panel-heading"><GameIcon :name="icon" /><div><p class="eyebrow">{{ subtitle }}</p><h2 :id="`${id}-title`">{{ title }}</h2></div><button class="icon-button" :aria-label="`Close ${title}`" @click="$emit('toggle')"><GameIcon name="close" /></button></header>
        <div class="panel-content"><slot /></div>
        <div class="panel-flourish" aria-hidden="true">✦</div>
      </aside>
    </Transition>
  </div>
</template>
