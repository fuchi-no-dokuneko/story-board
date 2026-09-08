package dev.storyblock.api.http;

import dev.storyblock.contracts.CanonicalPackageException;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

final class CanonicalRequestObject {
  @SuppressWarnings("unchecked")
  static Map<String, Object> object(Object value, String path) {
    if (!(value instanceof Map<?, ?> map)) {
      throw new CanonicalPackageException(path + " must be an object");
    }
    for (Object key : map.keySet()) {
      if (!(key instanceof String)) {
        throw new CanonicalPackageException(path + " contains a non-string key");
      }
    }
    return (Map<String, Object>) map;
  }
}
