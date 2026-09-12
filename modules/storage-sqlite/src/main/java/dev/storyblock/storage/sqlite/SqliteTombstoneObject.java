package dev.storyblock.storage.sqlite;
import dev.storyblock.storage.StorageException;
import java.util.Map;
final class SqliteTombstoneObject {
  @SuppressWarnings("unchecked")
  static Map<String, Object> requiredMap(Map<String, Object> value, String field) {
    Object entry = value.get(field);
    if (!(entry instanceof Map<?, ?> map)) {
      throw new StorageException("Stored tombstone field " + field + " is not an object");
    }
    return (Map<String, Object>) map;
  }
}
