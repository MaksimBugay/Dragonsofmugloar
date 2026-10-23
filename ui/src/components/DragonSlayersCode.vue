<script setup>
import { ref } from 'vue';
import GameDialog from './GameDialog.vue';
import GameIcon from './GameIcon.vue';
import { SHOP_ITEM_FEATURES } from '../domain/java-models.js';

const popup = ref(null);
const healingCost = -SHOP_ITEM_FEATURES.hpot.goldDiff;
</script>

<template>
  <button class="button dragon-code-button" title="Dragon Slayer’s Code" aria-haspopup="dialog"
    aria-controls="dragon-slayers-code" :aria-expanded="popup?.open ?? false" @click="popup.show()">
    <GameIcon name="book" /><span class="dragon-code-label">Dragon Slayer’s Code</span>
  </button>
  <GameDialog id="dragon-slayers-code" ref="popup" class="dragon-code-dialog" title="Dragon Slayer’s Code" icon="book">
    <p class="dragon-code-intro">Choose wisely. Guard your lives. Leave a legend.</p>
    <ol class="dragon-code-rules">
      <li><h3>Read the risk</h3><p>Green bounties are Easy, blue Uncertain, and red Risky. The probability is an estimate, so even an easy quest can fail.</p></li>
      <li><h3>Beware the glitter</h3><p>Orange notices about stealing a super awesome diamond are flagged as traps. Expired bounties cannot be attempted. Use the filters to find the quests you want.</p></li>
      <li><h3>Trust the order</h3><p>The board lists Easy, Uncertain, Risky, then Expired bounties, with traps last. Within each group, it weighs the reward against the risk and the cost of healing.</p></li>
      <li><h3>Guard your lives and your gold</h3><p>A failed quest can cost a life. A healing potion costs {{ healingCost }} gold and restores one life; upgrades raise your dragon’s level. Quest attempts and purchases can advance the turn, including unsuccessful actions.</p></li>
      <li><h3>Know your reputation</h3><p>Thumb badges show reputation hints: helping is positive, stealing negative, and other wording neutral. The Reputation banner shows People/State. Open it for all three factions; investigating uses one turn. Refreshing the bounty board or shop is free.</p></li>
      <li><h3>Keep your chronicle</h3><p>Adventure Journal records quest attempts, purchases, and reputation investigations, including failures and changes to your dragon. Your adventure ends when no lives remain. Reloading or choosing New adventure clears the game and journal.</p></li>
    </ol>
  </GameDialog>
</template>
