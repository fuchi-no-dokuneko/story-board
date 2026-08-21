# ADR 0005: Versioned Text Boundaries

- Status: Accepted
- Date: 2026-08-21
- Owner: StoryBlock local implementation

## Decision

Text validation uses an NFC comparison view and Unicode extended grapheme
clusters. The version 1 sentence parser recognizes Chinese and ASCII terminal
punctuation, paired closing quotation marks, ellipses, and explicit author split
anchors. It never splits a combining sequence or emoji grapheme.

The canonical text preserves the author's original code points. Parser and
normalization versions accompany validation results so later rule improvements
do not silently change historical acceptance.

## Consequences

Ambiguous punctuation is rejected with safe candidate anchors instead of being
silently guessed. Parser changes require new golden fixtures and a version bump.
