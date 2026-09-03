package io.github.sweetpark.apm.core.sql;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sweetpark.apm.core.context.LogSqlContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SqlTraceContextTest {

  @Test
  @DisplayName("add() 이후 목록/누적시간/카운트 검증")
  void testAddAndAggregates() {
    SqlTraceContext ctx = new SqlTraceContext();

    ctx.add("sql1", "SELECT 1", "p=1", 10L, false, true);
    ctx.add("sql2", "SELECT 2", "p=2", 20L, false, true);

    assertThat(ctx.getSqlList()).hasSize(2);
    assertThat(ctx.getTotalElapsed()).isEqualTo(30L);
    assertThat(ctx.count()).isEqualTo(2);
    assertThat(ctx.getOmittedCount()).isZero();
  }

  @Test
  @DisplayName("isFull / isDetailFull 임계치 판정")
  void testFullChecks() {
    SqlTraceContext ctx = new SqlTraceContext();
    ctx.add("sql1", "SELECT 1", null, 5L, false, true);

    assertThat(ctx.isFull(1)).isTrue();
    assertThat(ctx.isFull(2)).isFalse();
    assertThat(ctx.isDetailFull(1)).isTrue();
    assertThat(ctx.isDetailFull(2)).isFalse();
  }

  @Test
  @DisplayName("addOmitted() 누적 카운트 검증")
  void testAddOmitted() {
    SqlTraceContext ctx = new SqlTraceContext();
    ctx.addOmitted();
    ctx.addOmitted();

    assertThat(ctx.getOmittedCount()).isEqualTo(2);
    assertThat(ctx.count()).isEqualTo(2);
  }

  @Test
  @DisplayName("removeOldestNormal()은 에러가 아닌 첫 항목만 제거")
  void testRemoveOldestNormalSkipsErrors() {
    SqlTraceContext ctx = new SqlTraceContext();
    ctx.add("errSql", "SELECT err", null, 1L, true, true);
    ctx.add("normalSql", "SELECT ok", null, 2L, false, true);

    ctx.removeOldestNormal();

    assertThat(ctx.getSqlList()).hasSize(1);
    assertThat(ctx.getSqlList().get(0).getSqlId()).isEqualTo("errSql");
  }

  @Test
  @DisplayName("removeOldestNormal()은 대상이 없으면 아무 것도 하지 않음")
  void testRemoveOldestNormalNoop() {
    SqlTraceContext ctx = new SqlTraceContext();
    ctx.add("errSql", "SELECT err", null, 1L, true, true);

    ctx.removeOldestNormal();

    assertThat(ctx.getSqlList()).hasSize(1);
  }

  @Test
  @DisplayName("removeOldestNormal()이 상세 포함 항목을 지우면 detailCount 감소")
  void testRemoveOldestNormalDecrementsDetailCount() {
    SqlTraceContext ctx = new SqlTraceContext();
    ctx.add("normalSql", "SELECT ok", null, 2L, false, true);

    assertThat(ctx.isDetailFull(1)).isTrue();

    ctx.removeOldestNormal();

    assertThat(ctx.isDetailFull(1)).isFalse();
  }

  @Test
  @DisplayName("incrementCallCount / getCallCount로 N+1 호출 횟수 추적")
  void testCallCountTracking() {
    SqlTraceContext ctx = new SqlTraceContext();

    assertThat(ctx.getCallCount("sqlA")).isZero();
    assertThat(ctx.incrementCallCount("sqlA")).isEqualTo(1);
    assertThat(ctx.incrementCallCount("sqlA")).isEqualTo(2);
    assertThat(ctx.incrementCallCount("sqlB")).isEqualTo(1);
    assertThat(ctx.getCallCount("sqlA")).isEqualTo(2);
  }

  @Test
  @DisplayName("getSqlList()는 방어적 복사본을 반환하여 외부 변경에 영향받지 않음")
  void testGetSqlListIsImmutable() {
    SqlTraceContext ctx = new SqlTraceContext();
    ctx.add("sql1", "SELECT 1", null, 1L, false, true);

    assertThat(ctx.getSqlList()).hasSize(1);
    org.junit.jupiter.api.Assertions.assertThrows(
        UnsupportedOperationException.class,
        () -> ctx.getSqlList().add(new LogSqlContext("x", "x", null, 0, false, false)));
  }
}
