package io.github.sweetpark.apm.interceptor.mybatis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.sweetpark.apm.core.config.ApmProperties;
import io.github.sweetpark.apm.core.config.ApmPropertiesHolder;
import io.github.sweetpark.apm.core.sql.SqlTraceContext;
import io.github.sweetpark.apm.core.sql.SqlTraceContextHolder;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.SqlSourceBuilder;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class SqlTraceInterceptorTest {

  private ListAppender<ILoggingEvent> appender;
  private Logger apmLogger;
  private Configuration configuration;

  @BeforeEach
  void setUp() {
    configuration = new Configuration();
    SqlTraceContextHolder.init();
    SqlTraceContextHolder.setMyBatisActive(false);

    apmLogger = (Logger) LoggerFactory.getLogger("ApmLog");
    appender = new ListAppender<>();
    appender.start();
    apmLogger.addAppender(appender);
  }

  @AfterEach
  void tearDown() {
    apmLogger.detachAppender(appender);
    SqlTraceContextHolder.clear();
    ApmPropertiesHolder.setProperties(null);
  }

  private MappedStatement buildMappedStatement(String id, String sql) {
    SqlSourceBuilder builder = new SqlSourceBuilder(configuration);
    SqlSource sqlSource = builder.parse(sql, Map.class, new HashMap<>());
    return new MappedStatement.Builder(configuration, id, sqlSource, SqlCommandType.SELECT).build();
  }

  private Invocation buildInvocation(Executor executor, Object[] args)
      throws NoSuchMethodException {
    Method method =
        Executor.class.getMethod(
            "query", MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class);
    return new Invocation(executor, method, args);
  }

  @Test
  @DisplayName("파라미터가 바인딩된 완성형 SQL이 SqlTraceContext에 기록된다")
  void testInterceptTracksBoundSql() throws Throwable {
    MappedStatement ms =
        buildMappedStatement("test.selectUser", "SELECT * FROM users WHERE id = #{id}");
    Map<String, Object> param = new HashMap<>();
    param.put("id", 42);

    Executor executor = mock(Executor.class);
    when(executor.query(any(), any(), any(), any())).thenReturn(List.of());
    Invocation invocation =
        buildInvocation(executor, new Object[] {ms, param, RowBounds.DEFAULT, null});

    SqlTraceInterceptor interceptor = new SqlTraceInterceptor(new ApmProperties());
    interceptor.intercept(invocation);

    SqlTraceContext ctx = SqlTraceContextHolder.get();
    assertThat(ctx.getSqlList()).hasSize(1);
    assertThat(ctx.getSqlList().get(0).getSql()).contains("42");
    assertThat(ctx.getSqlList().get(0).getSqlParam()).contains("id=42");
  }

  @Test
  @DisplayName("MyBatis 인터셉터 실행 동안에는 스마트 중복 방지 플래그가 활성화된다")
  void testSetsMyBatisActiveDuringExecution() throws Throwable {
    MappedStatement ms =
        buildMappedStatement("test.selectUser", "SELECT * FROM users WHERE id = #{id}");
    Map<String, Object> param = Map.of("id", 1);

    Executor executor = mock(Executor.class);
    when(executor.query(any(), any(), any(), any()))
        .thenAnswer(
            invocationOnMock -> {
              assertThat(SqlTraceContextHolder.isMyBatisActive()).isTrue();
              return List.of();
            });
    Invocation invocation =
        buildInvocation(executor, new Object[] {ms, param, RowBounds.DEFAULT, null});

    SqlTraceInterceptor interceptor = new SqlTraceInterceptor(new ApmProperties());
    interceptor.intercept(invocation);

    assertThat(SqlTraceContextHolder.isMyBatisActive()).isFalse();
  }

  @Test
  @DisplayName("동일 SQL이 임계치만큼 반복되면 N+1 경고를 로깅한다")
  void testN1QueryDetection() throws Throwable {
    ApmProperties props = new ApmProperties();
    props.getLimit().setN1DetectionThreshold(3);
    SqlTraceInterceptor interceptor = new SqlTraceInterceptor(props);

    MappedStatement ms =
        buildMappedStatement("test.selectItem", "SELECT * FROM items WHERE id = #{id}");
    Executor executor = mock(Executor.class);
    when(executor.query(any(), any(), any(), any())).thenReturn(List.of());

    for (int i = 0; i < 3; i++) {
      Map<String, Object> param = Map.of("id", i);
      Invocation invocation =
          buildInvocation(executor, new Object[] {ms, param, RowBounds.DEFAULT, null});
      interceptor.intercept(invocation);
    }

    assertThat(appender.list)
        .anyMatch(e -> e.getFormattedMessage().contains("possible N+1 detected"));
  }

  @Test
  @DisplayName("실행 중 예외가 발생하면 에러로 기록되고 예외가 전파된다")
  void testExceptionDuringExecutionIsMarkedAsError() throws NoSuchMethodException {
    MappedStatement ms =
        buildMappedStatement("test.selectUser", "SELECT * FROM users WHERE id = #{id}");
    Map<String, Object> param = Map.of("id", 1);

    Executor executor = mock(Executor.class);
    RuntimeException boom = new RuntimeException("boom");
    Invocation invocation =
        buildInvocation(executor, new Object[] {ms, param, RowBounds.DEFAULT, null});

    SqlTraceInterceptor interceptor = new SqlTraceInterceptor(new ApmProperties());
    try {
      when(executor.query(any(), any(), any(), any())).thenThrow(boom);
    } catch (Exception ignored) {
      // Mockito's when(...) on a checked-throwing method never actually throws here.
    }

    // Invocation.proceed() reflectively invokes the target method, so the
    // underlying exception arrives wrapped in InvocationTargetException.
    var thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            java.lang.reflect.InvocationTargetException.class,
            () -> interceptor.intercept(invocation));
    assertThat(thrown.getCause()).isSameAs(boom);

    SqlTraceContext ctx = SqlTraceContextHolder.get();
    assertThat(ctx.getSqlList()).anyMatch(sql -> sql.isError());
  }

  @Test
  @DisplayName("컨텍스트가 가득 차면 초과분은 omitted로 처리된다")
  void testOmittedWhenContextFull() throws Throwable {
    ApmProperties props = new ApmProperties();
    props.getLimit().setMaxSqlCount(1);
    SqlTraceInterceptor interceptor = new SqlTraceInterceptor(props);

    Executor executor = mock(Executor.class);
    when(executor.query(any(), any(), any(), any())).thenReturn(List.of());

    MappedStatement ms1 = buildMappedStatement("test.first", "SELECT 1");
    interceptor.intercept(
        buildInvocation(executor, new Object[] {ms1, null, RowBounds.DEFAULT, null}));

    MappedStatement ms2 = buildMappedStatement("test.second", "SELECT 2");
    interceptor.intercept(
        buildInvocation(executor, new Object[] {ms2, null, RowBounds.DEFAULT, null}));

    assertThat(SqlTraceContextHolder.get().getOmittedCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("properties가 null이면 ApmPropertiesHolder의 기본값을 사용한다")
  void testFallsBackToPropertiesHolder() throws Throwable {
    ApmProperties holderProps = new ApmProperties();
    holderProps.getLimit().setN1DetectionThreshold(99);
    ApmPropertiesHolder.setProperties(holderProps);

    SqlTraceInterceptor interceptor = new SqlTraceInterceptor();
    MappedStatement ms = buildMappedStatement("test.holderProps", "SELECT 1");

    Executor executor = mock(Executor.class);
    when(executor.query(any(), any(), any(), any())).thenReturn(List.of());
    Invocation invocation =
        buildInvocation(executor, new Object[] {ms, null, RowBounds.DEFAULT, null});

    interceptor.intercept(invocation);

    assertThat(SqlTraceContextHolder.get().getSqlList()).hasSize(1);
  }

  @Test
  @DisplayName("파라미터가 없는 SQL은 sqlParam 없이 기록된다")
  void testNullParameter() throws Throwable {
    MappedStatement ms = buildMappedStatement("test.noParam", "SELECT * FROM users");
    Executor executor = mock(Executor.class);
    when(executor.query(any(), any(), any(), any())).thenReturn(List.of());
    Invocation invocation =
        buildInvocation(executor, new Object[] {ms, null, RowBounds.DEFAULT, null});

    SqlTraceInterceptor interceptor = new SqlTraceInterceptor(new ApmProperties());
    interceptor.intercept(invocation);

    assertThat(SqlTraceContextHolder.get().getSqlList().get(0).getSqlParam()).isNull();
  }
}
