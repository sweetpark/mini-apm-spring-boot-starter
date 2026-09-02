package io.github.sweetpark.apm.support.servlet;

import io.github.sweetpark.apm.core.config.ApmProperties;
import io.github.sweetpark.apm.core.enums.LogMarker;
import io.github.sweetpark.apm.core.enums.TraceLevel;
import io.github.sweetpark.apm.core.error.ErrorEvaluator;
import io.github.sweetpark.apm.core.process.AbstractLogProcessor;
import io.github.sweetpark.apm.core.support.util.CommonUtil;
import io.github.sweetpark.apm.core.support.util.SensitiveDataMasker;

/** 서블릿 환경의 API 요청/응답 및 SQL 상세 정보를 로깅하는 프로세서입니다. */
public class ServletLogProcessor extends AbstractLogProcessor<LogApiContext> {

  private final ErrorEvaluator errorEvaluator;

  public ServletLogProcessor(ApmProperties properties, ErrorEvaluator errorEvaluator) {
    super(properties);
    this.errorEvaluator = errorEvaluator;
  }

  @Override
  public void logApi(LogApiContext ctx) {
    String traceId = ctx.getTraceId();
    String spanId = ctx.getSpanId();
    TraceLevel level = resolveLevel();
    ApmProperties.CaptureMode bodyMode = properties.getCapture().getBody();

    int statusCode = 200;
    try {
      statusCode = Integer.parseInt(ctx.getStatus());
    } catch (Exception ignored) {
    }

    boolean isError =
        (errorEvaluator != null)
            ? errorEvaluator.isError(statusCode, ctx.getResponseBody(), ctx.getEx())
            : ctx.getEx() != null || statusCode >= 400;

    boolean isSlow = ctx.getElapsedMs() >= properties.getSlow().getApiMs();

    // 1. 기본 요약 로그 (항상 출력)
    logger.info(
        LogMarker.HTTP.marker(),
        "trace_id={} span_id={} interface_id={} uri={} method={} status={} elapsed={}ms",
        traceId != null ? traceId : "-",
        spanId != null ? spanId : "-",
        ctx.getInterfaceId() != null ? ctx.getInterfaceId() : "-",
        ctx.getUri() != null ? ctx.getUri() : "-",
        ctx.getMethod() != null ? ctx.getMethod() : "-",
        ctx.getStatus() != null ? ctx.getStatus() : "-",
        (long) ctx.getElapsedMs());

    // 2. 바디 로깅 여부 결정
    boolean shouldLogBody = false;
    if (bodyMode == ApmProperties.CaptureMode.ALWAYS) {
      shouldLogBody = true;
    } else if (bodyMode == ApmProperties.CaptureMode.ERROR && isError) {
      shouldLogBody = true;
    } else if (bodyMode == ApmProperties.CaptureMode.SLOW && isSlow) {
      shouldLogBody = true;
    } else if (bodyMode == ApmProperties.CaptureMode.SAMPLE) {
      shouldLogBody = level == TraceLevel.TRACE;
    } else if (level == TraceLevel.TRACE) {
      shouldLogBody = true;
    }

    if (shouldLogBody || isError) {
      int maxBodyLen = properties.getLimit().getMaxBodyLength();
      boolean maskBody =
          properties.getSecurity().isMaskingEnabled() && properties.getSecurity().isMaskBody();

      String reqBody =
          SensitiveDataMasker.maskIfEnabled(
              CommonUtil.truncate(ctx.getRequestBody(), maxBodyLen), maskBody);
      String resBody =
          SensitiveDataMasker.maskIfEnabled(
              CommonUtil.truncate(ctx.getResponseBody(), maxBodyLen), maskBody);
      String reqParam = SensitiveDataMasker.maskIfEnabled(ctx.getRequestParam(), maskBody);

      LogMarker marker = isError ? LogMarker.EXCEPTION : LogMarker.HTTP_DETAIL;

      logger.info(
          marker.marker(),
          "trace_id={} span_id={} params=\"{}\" request=\"{}\" response=\"{}\"",
          traceId != null ? traceId : "-",
          spanId != null ? spanId : "-",
          escape(reqParam),
          escape(reqBody),
          escape(resBody));
    }

    logSqlDetails(traceId, spanId, level, isError);
    logException(ctx, traceId, spanId);
  }

  private String escape(String s) {
    if (s == null) return "";
    return s.replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
  }
}
