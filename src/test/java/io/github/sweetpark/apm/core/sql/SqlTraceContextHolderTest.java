package io.github.sweetpark.apm.core.sql;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SqlTraceContextHolderTest {

  @AfterEach
  void cleanup() {
    SqlTraceContextHolder.clear();
  }

  @Test
  @DisplayName("컨텍스트 미초기화 상태에서는 안전한 기본값 반환")
  void testDefaultsWithoutInit() {
    assertThat(SqlTraceContextHolder.get()).isNull();
    assertThat(SqlTraceContextHolder.getAll()).isEmpty();
    assertThat(SqlTraceContextHolder.totalElapsed()).isZero();
    assertThat(SqlTraceContextHolder.count()).isZero();
    assertThat(SqlTraceContextHolder.isMyBatisActive()).isFalse();
  }

  @Test
  @DisplayName("init() 이후 get/getAll/totalElapsed/count 위임 검증")
  void testInitDelegatesToContext() {
    SqlTraceContext ctx = SqlTraceContextHolder.init();
    ctx.add("sql1", "SELECT 1", null, 15L, false, true);

    assertThat(SqlTraceContextHolder.get()).isSameAs(ctx);
    assertThat(SqlTraceContextHolder.getAll()).hasSize(1);
    assertThat(SqlTraceContextHolder.totalElapsed()).isEqualTo(15L);
    assertThat(SqlTraceContextHolder.count()).isEqualTo(1);
  }

  @Test
  @DisplayName("set()으로 외부 컨텍스트 교체 가능")
  void testSetReplacesContext() {
    SqlTraceContext replacement = new SqlTraceContext();
    replacement.add("sqlX", "SELECT X", null, 1L, false, true);

    SqlTraceContextHolder.set(replacement);

    assertThat(SqlTraceContextHolder.get()).isSameAs(replacement);
    assertThat(SqlTraceContextHolder.count()).isEqualTo(1);
  }

  @Test
  @DisplayName("MyBatis 활성 플래그로 JDBC 프록시의 스마트 중복 방지 신호 전달")
  void testMyBatisActiveFlag() {
    assertThat(SqlTraceContextHolder.isMyBatisActive()).isFalse();

    SqlTraceContextHolder.setMyBatisActive(true);
    assertThat(SqlTraceContextHolder.isMyBatisActive()).isTrue();

    SqlTraceContextHolder.setMyBatisActive(false);
    assertThat(SqlTraceContextHolder.isMyBatisActive()).isFalse();
  }

  @Test
  @DisplayName("clear() 이후 모든 ThreadLocal 상태 초기화")
  void testClearResetsEverything() {
    SqlTraceContextHolder.init();
    SqlTraceContextHolder.setMyBatisActive(true);

    SqlTraceContextHolder.clear();

    assertThat(SqlTraceContextHolder.get()).isNull();
    assertThat(SqlTraceContextHolder.isMyBatisActive()).isFalse();
  }
}
