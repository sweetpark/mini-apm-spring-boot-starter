package io.github.sweetpark.apm.core.context;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sweetpark.apm.core.enums.TraceLevel;
import io.github.sweetpark.apm.core.error.BreadcrumbEvent;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TraceContextHolderTest {

  @AfterEach
  void cleanup() {
    TraceContextHolder.clear();
  }

  @Test
  @DisplayName("init 이후 traceId/spanId/level 조회")
  void testInitAndAccessors() {
    TraceContextHolder.init("trace-1", "span-1", TraceLevel.TRACE, false);

    assertThat(TraceContextHolder.traceId()).isEqualTo("trace-1");
    assertThat(TraceContextHolder.spanId()).isEqualTo("span-1");
    assertThat(TraceContextHolder.level()).isEqualTo(TraceLevel.TRACE);
    assertThat(TraceContextHolder.isForceTrace()).isFalse();
    assertThat(TraceContextHolder.isTrace()).isTrue();
  }

  @Test
  @DisplayName("level null 입력 시 PROD로 기본값 대체")
  void testNullLevelDefaultsToProd() {
    TraceContextHolder.init("trace-2", "span-2", null, false);
    assertThat(TraceContextHolder.level()).isEqualTo(TraceLevel.PROD);
    assertThat(TraceContextHolder.isTrace()).isFalse();
  }

  @Test
  @DisplayName("forceTrace가 true이면 level과 무관하게 isTrace true")
  void testForceTraceOverridesLevel() {
    TraceContextHolder.init("trace-3", "span-3", TraceLevel.PROD, true);
    assertThat(TraceContextHolder.isForceTrace()).isTrue();
    assertThat(TraceContextHolder.isTrace()).isTrue();
  }

  @Test
  @DisplayName("init 없이 조회 시 기본값 반환")
  void testDefaultsWithoutInit() {
    assertThat(TraceContextHolder.traceId()).isNull();
    assertThat(TraceContextHolder.spanId()).isNull();
    assertThat(TraceContextHolder.level()).isEqualTo(TraceLevel.PROD);
    assertThat(TraceContextHolder.isForceTrace()).isFalse();
    assertThat(TraceContextHolder.getBreadcrumbs()).isEmpty();
  }

  @Test
  @DisplayName("Breadcrumb 추가 및 최대 개수(20개) 제한")
  void testBreadcrumbLimit() {
    TraceContextHolder.init("trace-4", "span-4", TraceLevel.PROD, false);

    for (int i = 0; i < 25; i++) {
      TraceContextHolder.addBreadcrumb("CAT" + i, "msg" + i);
    }

    List<BreadcrumbEvent> breadcrumbs = TraceContextHolder.getBreadcrumbs();
    assertThat(breadcrumbs).hasSize(20);
    assertThat(breadcrumbs.get(0).category()).isEqualTo("CAT0");
  }

  @Test
  @DisplayName("clear 이후 상태 초기화")
  void testClearResetsState() {
    TraceContextHolder.init("trace-5", "span-5", TraceLevel.TRACE, true);
    TraceContextHolder.addBreadcrumb("CAT", "msg");

    TraceContextHolder.clear();

    assertThat(TraceContextHolder.traceId()).isNull();
    assertThat(TraceContextHolder.spanId()).isNull();
    assertThat(TraceContextHolder.level()).isEqualTo(TraceLevel.PROD);
    assertThat(TraceContextHolder.isForceTrace()).isFalse();
    assertThat(TraceContextHolder.getBreadcrumbs()).isEmpty();
  }
}
