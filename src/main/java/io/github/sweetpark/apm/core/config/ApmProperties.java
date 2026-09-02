package io.github.sweetpark.apm.core.config;

import io.github.sweetpark.apm.core.enums.TraceLevel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * APM 및 로깅 관련 설정 속성 클래스입니다.
 *
 * <p>"apm" 접두어로 시작하는 설정값들을 매핑합니다. (예: apm.trace.level, apm.slow.query.ms 등)
 */
@ConfigurationProperties(prefix = "apm")
public class ApmProperties {

  private boolean enabled = true;
  private Trace trace = new Trace();
  private Slow slow = new Slow();
  private Limit limit = new Limit();
  private Capture capture = new Capture();
  private Security security = new Security();
  private Error error = new Error();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public Trace getTrace() {
    return trace;
  }

  public void setTrace(Trace trace) {
    this.trace = trace;
  }

  public Slow getSlow() {
    return slow;
  }

  public void setSlow(Slow slow) {
    this.slow = slow;
  }

  public Limit getLimit() {
    return limit;
  }

  public void setLimit(Limit limit) {
    this.limit = limit;
  }

  public Capture getCapture() {
    return capture;
  }

  public void setCapture(Capture capture) {
    this.capture = capture;
  }

  public Security getSecurity() {
    return security;
  }

  public void setSecurity(Security security) {
    this.security = security;
  }

  public Error getError() {
    return error;
  }

  public void setError(Error error) {
    this.error = error;
  }

  /** 추적(Trace) 관련 설정입니다. */
  public static class Trace {
    private TraceLevel level = TraceLevel.PROD;
    private String headerName = "X-Trace-Id";
    private String interfaceHeaderName = "X-Interface-Id";

    public TraceLevel getLevel() {
      return level;
    }

    public void setLevel(TraceLevel level) {
      this.level = (level != null) ? level : TraceLevel.PROD;
    }

    public String getHeaderName() {
      return headerName;
    }

    public void setHeaderName(String headerName) {
      this.headerName = headerName;
    }

    public String getInterfaceHeaderName() {
      return interfaceHeaderName;
    }

    public void setInterfaceHeaderName(String interfaceHeaderName) {
      this.interfaceHeaderName = interfaceHeaderName;
    }
  }

  /** 캡처 모드를 정의하는 열거형입니다. */
  public enum CaptureMode {
    ALWAYS,
    ERROR,
    SLOW,
    SAMPLE,
    OFF
  }

  /** 바디 및 SQL 캡처 전략 설정입니다. */
  public static class Capture {
    private CaptureMode body = CaptureMode.ERROR;
    private CaptureMode sql = CaptureMode.SLOW;
    private double sampleRate = 0.01; // 1%

    public CaptureMode getBody() {
      return body;
    }

    public void setBody(CaptureMode body) {
      this.body = body;
    }

    public CaptureMode getSql() {
      return sql;
    }

    public void setSql(CaptureMode sql) {
      this.sql = sql;
    }

    public double getSampleRate() {
      return sampleRate;
    }

    public void setSampleRate(double sampleRate) {
      this.sampleRate = sampleRate;
    }
  }

  /** 로깅 길이 및 개수 제한(OOM 방지) 설정입니다. */
  public static class Limit {
    private int maxSqlCount = 100;
    private int maxSqlDetailCount = 10;
    private int maxSqlLength = 2000;
    private int maxSqlParamLength = 1000;
    private int maxBodyLength = 2000;
    private int maxStackDepth = 5;
    private int maxStackLines = 3;
    private int n1DetectionThreshold = 3;

    public int getMaxSqlCount() {
      return maxSqlCount;
    }

    public void setMaxSqlCount(int maxSqlCount) {
      this.maxSqlCount = maxSqlCount;
    }

    public int getMaxSqlDetailCount() {
      return maxSqlDetailCount;
    }

    public void setMaxSqlDetailCount(int maxSqlDetailCount) {
      this.maxSqlDetailCount = maxSqlDetailCount;
    }

    public int getMaxSqlLength() {
      return maxSqlLength;
    }

    public void setMaxSqlLength(int maxSqlLength) {
      this.maxSqlLength = maxSqlLength;
    }

    public int getMaxSqlParamLength() {
      return maxSqlParamLength;
    }

    public void setMaxSqlParamLength(int maxSqlParamLength) {
      this.maxSqlParamLength = maxSqlParamLength;
    }

    public int getMaxBodyLength() {
      return maxBodyLength;
    }

    public void setMaxBodyLength(int maxBodyLength) {
      this.maxBodyLength = maxBodyLength;
    }

    public int getMaxStackDepth() {
      return maxStackDepth;
    }

    public void setMaxStackDepth(int maxStackDepth) {
      this.maxStackDepth = maxStackDepth;
    }

    public int getMaxStackLines() {
      return maxStackLines;
    }

    public void setMaxStackLines(int maxStackLines) {
      this.maxStackLines = maxStackLines;
    }

    public int getN1DetectionThreshold() {
      return n1DetectionThreshold;
    }

    public void setN1DetectionThreshold(int n1DetectionThreshold) {
      this.n1DetectionThreshold = n1DetectionThreshold;
    }
  }

  /** 슬로우 응답 및 쿼리 임계값 설정입니다. */
  public static class Slow {
    private int apiMs = 1000;
    private Query query = new Query();

    public int getApiMs() {
      return apiMs;
    }

    public void setApiMs(int apiMs) {
      this.apiMs = apiMs;
    }

    public Query getQuery() {
      return query;
    }

    public void setQuery(Query query) {
      this.query = query;
    }

    public static class Query {
      private int ms = 300;
      private int totalMs = 1000;

      public int getMs() {
        return ms;
      }

      public void setMs(int ms) {
        this.ms = ms;
      }

      public int getTotalMs() {
        return totalMs;
      }

      public void setTotalMs(int totalMs) {
        this.totalMs = totalMs;
      }
    }
  }

  /** 민감정보 마스킹 설정입니다. */
  public static class Security {
    private boolean maskingEnabled = true;
    private boolean maskBody = true;
    private boolean maskSqlParam = true;

    public boolean isMaskingEnabled() {
      return maskingEnabled;
    }

    public void setMaskingEnabled(boolean maskingEnabled) {
      this.maskingEnabled = maskingEnabled;
    }

    public boolean isMaskBody() {
      return maskBody;
    }

    public void setMaskBody(boolean maskBody) {
      this.maskBody = maskBody;
    }

    public boolean isMaskSqlParam() {
      return maskSqlParam;
    }

    public void setMaskSqlParam(boolean maskSqlParam) {
      this.maskSqlParam = maskSqlParam;
    }
  }

  /** 에러 판정 및 지문 생성 관련 설정입니다. */
  public static class Error {
    private Set<String> errorCodeKeys =
        new HashSet<>(Set.of("resCode", "res_cd", "code", "errorCode", "status"));
    private Set<String> errorCodes = new HashSet<>(Set.of("9999", "ERROR", "FAIL", "ERR"));
    private int httpStatusThreshold = 400;
    private List<String> appPackagePrefixes = new ArrayList<>();

    public Set<String> getErrorCodeKeys() {
      return errorCodeKeys;
    }

    public void setErrorCodeKeys(Set<String> errorCodeKeys) {
      this.errorCodeKeys = errorCodeKeys;
    }

    public Set<String> getErrorCodes() {
      return errorCodes;
    }

    public void setErrorCodes(Set<String> errorCodes) {
      this.errorCodes = errorCodes;
    }

    public int getHttpStatusThreshold() {
      return httpStatusThreshold;
    }

    public void setHttpStatusThreshold(int httpStatusThreshold) {
      this.httpStatusThreshold = httpStatusThreshold;
    }

    public List<String> getAppPackagePrefixes() {
      return appPackagePrefixes;
    }

    public void setAppPackagePrefixes(List<String> appPackagePrefixes) {
      this.appPackagePrefixes = appPackagePrefixes;
    }
  }
}
