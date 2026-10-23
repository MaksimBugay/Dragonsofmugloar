<script setup>
import { ref } from 'vue';
import GameDialog from './GameDialog.vue';
import GameIcon from './GameIcon.vue';

const popup = ref(null);
</script>

<template>
  <button class="button dragon-code-button" title="Dragon Trainer’s Code" aria-haspopup="dialog"
    aria-controls="dragon-code" :aria-expanded="popup?.open ?? false" @click="popup.show()">
    <GameIcon name="book" /><span class="dragon-code-label">Dragon Trainer’s Code</span>
  </button>
  <GameDialog id="dragon-code" ref="popup" class="dragon-code-dialog" title="Dragon Trainer’s Code" icon="book">
    <div class="dragon-code-manuscript" tabindex="0" role="region" aria-label="The seven rules of survival">
      <p class="dragon-code-kicker">Seven rules · raise a legend</p>
      <p class="dragon-code-intro">A wise trainer keeps their dragon healthy, chooses battles carefully, and saves gold for hard days. Guide your dragon well, and the kingdom will remember you both.</p>
      <ol class="dragon-code-rules">
        <li><h3>Keep your dragon alive</h3><p>Always keep enough gold to buy your dragon +1 life. When it has only one life left, buy another if you can afford it. A little care today keeps your companion flying tomorrow.</p></li>
        <li><h3>Choose the easy victories</h3><p>Send your dragon on Easy quests, starting at the top of the board. Keep going while easy quests remain. Build your gold before taking greater risks.</p></li>
        <li><h3>Help your dragon grow stronger</h3><p>When Easy quests run out, buy a +1 or +2 level upgrade if you can afford it while keeping your life reserve. Then check the board for Easy quests again. A stronger dragon may find the same trials easier.</p></li>
        <li><h3>Take on uncertain trials</h3><p>Move on to Uncertain quests. After every attempt, check for Easy quests again and take those first. A good trainer spots the safer path.</p></li>
        <li><h3>Build your dragon’s strength again</h3><p>If no Uncertain quests remain either, buy a +1 or +2 level upgrade if your gold allows, keeping your life reserve. Check again for Easy or Uncertain quests before sending your dragon into greater danger.</p></li>
        <li><h3>Wait for a better opening</h3><p>If you have no gold, your dragon has one life, and only Risky quests or suspected traps remain, try investigating your reputation. Each investigation spends a turn. After two or three turns, Easy or Uncertain quests may appear. Check after each turn. Patience is part of a trainer’s craft.</p></li>
        <li><h3>Pray, then trust your dragon</h3><p>If no better path opens, say a prayer and send your dragon on a Risky quest. May your training hold and your companion return in triumph.</p></li>
      </ol>
      <p class="dragon-code-oath">Care for your dragon. Make every turn count.<br />Build a legend together.</p>
      <p class="dragon-code-footnote">Even the best trainer cannot promise victory. The odds are estimates, and easy quests can still fail.</p>
    </div>
  </GameDialog>
</template>
