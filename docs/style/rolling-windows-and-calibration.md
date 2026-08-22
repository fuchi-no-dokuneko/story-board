# Rolling Windows And Style Calibration

ADR-305 implements deterministic stratification, rolling windows, calibration,
and anomaly policy. ADR-306 persists its summaries and compact decisions through
the durable job, pagination, and trace-artifact boundary documented in
[`durable-analysis-jobs.md`](durable-analysis-jobs.md).

## Stratification

Each analysis block is normalized into narration or dialogue, with optional
speaker identity, POV, narrative mode, scene identity, and an intentional style
shift reason. A new segment starts when any of these change:

- narration versus dialogue;
- POV;
- narrative mode;
- entry into or exit from a scene marked with
  `intentional_style_shift_reason`.

Speaker changes do not split a dialogue conversation. A dialogue window uses a
speaker-specific stratum only when every block identifies the same speaker;
otherwise it uses the global dialogue stratum. At scoring time, a calibrated
speaker profile is preferred. If that speaker has fewer than 30 calibration
windows, selection falls back to global dialogue. Missing or under-sampled
fallback data is `LOW_CONFIDENCE`.

## Window Capabilities

The planner keeps whole block boundaries and produces three immutable window
kinds from the versioned configuration:

| Kind | Default size and stride | Capability |
|---|---|---|
| Operational | 3,000 / 1,500 graphemes | Primary decision input when full-sized |
| Micro | 750 / 375 graphemes | Localization only |
| Non-overlap | 3,000 / 3,000 graphemes | Sustainment evidence only |

POV, mode, and intentional-shift boundaries are never crossed. A short segment
still gets a diagnostic partial window, but partial windows are not decision or
sustainment evidence. Capability is derived from window kind and completeness;
there is no mutable flag through which a caller can promote a micro window.

## Calibration

Only full operational target-corpus windows enter calibration. Inputs are sorted
by content-derived window ID, so caller ordering does not affect the output.
For each stratum and each window:

1. Exclude that window.
2. Average the remaining feature vectors under the same feature contract.
3. Reapply Top-K retention and merge the remainder into `OTHER`.
4. Compare the excluded window to that leave-one-out baseline.
5. Retain the sorted primary-distance references per channel.

The persisted channel statistics are median, median absolute deviation, nearest
rank q95, nearest-rank q99, and the sorted reference distances used for an
empirical percentile. Deserialization recomputes all four summaries and rejects
any mismatch. Calibration is bound to the target corpus hash, feature contract
hash, and window configuration hash.

Thirty windows are required for a calibrated stratum. Smaller strata remain
available for diagnostics but report `LOW_CONFIDENCE`. Robust z scores use
`abs(distance - median) / (1.4826 * MAD)`; a nonzero deviation with zero MAD is
represented by a bounded sentinel rather than an infinite value.

## Decision Policy

The operational window controls the decision. Micro windows can only identify
locations, and non-overlap windows can only establish sustainment.

| State | Rule | Rewrite allowed |
|---|---|---|
| `normal` | Fewer than two independent channels exceed q95 | No |
| `warning` | At least two independent channels exceed q95 | No |
| `rewrite_candidate` | At least two channels exceed q99 and two full non-overlap windows repeat that condition | Yes |
| `topic_shift_only` | Surface/token channel is the sole q95 breach | No |
| `low_confidence` | Selected calibration is absent or has fewer than 30 windows | No |

Optional embeddings are secondary evidence and do not count as an independent
gate channel. Directional KL remains diagnostic-only. An intentional style
shift lowers a candidate by one severity level, so it cannot directly trigger a
rewrite. Every channel also reports deterministic Top-10 feature contributors.

`StyleAnomalyDecision.can_trigger_rewrite` is true only for
`rewrite_candidate`; constructors reject inconsistent low-confidence,
topic-only, or unsustained rewrite decisions.
The compact decision also persists the sorted identities of independently gated
channels above q99, so a later rewrite gate can verify multi-channel evidence
without loading or duplicating dense score traces.

## Verification

```bash
./mvnw -o -pl modules/style -am test
```

Tests cover POV and intentional-shift segmentation, all three window kinds,
speaker-specific and global dialogue selection, order-independent leave-one-out
calibration, canonical round trips, empirical thresholds, sustained q99
candidacy, and the low-confidence, topic-only, micro-only, and intentional-shift
rewrite blocks.
