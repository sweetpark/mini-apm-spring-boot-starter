package io.github.sweetpark.apm.support.netty;

import io.github.sweetpark.apm.core.config.ApmProperties;
import io.github.sweetpark.apm.core.enums.LogMarker;
import io.github.sweetpark.apm.core.enums.TraceLevel;
import io.github.sweetpark.apm.core.process.AbstractLogProcessor;
import io.github.sweetpark.apm.core.support.util.CommonUtil;
import io.github.sweetpark.apm.core.support.util.SensitiveDataMasker;

/** Netty TCP 런타임의 요청/응답 및 SQL 통계를 로깅하는 프로세서입니다. */
public class NettyLogProcessor extends AbstractLogProcessor<LogNettyContext> {

  public NettyLogProcessor(ApmProperties properties) {
    super(properties);
  }

  @Override
  protected void logApi(LogNettyContext ctx) {
    String traceId = ctx.getTraceId();
    String spanId = ctx.getSpanId();
    TraceLevel level = resolveLevel();
    ApmProperties.CaptureMode bodyMode = properties.getCapture().getBody();

    boolean isError = ctx.getEx() != null || "ERROR".equalsIgnoreCase(ctx.getStatus());
    boolean isSlow = ctx.getElapsedMs() >= properties.getSlow().getApiMs();

    // 1. Netty 요약 로그 (항상 출력)
    logger.info(
        LogMarker.NETTY.marker(),
        "trace_id={} span_id={} interface_id={} client_ip={} method={} status={} elapsed={}ms"
            + " sql_count={} sql_total_elapsed={}ms",
        traceId != null ? traceId : "-",
        spanId != null ? spanId : "-",
        ctx.getInterfaceId() != null ? ctx.getInterfaceId() : "-",
        ctx.getClientIp() != null ? ctx.getClientIp() : "-",
        ctx.getMethod() != null ? ctx.getMethod() : "-",
        ctx.getStatus() != null ? ctx.getStatus() : "-",
        (long) ctx.getElapsedMs(),
        ctx.getSqlCount(),
        ctx.getSqlTotalElapsed());

    // 2. Body/Payload 로깅 여부 결정
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

      String reqData =
          SensitiveDataMasker.maskIfEnabled(
              CommonUtil.truncate(ctx.getRequestData(), maxBodyLen), maskBody);
      String resData =
          SensitiveDataMasker.maskIfEnabled(
              CommonUtil.truncate(ctx.getResponseData(), maxBodyLen), maskBody);

      logger.info(
          LogMarker.NETTY_DETAIL.marker(),
          "trace_id={} span_id={} request=\"{}\" response=\"{}\"",
          traceId != null ? traceId : "-",
          spanId != null ? spanId : "-",
          escape(reqData),
          escape(resData));
    }

    logSqlDetails(traceId, spanId, level, isError);
    logException(ctx, traceId, spanId);
  }

  private String escape(String s) {
    if (s == null) return "";
    return s.replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
  }
}
