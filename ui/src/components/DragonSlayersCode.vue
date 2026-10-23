<script setup>
import { ref } from 'vue';
import GameDialog from './GameDialog.vue';
import GameIcon from './GameIcon.vue';

const popup = ref(null);
</script>

<template>
  <button class="button dragon-code-button" title="Dragon Slayer’s Code" aria-haspopup="dialog"
    aria-controls="dragon-slayers-code" :aria-expanded="popup?.open ?? false" @click="popup.show()">
    <GameIcon name="book" /><span class="dragon-code-label">Dragon Slayer’s Code</span>
  </button>
  <GameDialog id="dragon-slayers-code" ref="popup" class="dragon-code-dialog" title="Dragon Slayer’s Code" icon="book">
    <div class="dragon-code-manuscript" tabindex="0" role="region" aria-label="The seven rules of survival">
      <p class="dragon-code-kicker">Seven rules · a path to glory</p>
      <p class="dragon-code-intro">Slayer, guard your life, claim the easier victories, and build your strength before facing greater foes.</p>
      <ol class="dragon-code-rules">
        <li><h3>Keep gold for one more life</h3><p>Always keep enough gold in reserve to buy +1 life. Whenever only one life remains, buy another if you can afford it. Live to hunt another day.</p></li>
        <li><h3>Claim the easy victories</h3><p>Solve the Easy quests, starting at the top of the board. Keep going while easy quests remain.</p></li>
        <li><h3>Grow stronger when easy quests run out</h3><p>If no Easy quests remain, buy a +1 or +2 level upgrade if you can afford it while keeping your life reserve. Then check the board for Easy quests again.</p></li>
        <li><h3>Face the uncertain trials</h3><p>Move on to Uncertain quests. After every attempt, check for Easy quests again and take those first.</p></li>
        <li><h3>Build your strength once more</h3><p>If no Uncertain quests remain either, buy a +1 or +2 level upgrade if your gold allows, keeping your life reserve. Check the board again for Easy or Uncertain quests.</p></li>
        <li><h3>Buy time when hope runs thin</h3><p>If you have no gold, only one life, and nothing but Risky quests or suspected traps left, try investigating your reputation. Each investigation spends a turn. After two or three turns, Easy or Uncertain quests may appear. Check the board after each turn.</p></li>
        <li><h3>Pray, then brave the danger</h3><p>If no better path opens, say a prayer and attempt a Risky quest. May your courage hold and your blade strike true.</p></li>
      </ol>
      <p class="dragon-code-oath">Stay alive. Make every turn count.<br />Build a legend the kingdom will remember.</p>
      <p class="dragon-code-footnote">No strategy guarantees victory. The odds are estimates, and even easy quests can fail.</p>
    </div>
  </GameDialog>
</template>
