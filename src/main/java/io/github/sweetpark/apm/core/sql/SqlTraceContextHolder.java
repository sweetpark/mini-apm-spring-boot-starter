package io.github.sweetpark.apm.core.sql;

import io.github.sweetpark.apm.core.context.LogSqlContext;
import java.util.Collections;
import java.util.List;

/** ThreadLocal 기반의 SQL 트레이스 컨텍스트 홀더입니다. */
public final class SqlTraceContextHolder {

  private static final ThreadLocal<SqlTraceContext> CONTEXT = new ThreadLocal<>();
  private static final ThreadLocal<Boolean> MYBATIS_ACTIVE = ThreadLocal.withInitial(() -> false);

  private SqlTraceContextHolder() {}

  public static SqlTraceContext init() {
    SqlTraceContext ctx = new SqlTraceContext();
    CONTEXT.set(ctx);
    return ctx;
  }

  public static void set(SqlTraceContext ctx) {
    CONTEXT.set(ctx);
  }

  public static SqlTraceContext get() {
    return CONTEXT.get();
  }

  public static List<LogSqlContext> getAll() {
    SqlTraceContext ctx = CONTEXT.get();
    return ctx != null ? ctx.getSqlList() : Collections.emptyList();
  }

  public static long totalElapsed() {
    SqlTraceContext ctx = CONTEXT.get();
    return ctx != null ? ctx.getTotalElapsed() : 0;
  }

  public static int count() {
    SqlTraceContext ctx = CONTEXT.get();
    return ctx != null ? ctx.count() : 0;
  }

  public static boolean isMyBatisActive() {
    return Boolean.TRUE.equals(MYBATIS_ACTIVE.get());
  }

  public static void setMyBatisActive(boolean active) {
    MYBATIS_ACTIVE.set(active);
  }

  public static void clear() {
    CONTEXT.remove();
    MYBATIS_ACTIVE.remove();
  }
}
