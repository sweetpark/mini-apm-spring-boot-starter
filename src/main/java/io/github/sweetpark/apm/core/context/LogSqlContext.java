package io.github.sweetpark.apm.core.context;

/** 개별 SQL 실행 정보 및 측정 지표를 담는 컨텍스트 모델입니다. */
public class LogSqlContext {
  private final String sqlId;
  private final String sql;
  private final String sqlParam;
  private final long elapsed;
  private final boolean error;
  private final boolean includeDetail;

  public LogSqlContext(
      String sqlId,
      String sql,
      String sqlParam,
      long elapsed,
      boolean error,
      boolean includeDetail) {
    this.sqlId = sqlId;
    this.sql = sql;
    this.sqlParam = sqlParam;
    this.elapsed = elapsed;
    this.error = error;
    this.includeDetail = includeDetail;
  }

  public String getSqlId() {
    return sqlId;
  }

  public String getSql() {
    return sql;
  }

  public String getSqlParam() {
    return sqlParam;
  }

  public long getElapsed() {
    return elapsed;
  }

  public boolean isError() {
    return error;
  }

  public boolean isIncludeDetail() {
    return includeDetail;
  }
}
