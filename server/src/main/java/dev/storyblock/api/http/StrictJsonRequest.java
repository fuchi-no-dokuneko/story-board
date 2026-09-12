package dev.storyblock.api.http;

import dev.storyblock.contracts.CanonicalJson;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class StrictJsonRequest {
  StrictJsonRequest() {
  }

  static Map<String, Object> parseObject(byte[] value, String path) {
    try {
      @SuppressWarnings("unchecked")
      Map<String, Object> parsed = CanonicalJson.mapper().readValue(value, Map.class);
      return object(parsed, path);
    } catch (RuntimeException failure) {
      throw new IllegalArgumentException(path + " is malformed", failure);
    }
  }

  @SuppressWarnings("unchecked")
  static Map<String, Object> object(Object value, String path) {
    if (!(value instanceof Map<?, ?> map)) {
      throw new IllegalArgumentException(path + " must be an object");
    }
    for (Object key : map.keySet()) {
      if (!(key instanceof String)) {
        throw new IllegalArgumentException(path + " contains a non-string key");
      }
    }
    return (Map<String, Object>) map;
  }

  static String string(Map<String, Object> value, String field, String path) {
    Object entry = value.get(field);
    if (!(entry instanceof String text)) {
      throw new IllegalArgumentException(path + "." + field + " must be a string");
    }
    return text;
  }

  static Instant instant(Map<String, Object> value, String field, String path) {
    return StrictJsonRequestInstantFactory.instant(value, field, path);
  }

  static int integer(Map<String, Object> value, String field, String path) {
    return StrictJsonRequestIntegerFactory.integer(value, field, path);
  }

  static List<Map<String, Object>> objects(Object value, String path) {
    if (!(value instanceof List<?> values)) {
      throw new IllegalArgumentException(path + " must be an array");
    }
    List<Map<String, Object>> result = new ArrayList<>();
    for (int index = 0; index < values.size(); index++) {
      result.add(object(values.get(index), path + "[" + index + "]"));
    }
    return List.copyOf(result);
  }

  static List<String> uniqueStrings(
      Map<String, Object> value,
      String field,
      String path
  ) {
    return StrictJsonRequestUniqueStringsFactory.uniqueStrings(value, field, path);
  }

  static void requireKeys(
      Map<String, Object> value,
      Set<String> expected,
      String path
  ) {
    StrictJsonRequestRequireKeysFactory.requireKeys(value, expected, path);
  }

  static String unquoteEtag(String value) {
    if (value == null || value.length() < 2
        || value.charAt(0) != '"' || value.charAt(value.length() - 1) != '"') {
      throw new IllegalArgumentException("If-Match is not a quoted strong ETag");
    }
    return value.substring(1, value.length() - 1);
  }
}
