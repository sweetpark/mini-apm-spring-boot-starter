package io.github.sweetpark.apm.core.context;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LogContextTest {

  static class TestLogContext extends LogContext {
    protected TestLogContext(Builder builder) {
      super(builder);
    }

    static class Builder extends LogContext.Builder<Builder> {
      @Override
      protected Builder self() {
        return this;
      }

      TestLogContext build() {
        return new TestLogContext(this);
      }
    }
  }

  @Test
  void testBuilderPopulatesAllFields() {
    Exception ex = new RuntimeException("boom");

    TestLogContext ctx =
        new TestLogContext.Builder()
            .traceId("trace-1")
            .spanId("span-1")
            .interfaceId("if-1")
            .ex(ex)
            .elapsedMs(12.5)
            .build();

    assertThat(ctx.getTraceId()).isEqualTo("trace-1");
    assertThat(ctx.getSpanId()).isEqualTo("span-1");
    assertThat(ctx.getInterfaceId()).isEqualTo("if-1");
    assertThat(ctx.getEx()).isSameAs(ex);
    assertThat(ctx.getElapsedMs()).isEqualTo(12.5);
  }
}
