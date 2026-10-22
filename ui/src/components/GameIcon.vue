<script setup>
defineProps({ name: { type: String, required: true } });
const paths = {
  dragon: 'M4 18c1-4 3-6 7-6l-2-3 4-6 1 4 5-2-1 4 3 2-5 1-2 4 4 4-5-1-3-3c-4 0-5 3-8 3l2-1ZM9 12 3 5l1 7-3 2 8-2Z',
  sword: 'm14 3 7-1-1 7-9 9-5-5 8-10ZM4 12l8 8M3 21l5-5M2 20l2 2',
  coin: 'M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18Zm0 3a6 6 0 1 0 0 12 6 6 0 0 0 0-12Zm0 2v8m2-7h-3a1.5 1.5 0 0 0 0 3h2a1.5 1.5 0 0 1 0 3h-3',
  heart: 'M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1.1-1.1a5.5 5.5 0 0 0-7.8 7.8L12 21l8.8-8.6a5.5 5.5 0 0 0 0-7.8Z',
  crown: 'm3 6 4 4 5-7 5 7 4-4-2 13H5L3 6Zm2 10h14',
  star: 'm12 2 3 6.5 7 1-5 5 1.2 7-6.2-3.3-6.2 3.3 1.2-7-5-5 7-1L12 2Z',
  clock: 'M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18Zm0 3v6l4 2',
  close: 'm6 6 12 12M6 18 18 6',
  flask: 'M9 2h6m-5 0v6L4 19a2 2 0 0 0 2 3h12a2 2 0 0 0 2-3L14 8V2M7 14h10M8 18h2',
  refresh: 'M20 8A8.5 8.5 0 0 0 5 5L2 8m0-6v6h6m-4 8a8.5 8.5 0 0 0 15 3l3-3m0 6v-6h-6',
  check: 'm4 12 5 5L20 6',
  feather: 'M4 21 17 8M5 17l1-8 7-6c7-4 10 2 5 7l-6 7H5Zm4-5h6',
  book: 'M3 3h7l2 2 2-2h7v16h-7l-2 2-2-2H3V3Zm9 2v16M6 7h3M6 11h3m6-4h3m-3 4h3',
  scroll: 'M7 3h12a2 2 0 1 1-2 2v13a3 3 0 0 1-3 3H5a2 2 0 1 1 2-2V5a2 2 0 0 0-4 0v2h4m3 1h4m-4 4h4',
  shield: 'm12 2 9 4-1 9-8 7-8-7-1-9 9-4Zm0 4v12',
  flame: 'M12 3c1 5-5 6-5 11a5 5 0 0 0 10 0c0-3-2-5-3-6 0 3-2 4-3 5 1-4 3-6 1-10Zm0 12c-2 2-3 3-3 4a3 3 0 0 0 6 0c0-2-2-3-3-4Z',
  warning: 'm12 3 10 18H2L12 3Zm0 6v5m0 3v1',
  arrow: 'M3 12h17m-6-6 6 6-6 6',
  spark: 'm12 2 3 7 7 3-7 3-3 7-3-7-7-3 7-3 3-7Z',
};
</script>

<template>
  <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"
    stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false">
    <path :d="paths[name] || paths.spark" />
  </svg>
</template>
