package io.github.sweetpark.apm.support.servlet;

import io.github.sweetpark.apm.core.context.LogContext;

/** HTTP 서블릿 요청/응답에 대한 메트릭 및 상세 정보를 담는 컨텍스트입니다. */
public class LogApiContext extends LogContext {

  private final String uri;
  private final String method;
  private final String status;
  private final String requestParam;
  private final String requestBody;
  private final String responseBody;

  private LogApiContext(Builder builder) {
    super(builder);
    this.uri = builder.uri;
    this.method = builder.method;
    this.status = builder.status;
    this.requestParam = builder.requestParam;
    this.requestBody = builder.requestBody;
    this.responseBody = builder.responseBody;
  }

  public String getUri() {
    return uri;
  }

  public String getMethod() {
    return method;
  }

  public String getStatus() {
    return status;
  }

  public String getRequestParam() {
    return requestParam;
  }

  public String getRequestBody() {
    return requestBody;
  }

  public String getResponseBody() {
    return responseBody;
  }

  public static class Builder extends LogContext.Builder<Builder> {
    private String uri;
    private String method;
    private String status;
    private String requestParam;
    private String requestBody;
    private String responseBody;

    @Override
    protected Builder self() {
      return this;
    }

    public Builder uri(String uri) {
      this.uri = uri;
      return this;
    }

    public Builder method(String method) {
      this.method = method;
      return this;
    }

    public Builder status(String status) {
      this.status = status;
      return this;
    }

    public Builder requestParam(String requestParam) {
      this.requestParam = requestParam;
      return this;
    }

    public Builder requestBody(String requestBody) {
      this.requestBody = requestBody;
      return this;
    }

    public Builder responseBody(String responseBody) {
      this.responseBody = responseBody;
      return this;
    }

    public LogApiContext build() {
      return new LogApiContext(this);
    }
  }
}
