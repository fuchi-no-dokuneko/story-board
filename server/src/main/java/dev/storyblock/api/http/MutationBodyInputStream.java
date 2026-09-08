package dev.storyblock.api.http;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import static dev.storyblock.api.http.MutationPreconditionFilter.*;

final class MutationBodyInputStream extends ServletInputStream {
  private final ByteArrayInputStream input;

  MutationBodyInputStream(byte[] body) {
    this.input = new ByteArrayInputStream(body);
  }

  @Override
  public int read() {
    return input.read();
  }

  @Override
  public int read(byte[] bytes, int offset, int length) {
    return input.read(bytes, offset, length);
  }

  @Override
  public boolean isFinished() {
    return input.available() == 0;
  }

  @Override
  public boolean isReady() {
    return true;
  }

  @Override
  public void setReadListener(ReadListener listener) {
    java.util.Objects.requireNonNull(listener, "listener");
    try {
      if (!isFinished()) {
        listener.onDataAvailable();
      }
      if (isFinished()) {
        listener.onAllDataRead();
      }
    } catch (IOException failure) {
      listener.onError(failure);
    }
  }
}
