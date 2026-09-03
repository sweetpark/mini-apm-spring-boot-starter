package io.github.sweetpark.apm.core.context;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LogSqlContextTest {

  @Test
  void testAccessors() {
    LogSqlContext ctx = new LogSqlContext("findUser", "SELECT 1", "id=1", 42L, true, false);

    assertThat(ctx.getSqlId()).isEqualTo("findUser");
    assertThat(ctx.getSql()).isEqualTo("SELECT 1");
    assertThat(ctx.getSqlParam()).isEqualTo("id=1");
    assertThat(ctx.getElapsed()).isEqualTo(42L);
    assertThat(ctx.isError()).isTrue();
    assertThat(ctx.isIncludeDetail()).isFalse();
  }
}
