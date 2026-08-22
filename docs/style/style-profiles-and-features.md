# Immutable Style Profiles And Feature Channels

ADR-304 implements the governed baseline and deterministic feature boundary for
style analysis. ADR-305 adds the rolling-window, calibration, and anomaly policy
documented in
[`rolling-windows-and-calibration.md`](rolling-windows-and-calibration.md).

## Governance Model

A `StyleProfile` has an immutable ID, name, provenance statement, and scope. A
scope is always bound to one novel and may narrow analysis to the novel, a
series subject, or a character subject. API handlers authorize against the
stored novel before returning or mutating a profile.

Each `StyleProfileVersion` is immutable and stores:

- a monotonically allocated version number;
- the exact profile scope;
- corpus IDs, content hashes, source kind, provenance, license, and ownership;
- the analyzer, feature schema, tokenizer, vocabulary, and normalizer contract;
- versioned feature vectors for every required channel;
- operational, micro, stride, and non-overlap window configuration;
- a reserved canonical calibration-statistics object;
- the authenticated creator and creation time.

At least one corpus source hash must equal the feature set's source hash. This
prevents a feature payload from being detached from all declared provenance.

## Lifecycle And Approval

Every new version begins in `DRAFT`. Lifecycle changes are append-only events
and must follow exactly:

```text
DRAFT -> CALIBRATING -> READY -> DEPRECATED
```

The client cannot supply `created_by` or `approved_by`. Both identities and
timestamps come from the authenticated audit context. A version can gate a
rewrite only while its current state is `READY` and its READY event supplies the
approval actor and time. Rewrite gating additionally requires at least one
stratum with 30 calibration windows; READY versions with only low-confidence
calibration remain reproducible but cannot gate a rewrite.

Promoting a new version to `READY` deprecates the prior READY version in the
same SQLite transaction. Generated or mixed corpus additionally requires
`confirm_generated_corpus_promotion=true` on the READY transition. A failed or
unconfirmed promotion does not append an event. Deprecated versions remain
readable for reproduction but cannot gate new rewrites.

Profiles, versions, lifecycle events, and idempotency records have SQLite
update/delete rejection triggers. Mutations use per-novel idempotency keys and
strong profile/status ETags. A stale ETag returns `412`; an illegal lifecycle
step or missing generated-corpus acknowledgement returns `409`.

## Feature Contract

The deterministic analyzer accepts 1 to 1,000 immutable narrative blocks and
emits these versioned channels:

| Channel | Features | Primary distance |
|---|---|---|
| Surface | Masked character 2-4 grams and tokens | Jensen-Shannon distance |
| Grammar | Function words and deterministic token-shape bigrams | Jensen-Shannon distance |
| Rhythm | Sentence/clause/paragraph lengths and punctuation | Wasserstein distance, JSD diagnostic |
| Narrative | Dialogue/action/description, POV, mode, speaker turns | Robust-calibration input distance |
| Lexical | Diversity, repeated trigrams, token lengths | Robust-calibration input distance |
| Optional embedding | Content-reduced numeric vector | Cosine distance, secondary evidence only |

Names, places, and numbers are masked in the surface channel. Top-K retention,
`OTHER`, additive smoothing, vocabulary hash, and normalizer identity are part
of the versioned contract. Feature sets with different contract hashes cannot
be compared.

Jensen-Shannon distance is the primary distribution metric. Both directional
smoothed KL values are emitted only as surface diagnostics. No KL metric can be
constructed as a primary gate in `StyleChannelDistance`, and optional embedding
evidence is never independent gate evidence.

## HTTP Resources

```text
POST /v1/style-profiles
GET  /v1/style-profiles/{profileId}
POST /v1/style-profiles/{profileId}/versions
GET  /v1/style-profiles/{profileId}/versions/{versionId}
POST /v1/style-profiles/{profileId}/versions/{versionId}/transitions
```

Profile and version creation return `201`, or `200` for an idempotent replay.
All responses are `no-store`; resource responses carry a strong ETag. Profile
mutations require `style:admin`, while reads require `novel:read` and always
apply the profile's stored novel boundary.

## Verification

```bash
./mvnw -o -pl modules/style,modules/storage-sqlite,apps/api -am test
```

Tests cover deterministic extraction, every required channel and distance,
diagnostic-only KL, generated-corpus confirmation, lifecycle ordering, stale
ETags, idempotency conflicts, single READY promotion, rewrite-gate rejection,
SQLite immutability, real bearer authorization, and cross-novel denial.
