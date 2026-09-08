package dev.storyblock.api.http;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import static dev.storyblock.api.http.MutationPreconditionFilter.*;

final class BufferedMutationRequest extends HttpServletRequestWrapper {
  private final byte[] body;

  BufferedMutationRequest(HttpServletRequest request, byte[] body) {
    super(request);
    this.body = body.clone();
  }

  @Override
  public int getContentLength() {
    return body.length;
  }

  @Override
  public long getContentLengthLong() {
    return body.length;
  }

  @Override
  public ServletInputStream getInputStream() {
    return new MutationBodyInputStream(body);
  }

  @Override
  public BufferedReader getReader() {
    String encoding = getCharacterEncoding();
    Charset charset = encoding == null
        ? StandardCharsets.UTF_8
        : Charset.forName(encoding);
    return new BufferedReader(new InputStreamReader(getInputStream(), charset));
  }
}
