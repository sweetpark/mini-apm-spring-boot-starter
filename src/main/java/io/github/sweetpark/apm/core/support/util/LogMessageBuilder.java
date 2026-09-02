package io.github.sweetpark.apm.core.support.util;

import io.github.sweetpark.apm.core.enums.LogMarker;
import io.github.sweetpark.apm.core.error.BreadcrumbEvent;
import java.util.List;

/** Grafana Loki logfmt 규격에 최적화된 로그 메시지를 빌드하는 클래스입니다. */
public final class LogMessageBuilder {

  private LogMessageBuilder() {}

  public static String buildSql(
      LogMarker marker,
      String traceId,
      String spanId,
      String sqlId,
      long elapsed,
      String sql,
      String sqlParam) {
    StringBuilder sb = new StringBuilder();
    sb.append("trace_id=")
        .append(traceId != null ? traceId : "-")
        .append(" span_id=")
        .append(spanId != null ? spanId : "-")
        .append(" sql_id=")
        .append(sqlId != null ? sqlId : "DYNAMIC_QUERY")
        .append(" elapsed=")
        .append(elapsed)
        .append("ms")
        .append(" sql=\"")
        .append(escape(sql))
        .append("\"");

    if (sqlParam != null && !sqlParam.isBlank()) {
      sb.append(" param=\"").append(escape(sqlParam)).append("\"");
    }

    return sb.toString();
  }

  public static String buildTotalSlow(String traceId, String spanId, long totalElapsed, int limit) {
    return String.format(
        "trace_id=%s span_id=%s total_sql_elapsed=%dms limit=%dms [TOTAL_SQL_SLOW]",
        traceId != null ? traceId : "-", spanId != null ? spanId : "-", totalElapsed, limit);
  }

  public static String buildSqlOmitted(String traceId, String spanId, int omittedCount) {
    return String.format(
        "trace_id=%s span_id=%s omitted_sql_count=%d [SQL_LIMIT_EXCEEDED]",
        traceId != null ? traceId : "-", spanId != null ? spanId : "-", omittedCount);
  }

  public static String buildError(
      String traceId,
      String spanId,
      String fingerprint,
      String errorType,
      List<BreadcrumbEvent> breadcrumbs,
      Throwable ex,
      int maxDepth,
      int maxLines) {
    StringBuilder sb = new StringBuilder();
    sb.append("trace_id=")
        .append(traceId != null ? traceId : "-")
        .append(" span_id=")
        .append(spanId != null ? spanId : "-")
        .append(" error_fingerprint=")
        .append(fingerprint)
        .append(" error_type=")
        .append(errorType)
        .append(" message=\"")
        .append(escape(ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()))
        .append("\"");

    if (breadcrumbs != null && !breadcrumbs.isEmpty()) {
      sb.append(" breadcrumbs=[");
      for (int i = 0; i < breadcrumbs.size(); i++) {
        if (i > 0) sb.append(", ");
        BreadcrumbEvent b = breadcrumbs.get(i);
        sb.append("{cat:\"")
            .append(b.category())
            .append("\",msg:\"")
            .append(escape(b.message()))
            .append("\"}");
      }
      sb.append("]");
    }

    sb.append(" stacktrace=\"");
    appendStackTrace(sb, ex, maxDepth, maxLines);
    sb.append("\"");

    return sb.toString();
  }

  private static void appendStackTrace(StringBuilder sb, Throwable ex, int maxDepth, int maxLines) {
    Throwable curr = ex;
    int depth = 0;
    while (curr != null && depth < maxDepth) {
      if (depth > 0) {
        sb.append(" | Caused by: ");
      }
      sb.append(curr.getClass().getName()).append(": ").append(escape(curr.getMessage()));
      StackTraceElement[] frames = curr.getStackTrace();
      for (int i = 0; i < Math.min(frames.length, maxLines); i++) {
        sb.append(" at ").append(frames[i].toString());
      }
      if (frames.length > maxLines) {
        sb.append(" ... (").append(frames.length - maxLines).append(" more)");
      }
      curr = curr.getCause();
      depth++;
    }
  }

  private static String escape(String s) {
    if (s == null) return "";
    return s.replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
  }
}
