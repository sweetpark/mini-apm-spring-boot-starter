package io.github.sweetpark.apm.support.netty;

import io.github.sweetpark.apm.core.context.LogContext;

/** Netty TCP 연결 및 데이터 처리에 대한 로깅 컨텍스트입니다. */
public class LogNettyContext extends LogContext {

  private final String clientIp;
  private final String method;
  private final String status;
  private final String requestData;
  private final String responseData;
  private final int sqlCount;
  private final long sqlTotalElapsed;

  private LogNettyContext(Builder builder) {
    super(builder);
    this.clientIp = builder.clientIp;
    this.method = builder.method;
    this.status = builder.status;
    this.requestData = builder.requestData;
    this.responseData = builder.responseData;
    this.sqlCount = builder.sqlCount;
    this.sqlTotalElapsed = builder.sqlTotalElapsed;
  }

  public String getClientIp() {
    return clientIp;
  }

  public String getMethod() {
    return method;
  }

  public String getStatus() {
    return status;
  }

  public String getRequestData() {
    return requestData;
  }

  public String getResponseData() {
    return responseData;
  }

  public int getSqlCount() {
    return sqlCount;
  }

  public long getSqlTotalElapsed() {
    return sqlTotalElapsed;
  }

  public static class Builder extends LogContext.Builder<Builder> {
    private String clientIp;
    private String method;
    private String status;
    private String requestData;
    private String responseData;
    private int sqlCount;
    private long sqlTotalElapsed;

    @Override
    protected Builder self() {
      return this;
    }

    public Builder clientIp(String clientIp) {
      this.clientIp = clientIp;
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

    public Builder requestData(String requestData) {
      this.requestData = requestData;
      return this;
    }

    public Builder responseData(String responseData) {
      this.responseData = responseData;
      return this;
    }

    public Builder sqlCount(int sqlCount) {
      this.sqlCount = sqlCount;
      return this;
    }

    public Builder sqlTotalElapsed(long sqlTotalElapsed) {
      this.sqlTotalElapsed = sqlTotalElapsed;
      return this;
    }

    public LogNettyContext build() {
      return new LogNettyContext(this);
    }
  }
}
