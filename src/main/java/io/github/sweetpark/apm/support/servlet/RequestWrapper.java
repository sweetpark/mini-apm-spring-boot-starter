package io.github.sweetpark.apm.support.servlet;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/** HTTP 요청의 본문(Body)을 여러 번 읽을 수 있도록 메모리에 캐싱하는 요청 래퍼입니다. */
public class RequestWrapper extends HttpServletRequestWrapper {

  private final byte[] body;
  private final String encoding;

  public RequestWrapper(HttpServletRequest request) throws IOException {
    super(request);
    String characterEncoding = request.getCharacterEncoding();
    this.encoding =
        (characterEncoding != null && !characterEncoding.isBlank())
            ? characterEncoding
            : StandardCharsets.UTF_8.name();
    ServletInputStream inputStream = request.getInputStream();
    this.body = inputStream != null ? inputStream.readAllBytes() : new byte[0];
  }

  @Override
  public ServletInputStream getInputStream() {
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(this.body);
    return new ServletInputStream() {
      @Override
      public boolean isFinished() {
        return byteArrayInputStream.available() == 0;
      }

      @Override
      public boolean isReady() {
        return true;
      }

      @Override
      public void setReadListener(ReadListener readListener) {}

      @Override
      public int read() {
        return byteArrayInputStream.read();
      }
    };
  }

  @Override
  public BufferedReader getReader() {
    return new BufferedReader(
        new InputStreamReader(this.getInputStream(), Charset.forName(this.encoding)));
  }

  public String getBody() {
    return new String(this.body, Charset.forName(this.encoding));
  }
}
