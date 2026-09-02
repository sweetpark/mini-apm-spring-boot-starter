package io.github.sweetpark.apm.core.sql;

import io.github.sweetpark.apm.core.context.LogSqlContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 하나의 요청 또는 트랜잭션 내에서 실행된 SQL 통계 및 이력을 저장하는 컨텍스트 클래스입니다. */
public class SqlTraceContext {

  private final List<LogSqlContext> sqlList = new ArrayList<>();
  private final Map<String, Integer> callCounts = new HashMap<>();
  private long totalElapsed = 0;
  private int omittedCount = 0;
  private int detailCount = 0;

  public synchronized void add(
      String sqlId,
      String sql,
      String sqlParam,
      long elapsed,
      boolean error,
      boolean includeDetail) {
    sqlList.add(new LogSqlContext(sqlId, sql, sqlParam, elapsed, error, includeDetail));
    totalElapsed += elapsed;
    if (includeDetail) {
      detailCount++;
    }
  }

  public synchronized boolean isFull(int maxCount) {
    return sqlList.size() >= maxCount;
  }

  public synchronized boolean isDetailFull(int maxDetailCount) {
    return detailCount >= maxDetailCount;
  }

  public synchronized void addOmitted() {
    omittedCount++;
  }

  public synchronized void removeOldestNormal() {
    for (int i = 0; i < sqlList.size(); i++) {
      if (!sqlList.get(i).isError()) {
        LogSqlContext removed = sqlList.remove(i);
        if (removed.isIncludeDetail()) {
          detailCount--;
        }
        break;
      }
    }
  }

  public synchronized int incrementCallCount(String sqlId) {
    int count = callCounts.getOrDefault(sqlId, 0) + 1;
    callCounts.put(sqlId, count);
    return count;
  }

  public synchronized int getCallCount(String sqlId) {
    return callCounts.getOrDefault(sqlId, 0);
  }

  public synchronized List<LogSqlContext> getSqlList() {
    return Collections.unmodifiableList(new ArrayList<>(sqlList));
  }

  public synchronized long getTotalElapsed() {
    return totalElapsed;
  }

  public synchronized int count() {
    return sqlList.size() + omittedCount;
  }

  public synchronized int getOmittedCount() {
    return omittedCount;
  }
}
