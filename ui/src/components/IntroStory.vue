<script setup>
import GameIcon from './GameIcon.vue';
import story from '../content/story.json';
defineProps({ error: { type: String, default: '' } });
defineEmits(['start']);
</script>

<template>
  <main id="main-content" class="intro-view">
    <div class="intro-heading">
      <p class="eyebrow">A kingdom of opportunity. A world of adventure.</p>
      <h1>Every dragon has a destiny.</h1>
      <div class="ornament" aria-hidden="true"><span></span><GameIcon name="spark" /><span></span></div>
    </div>
    <section class="story-scroll parchment" aria-labelledby="story-title">
      <div class="scroll-rod top" aria-hidden="true"></div>
      <div class="story-heading"><span class="chapter-mark">I</span><p class="eyebrow" id="story-title">The tale of Mugloar</p></div>
      <div class="story-copy">
        <p v-for="(paragraph, index) in story.paragraphs" :key="index">{{ paragraph }}</p>
      </div>
      <div class="story-action">
        <p v-if="error" class="error-message" role="alert">{{ error }}</p>
        <button class="button button-primary start-button" @click="$emit('start')">
          <GameIcon name="sword" /> Start Game <GameIcon name="arrow" />
        </button>
        <p class="story-footnote">Your fortune is waiting to be written.</p>
      </div>
      <div class="scroll-rod bottom" aria-hidden="true"></div>
    </section>
    <details class="source-credits">
      <summary>Original story & credits</summary>
      <p>Story from <a :href="story.source" target="_blank" rel="noopener noreferrer">Dragons of Mugloar</a>.</p>
      <p>{{ story.attribution }}</p>
      <p>This interface uses original generated kingdom artwork.</p>
    </details>
  </main>
</template>
