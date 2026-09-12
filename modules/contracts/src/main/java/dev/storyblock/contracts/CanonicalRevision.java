package dev.storyblock.contracts;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public final class CanonicalRevision {
  public static final String SCHEMA_VERSION = "1.0.0";

  private final Map<String, Object> canonicalContent;
  private final Map<String, Object> derivedData;
  private final byte[] canonicalBytes;
  private final String contentHash;

  CanonicalRevision(
      Map<String, ?> canonicalContent,
      Map<String, ?> derivedData
  ) {
    this.canonicalContent = CanonicalRevisionFreezeMap.freezeMap(canonicalContent, "document");
    this.derivedData = CanonicalRevisionFreezeMap.freezeMap(derivedData, "derived");
    CanonicalRevisionValidateDocument.validateDocument(this.canonicalContent);
    this.canonicalBytes = CanonicalJson.bytes(this.canonicalContent);
    this.contentHash = CanonicalJson.hashBytes(this.canonicalBytes);
  }

  public static CanonicalRevision of(Map<String, ?> canonicalContent) {
    return new CanonicalRevision(canonicalContent, Map.of());
  }

  public static CanonicalRevision of(
      Map<String, ?> canonicalContent,
      Map<String, ?> derivedData
  ) {
    return new CanonicalRevision(canonicalContent, derivedData);
  }

  public static CanonicalRevision parseEnvelope(byte[] json) {
    return CanonicalRevisionParseEnvelopeFactory.parseEnvelope(json);
  }

  public Map<String, Object> canonicalContent() {
    return canonicalContent;
  }

  public Map<String, Object> derivedData() {
    return derivedData;
  }

  public byte[] canonicalBytes() {
    return canonicalBytes.clone();
  }

  public String contentHash() {
    return contentHash;
  }

  public byte[] envelopeBytes() {
    return CanonicalJson.bytes(envelope());
  }

  public Map<String, Object> envelope() {
    Map<String, Object> envelope = new TreeMap<>(canonicalContent);
    envelope.put("content_hash", contentHash);
    return Collections.unmodifiableMap(envelope);
  }

  public byte[] diagnosticExportBytes() {
    Map<String, Object> export = new TreeMap<>(canonicalContent);
    export.put("content_hash", contentHash);
    if (!derivedData.isEmpty()) {
      export.put("derived", derivedData);
    }
    return CanonicalJson.bytes(export);
  }

  @SuppressWarnings("unchecked")
  static Map<String, Object> requireMap(Object value, String path) {
    if (!(value instanceof Map<?, ?> map)) {
      throw new IllegalArgumentException(path + " must be an object");
    }
    return (Map<String, Object>) map;
  }

}
