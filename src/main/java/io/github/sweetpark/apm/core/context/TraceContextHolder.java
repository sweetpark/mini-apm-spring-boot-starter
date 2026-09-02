package io.github.sweetpark.apm.core.context;

import io.github.sweetpark.apm.core.enums.TraceLevel;
import io.github.sweetpark.apm.core.error.BreadcrumbEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 현재 스레드의 트레이스 식별자, 레벨 및 Breadcrumb 이력을 관리하는 ThreadLocal 홀더입니다. */
public final class TraceContextHolder {

  private static final int MAX_BREADCRUMBS = 20;

  private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();
  private static final ThreadLocal<String> SPAN_ID = new ThreadLocal<>();
  private static final ThreadLocal<TraceLevel> TRACE_LEVEL =
      ThreadLocal.withInitial(() -> TraceLevel.PROD);
  private static final ThreadLocal<Boolean> FORCE_TRACE = ThreadLocal.withInitial(() -> false);
  private static final ThreadLocal<List<BreadcrumbEvent>> BREADCRUMBS =
      ThreadLocal.withInitial(ArrayList::new);

  private TraceContextHolder() {}

  public static void init(String traceId, String spanId, TraceLevel level, boolean forceTrace) {
    TRACE_ID.set(traceId);
    SPAN_ID.set(spanId);
    TRACE_LEVEL.set(level != null ? level : TraceLevel.PROD);
    FORCE_TRACE.set(forceTrace);
    BREADCRUMBS.set(new ArrayList<>());
  }

  public static String traceId() {
    return TRACE_ID.get();
  }

  public static String spanId() {
    return SPAN_ID.get();
  }

  public static TraceLevel level() {
    return TRACE_LEVEL.get();
  }

  public static boolean isForceTrace() {
    return Boolean.TRUE.equals(FORCE_TRACE.get());
  }

  public static boolean isTrace() {
    return isForceTrace() || level() == TraceLevel.TRACE;
  }

  public static void addBreadcrumb(String category, String message) {
    List<BreadcrumbEvent> list = BREADCRUMBS.get();
    if (list == null) {
      list = new ArrayList<>();
      BREADCRUMBS.set(list);
    }
    if (list.size() < MAX_BREADCRUMBS) {
      list.add(new BreadcrumbEvent(category, message));
    }
  }

  public static List<BreadcrumbEvent> getBreadcrumbs() {
    List<BreadcrumbEvent> list = BREADCRUMBS.get();
    return list == null ? Collections.emptyList() : Collections.unmodifiableList(list);
  }

  public static void clear() {
    TRACE_ID.remove();
    SPAN_ID.remove();
    TRACE_LEVEL.remove();
    FORCE_TRACE.remove();
    BREADCRUMBS.remove();
  }
}
