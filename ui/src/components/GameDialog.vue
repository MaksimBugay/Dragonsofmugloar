<script setup>
import { onBeforeUnmount, ref } from 'vue';
import { managePanelFocus } from '../presentation/panelFocus.js';
import GameIcon from './GameIcon.vue';

defineOptions({ inheritAttrs: false });
defineProps({
  id: { type: String, required: true },
  title: { type: String, required: true },
  icon: { type: String, default: 'book' },
  closeLabel: { type: String, default: '' },
});
const popup = ref(null);
const open = ref(false);
let releaseFocus;

function show() {
  releaseFocus = managePanelFocus(popup.value, close, { trapFocus: true });
  popup.value.showModal();
  open.value = true;
}
function close() {
  popup.value.close();
}
function handleClose() {
  open.value = false;
  releaseFocus?.();
  releaseFocus = undefined;
}
onBeforeUnmount(() => releaseFocus?.());
defineExpose({ show, close, open });
</script>

<template>
  <Teleport to="body">
    <dialog v-bind="$attrs" :id="id" ref="popup" class="game-dialog" :aria-labelledby="`${id}-title`" @close="handleClose">
      <header class="game-dialog-heading">
        <GameIcon :name="icon" /><h2 :id="`${id}-title`">{{ title }}</h2>
        <button class="icon-button" :aria-label="closeLabel || `Close ${title}`" autofocus @click="close"><GameIcon name="close" /></button>
      </header>
      <slot />
    </dialog>
  </Teleport>
</template>
