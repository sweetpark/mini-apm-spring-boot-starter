package io.github.sweetpark.apm.support.servlet;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/** HTTP 응답의 본문(Body)을 로깅하기 위해 응답 출력을 캡처하는 래퍼 클래스입니다. */
public class ResponseWrapper extends HttpServletResponseWrapper {

  private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
  private ServletOutputStream servletOutputStream;
  private PrintWriter printWriter;

  public ResponseWrapper(HttpServletResponse response) {
    super(response);
  }

  @Override
  public ServletOutputStream getOutputStream() throws IOException {
    if (printWriter != null) {
      throw new IllegalStateException("getWriter() has already been called on this response.");
    }
    if (servletOutputStream == null) {
      servletOutputStream =
          new ServletOutputStream() {
            @Override
            public boolean isReady() {
              return true;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {}

            @Override
            public void write(int b) {
              outputStream.write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) {
              outputStream.write(b, off, len);
            }
          };
    }
    return servletOutputStream;
  }

  @Override
  public PrintWriter getWriter() throws IOException {
    if (servletOutputStream != null) {
      throw new IllegalStateException(
          "getOutputStream() has already been called on this response.");
    }
    if (printWriter == null) {
      String encoding = getCharacterEncoding();
      Charset charset =
          (encoding != null && !encoding.isBlank())
              ? Charset.forName(encoding)
              : StandardCharsets.UTF_8;
      printWriter = new PrintWriter(new OutputStreamWriter(outputStream, charset), true);
    }
    return printWriter;
  }

  @Override
  public void flushBuffer() throws IOException {
    if (printWriter != null) {
      printWriter.flush();
    } else if (servletOutputStream != null) {
      servletOutputStream.flush();
    }
  }

  public byte[] getContentAsByteArray() {
    return outputStream.toByteArray();
  }

  public String getBodyAsString() {
    String encoding = getCharacterEncoding();
    Charset charset =
        (encoding != null && !encoding.isBlank())
            ? Charset.forName(encoding)
            : StandardCharsets.UTF_8;
    return new String(outputStream.toByteArray(), charset);
  }

  public void copyBodyToResponse() throws IOException {
    if (outputStream.size() > 0) {
      HttpServletResponse originalResponse = (HttpServletResponse) getResponse();
      if (!originalResponse.isCommitted()) {
        byte[] bodyBytes = outputStream.toByteArray();
        originalResponse.setContentLength(bodyBytes.length);
        originalResponse.getOutputStream().write(bodyBytes);
        originalResponse.getOutputStream().flush();
      }
    }
  }
}
