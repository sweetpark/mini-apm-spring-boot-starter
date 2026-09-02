package io.github.sweetpark.apm.core.process;

import io.github.sweetpark.apm.core.config.ApmProperties;
import io.github.sweetpark.apm.core.context.LogContext;
import io.github.sweetpark.apm.core.context.LogSqlContext;
import io.github.sweetpark.apm.core.context.TraceContextHolder;
import io.github.sweetpark.apm.core.enums.LogMarker;
import io.github.sweetpark.apm.core.enums.TraceLevel;
import io.github.sweetpark.apm.core.error.BreadcrumbEvent;
import io.github.sweetpark.apm.core.error.ErrorClassifier;
import io.github.sweetpark.apm.core.error.ErrorFingerprinter;
import io.github.sweetpark.apm.core.sql.SqlTraceContextHolder;
import io.github.sweetpark.apm.core.support.util.LogMessageBuilder;
import io.github.sweetpark.apm.core.support.util.SensitiveDataMasker;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 런타임 공통 로깅 처리 최상위 추상 클래스입니다. */
public abstract class AbstractLogProcessor<T extends LogContext> {

  protected final ApmProperties properties;
  protected final Logger logger = LoggerFactory.getLogger("ApmLog");

  protected AbstractLogProcessor(ApmProperties properties) {
    this.properties = properties;
  }

  protected abstract void logApi(T ctx);

  /** 로깅 실패가 비즈니스 요청으로 전파되지 않도록 감싸는 Fail-safe 진입점입니다. */
  public final void process(T ctx) {
    try {
      logApi(ctx);
    } catch (Exception ex) {
      logger.error(
          "[APM_INTERNAL_ERROR] traceId={} cause={}",
          ctx != null ? ctx.getTraceId() : "-",
          ex.getMessage());
    }
  }

  protected TraceLevel resolveLevel() {
    if (TraceContextHolder.isTrace()) {
      return TraceLevel.TRACE;
    }
    return TraceLevel.PROD;
  }

  protected void logSqlDetails(String traceId, String spanId, TraceLevel level, boolean isError) {
    if (!logger.isInfoEnabled()) {
      return;
    }

    long totalSqlElapsed = SqlTraceContextHolder.totalElapsed();
    int totalSlowLimit = properties.getSlow().getQuery().getTotalMs();
    int slowQueryLimit = properties.getSlow().getQuery().getMs();
    ApmProperties.CaptureMode sqlMode = properties.getCapture().getSql();

    if (totalSqlElapsed >= totalSlowLimit) {
      logger.info(
          LogMarker.SLOW_SQL.marker(),
          LogMessageBuilder.buildTotalSlow(traceId, spanId, totalSqlElapsed, totalSlowLimit));
    }

    for (LogSqlContext sql : SqlTraceContextHolder.getAll()) {
      boolean isSlow = sql.getElapsed() >= slowQueryLimit;
      boolean shouldLog = false;

      if (sqlMode == ApmProperties.CaptureMode.ALWAYS) {
        shouldLog = true;
      } else if (sqlMode == ApmProperties.CaptureMode.ERROR && (isError || sql.isError())) {
        shouldLog = true;
      } else if (sqlMode == ApmProperties.CaptureMode.SLOW
          && (isSlow || totalSqlElapsed >= totalSlowLimit)) {
        shouldLog = true;
      } else if (sqlMode == ApmProperties.CaptureMode.SAMPLE) {
        shouldLog = level == TraceLevel.TRACE;
      } else if (level == TraceLevel.TRACE) {
        shouldLog = true;
      }

      if (shouldLog || isError || sql.isError() || isSlow) {
        LogMarker marker;
        if (sql.isError()) {
          marker = LogMarker.SQL_EXCEPTION;
        } else if (isSlow) {
          marker = LogMarker.SLOW_SQL;
        } else {
          marker = LogMarker.SQL;
        }

        String sqlText = (sql.getSql() != null) ? sql.getSql() : "[SQL TEXT OMITTED BY POLICY]";
        String sqlParam = (level == TraceLevel.TRACE) ? sql.getSqlParam() : null;

        boolean maskSql =
            properties.getSecurity().isMaskingEnabled()
                && properties.getSecurity().isMaskSqlParam();
        sqlText = SensitiveDataMasker.maskIfEnabled(sqlText, maskSql);
        sqlParam = SensitiveDataMasker.maskIfEnabled(sqlParam, maskSql);

        logger.info(
            marker.marker(),
            LogMessageBuilder.buildSql(
                marker, traceId, spanId, sql.getSqlId(), sql.getElapsed(), sqlText, sqlParam));
      }
    }

    int omittedCount =
        SqlTraceContextHolder.get() != null ? SqlTraceContextHolder.get().getOmittedCount() : 0;
    if (omittedCount > 0) {
      logger.info(
          LogMarker.SQL.marker(), LogMessageBuilder.buildSqlOmitted(traceId, spanId, omittedCount));
    }
  }

  protected void logException(LogContext ctx, String traceId, String spanId) {
    if (!logger.isInfoEnabled() || ctx.getEx() == null) {
      return;
    }

    Throwable ex = ctx.getEx();
    String fingerprint = ErrorFingerprinter.fingerprint(ex);
    ErrorClassifier.ErrorType errorType = ErrorClassifier.classify(ex);
    List<BreadcrumbEvent> breadcrumbs = TraceContextHolder.getBreadcrumbs();
    LogMarker marker = resolveErrorMarker(errorType);

    logger.info(
        marker.marker(),
        LogMessageBuilder.buildError(
            traceId,
            spanId,
            fingerprint,
            errorType.getLabel(),
            breadcrumbs,
            ex,
            properties.getLimit().getMaxStackDepth(),
            properties.getLimit().getMaxStackLines()));
  }

  private LogMarker resolveErrorMarker(ErrorClassifier.ErrorType errorType) {
    return switch (errorType) {
      case BIZ -> LogMarker.ERROR_BIZ;
      case DATABASE -> LogMarker.ERROR_DB;
      case EXTERNAL -> LogMarker.ERROR_EXTERNAL;
      default -> LogMarker.ERROR_SYSTEM;
    };
  }
}
