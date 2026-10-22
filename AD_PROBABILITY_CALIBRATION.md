# Ad probability calibration

Live calibration performed on 21 October 2026 within a ten-minute investigation budget.

Sample: **160 distinct ads across 3 games**, all at level zero. Observed 77 successful solves and 1 explicitly identified trap failure.

## Method

The player paused 600 ms between API calls, selected randomly among the least-sampled available labels, attempted each ad ID once, and bought only healing potions. Older gameplay traces were excluded because their reward-seeking selection over-sampled traps.

The updated enum values include trap failures. Weighted isotonic regression fits empirical binomial counts and pools adjacent labels when sampling noise would otherwise invert their easy-to-hard order. Results are rounded to whole percentages and constrained to 1–99 to avoid asserting certainty from finite samples. The ordering constraint and these bounds are modeling choices. Unobserved labels retain their previous fallback.

## Results

| Label | Successes / attempts | Trap failures | Raw success | Approx. raw 95% interval | Previous enum | Updated enum |
|---|---:|---:|---:|---:|---:|---:|
| Sure thing | 12 / 12 | 0 | 100.0% | 75.8–100% | 100% | 99% |
| Piece of cake | 12 / 13 | 1 | 92.31% | 66.7–98.6% | 95% | 92% |
| Walk in the park | 12 / 14 | 0 | 85.71% | 60.1–96.0% | 90% | 86% |
| Quite likely | 12 / 14 | 0 | 85.71% | 60.1–96.0% | 80% | 86% |
| Hmmm.... | 7 / 14 | 0 | 50.0% | 26.8–73.2% | 60% | 50% |
| Gamble | 6 / 13 | 0 | 46.15% | 23.2–70.9% | 50% | 46% |
| Risky | 6 / 15 | 0 | 40.0% | 19.8–64.3% | 40% | 40% |
| Playing with fire | 5 / 14 | 0 | 35.71% | 16.3–61.2% | 30% | 36% |
| Rather detrimental | 3 / 14 | 0 | 21.43% | 7.6–47.6% | 20% | 21% |
| Suicide mission | 2 / 18 | 0 | 11.11% | 3.1–32.8% | 10% | 11% |
| Impossible | 0 / 19 | 0 | 0.0% | 0–16.8% | 1% | 1% |
| Unknown | 0 / 0 | 0 | Not observed | Not observed | 50% | 50% |

## Interpretation and limits

- These are estimates of success conditional on the sampled label and policy, not published API odds or guarantees.
- Trap failures count as failures. The machine-readable summary also reports success rates excluding explicitly identified traps.
- The raw rates in this dataset already followed the label order, so no adjacent labels needed pooling. Walk in the park and Quite likely both had 12 successes in 14 attempts and therefore share an estimate.
- The confidence intervals apply to the raw per-label observations, not to the fitted percentages.
- Samples are limited and share game state; availability, reputation and task mix can vary. Results at higher dragon levels were not tested.
- UNKNOWN remains a fallback, not a calibrated class.
- Small differences between neighboring labels should not be treated as statistically established.

Evidence: [raw observations](ad-probability-observations.jsonl) and [machine-readable summary](ad-probability-calibration.json).
