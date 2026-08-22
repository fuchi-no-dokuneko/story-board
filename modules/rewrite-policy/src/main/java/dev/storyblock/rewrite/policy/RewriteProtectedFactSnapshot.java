package dev.storyblock.rewrite.policy;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.CanonicalValues;
import dev.storyblock.domain.Ids;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record RewriteProtectedFactSnapshot(
        Ids.BlockId blockId,
        List<RewriteProtectedFact> facts,
        List<String> manualRiskReasons
) {
    public RewriteProtectedFactSnapshot {
        Objects.requireNonNull(blockId, "blockId");
        facts = List.copyOf(facts);
        manualRiskReasons = List.copyOf(manualRiskReasons);
        if (new HashSet<>(facts.stream().map(fact ->
                fact.kind().canonicalName() + ":" + fact.valueHash()
        ).toList()).size() != facts.size()
                || !facts.stream().sorted(java.util.Comparator
                        .comparing((RewriteProtectedFact fact) -> fact.kind().ordinal())
                        .thenComparing(RewriteProtectedFact::valueHash))
                        .toList().equals(facts)
                || new HashSet<>(manualRiskReasons).size()
                != manualRiskReasons.size()
                || !manualRiskReasons.stream().sorted().toList().equals(
                        manualRiskReasons
                )) {
            throw new IllegalArgumentException(
                    "Protected rewrite fact snapshot is not canonical"
            );
        }
    }

    public String snapshotHash() {
        return CanonicalJson.hash(canonicalValue());
    }

    public Map<String, Object> canonicalValue() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("block_id", blockId.value());
        value.put("contract_version", RewritePolicyModule.FACT_CONTRACT_VERSION);
        value.put("facts", facts.stream()
                .map(RewriteProtectedFact::canonicalValue).toList());
        value.put("manual_risk_reasons", manualRiskReasons);
        return CanonicalValues.freezeMap(value, "rewrite_protected_fact_snapshot");
    }
}
