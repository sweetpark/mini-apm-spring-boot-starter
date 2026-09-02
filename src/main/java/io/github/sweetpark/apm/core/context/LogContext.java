package io.github.sweetpark.apm.core.context;

/** 런타임별 로깅 컨텍스트의 최상위 추상 클래스입니다. */
public abstract class LogContext {
  protected final String traceId;
  protected final String spanId;
  protected final String interfaceId;
  protected final Exception ex;
  protected final double elapsedMs;

  protected LogContext(Builder<?> builder) {
    this.traceId = builder.traceId;
    this.spanId = builder.spanId;
    this.interfaceId = builder.interfaceId;
    this.ex = builder.ex;
    this.elapsedMs = builder.elapsedMs;
  }

  public String getTraceId() {
    return traceId;
  }

  public String getSpanId() {
    return spanId;
  }

  public String getInterfaceId() {
    return interfaceId;
  }

  public Exception getEx() {
    return ex;
  }

  public double getElapsedMs() {
    return elapsedMs;
  }

  public abstract static class Builder<T extends Builder<T>> {
    protected String traceId;
    protected String spanId;
    protected String interfaceId;
    protected Exception ex;
    protected double elapsedMs;

    protected abstract T self();

    public T traceId(String traceId) {
      this.traceId = traceId;
      return self();
    }

    public T spanId(String spanId) {
      this.spanId = spanId;
      return self();
    }

    public T interfaceId(String interfaceId) {
      this.interfaceId = interfaceId;
      return self();
    }

    public T ex(Exception ex) {
      this.ex = ex;
      return self();
    }

    public T elapsedMs(double elapsedMs) {
      this.elapsedMs = elapsedMs;
      return self();
    }
  }
}
