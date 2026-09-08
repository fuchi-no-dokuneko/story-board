package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import dev.storyblock.monitor.*;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class MonitorAffectedWindow {
  static List<MonitorBlockFingerprint> validate(MonitorPacket packet, List<Ids.BlockId> affectedBlockIds, Ids.BlockId targetBlockId, MonitorOutput output) {
    List<Ids.BlockId> requestedAffected = List.copyOf(affectedBlockIds);
    Set<Ids.BlockId> affected = new LinkedHashSet<>(requestedAffected);
    if (affected.isEmpty() || affected.size() != requestedAffected.size()
        || affected.size() > 5 || !affected.contains(targetBlockId)) {
      throw new IllegalArgumentException(
          "Monitor affected block IDs must be unique, include the target, and number 1 to 5"
      );
    }

    Map<Ids.BlockId, MonitorBlockFingerprint> windowFingerprints = new LinkedHashMap<>();
    packet.localInvariants().windowBlocks().forEach(fingerprint ->
        windowFingerprints.put(fingerprint.blockId(), fingerprint)
    );
    if (!windowFingerprints.keySet().containsAll(affected)) {
      throw new IllegalArgumentException(
          "Monitor affected block IDs must remain inside the supplied packet window"
      );
    }
    MonitorServiceValidateEvidence.validateEvidence(packet, affected, output);
    if (output instanceof MonitorProposedOperation proposal) {
      MonitorServiceValidateProposal.validateProposal(packet, affected, proposal.operation());
    }

    List<MonitorBlockFingerprint> affectedFingerprints = windowFingerprints.values()
        .stream()
        .filter(fingerprint -> affected.contains(fingerprint.blockId()))
        .toList();
    return affectedFingerprints;
  }
}
