package io.github.sweetpark.apm.support.batch;

import io.github.sweetpark.apm.core.config.ApmProperties;
import io.github.sweetpark.apm.core.config.ApmPropertiesHolder;
import io.github.sweetpark.apm.core.context.TraceContextHolder;
import io.github.sweetpark.apm.core.enums.TraceLevel;
import io.github.sweetpark.apm.core.sql.SqlTraceContextHolder;
import io.github.sweetpark.apm.core.support.util.TraceIdUtil;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.lang.NonNull;

/** 멀티스레드 배치 실행 시 부모 스레드의 TraceId를 자식 워커 스레드로 전파하고 독립 SpanId를 부여하는 데코레이터입니다. */
public class LoggingTaskDecorator implements TaskDecorator {

  private volatile BatchLogProcessorAdapter adapter;

  @Override
  @NonNull
  public Runnable decorate(@NonNull Runnable runnable) {
    Map<String, String> contextMap = MDC.getCopyOfContextMap();
    String traceId = TraceContextHolder.traceId();
    TraceLevel level = TraceContextHolder.level();
    boolean forceTrace = TraceContextHolder.isForceTrace();

    return () -> {
      String childSpanId = TraceIdUtil.generateSpanId();
      try {
        if (contextMap != null) {
          MDC.setContextMap(contextMap);
        }
        MDC.put("spanId", childSpanId);

        TraceContextHolder.init(traceId, childSpanId, level, forceTrace);
        SqlTraceContextHolder.init();

        runnable.run();

        logTaskSql(traceId, childSpanId, level);
      } finally {
        SqlTraceContextHolder.clear();
        TraceContextHolder.clear();
        MDC.clear();
      }
    };
  }

  private void logTaskSql(String traceId, String spanId, TraceLevel level) {
    ApmProperties props = ApmPropertiesHolder.getProperties();
    if (props == null) {
      return;
    }
    getAdapter(props).logSqlOnly(traceId, spanId, level);
  }

  private BatchLogProcessorAdapter getAdapter(ApmProperties props) {
    if (adapter == null) {
      synchronized (this) {
        if (adapter == null) {
          adapter = new BatchLogProcessorAdapter(props);
        }
      }
    }
    return adapter;
  }
}
