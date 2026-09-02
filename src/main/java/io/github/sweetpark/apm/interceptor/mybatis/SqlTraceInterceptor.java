package io.github.sweetpark.apm.interceptor.mybatis;

import io.github.sweetpark.apm.core.config.ApmProperties;
import io.github.sweetpark.apm.core.config.ApmPropertiesHolder;
import io.github.sweetpark.apm.core.context.TraceContextHolder;
import io.github.sweetpark.apm.core.enums.LogMarker;
import io.github.sweetpark.apm.core.sql.SqlTraceContext;
import io.github.sweetpark.apm.core.sql.SqlTraceContextHolder;
import io.github.sweetpark.apm.core.support.util.SQLUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** MyBatis SQL 실행을 가로채어 실행 시간, 파라미터가 바인딩된 완성형 SQL 및 N+1 쿼리를 추적하는 인터셉터입니다. */
@Intercepts({
  @Signature(
      type = Executor.class,
      method = "update",
      args = {MappedStatement.class, Object.class}),
  @Signature(
      type = Executor.class,
      method = "query",
      args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
  @Signature(
      type = Executor.class,
      method = "query",
      args = {
        MappedStatement.class,
        Object.class,
        RowBounds.class,
        ResultHandler.class,
        CacheKey.class,
        BoundSql.class
      })
})
public class SqlTraceInterceptor implements Interceptor {

  private static final Logger logger = LoggerFactory.getLogger("ApmLog");

  private final ApmProperties properties;
  private static final ThreadLocal<Boolean> IS_LOGGING = ThreadLocal.withInitial(() -> false);

  public SqlTraceInterceptor() {
    this(null);
  }

  public SqlTraceInterceptor(ApmProperties properties) {
    this.properties = properties;
  }

  private ApmProperties getProperties() {
    if (this.properties != null) {
      return this.properties;
    }
    return ApmPropertiesHolder.getProperties();
  }

  @Override
  public Object intercept(Invocation invocation) throws Throwable {
    long start = System.currentTimeMillis();
    boolean isError = false;

    ApmProperties props = getProperties();
    if (props == null) {
      props = new ApmProperties();
    }

    if (Boolean.TRUE.equals(IS_LOGGING.get())) {
      return invocation.proceed();
    }

    IS_LOGGING.set(true);
    SqlTraceContextHolder.setMyBatisActive(true);

    try {
      return invocation.proceed();
    } catch (Throwable t) {
      isError = true;
      throw t;
    } finally {
      long elapsed = System.currentTimeMillis() - start;

      Object[] args = invocation.getArgs();
      MappedStatement ms = (MappedStatement) args[0];
      Object param = args.length > 1 ? args[1] : null;
      String sqlId = ms.getId();

      SqlTraceContext ctx = SqlTraceContextHolder.get();

      if (ctx != null) {
        int maxCount = props.getLimit().getMaxSqlCount();
        int maxDetailCount = props.getLimit().getMaxSqlDetailCount();

        boolean isFull = ctx.isFull(maxCount);

        if (isFull && isError) {
          ctx.removeOldestNormal();
          isFull = false;
        }

        if (isFull) {
          ctx.addOmitted();
        } else {
          boolean includeDetail = isError || !ctx.isDetailFull(maxDetailCount);
          String sql = null;
          String sqlParam = null;

          if (includeDetail) {
            int maxSqlLen = props.getLimit().getMaxSqlLength();
            int maxParamLen = props.getLimit().getMaxSqlParamLength();

            BoundSql boundSql = ms.getBoundSql(param);
            sql = buildBoundSql(boundSql, ms.getConfiguration(), param, maxSqlLen);
            sqlParam = extractSqlParam(boundSql, ms.getConfiguration(), param, maxParamLen);
          }

          ctx.add(sqlId, sql, sqlParam, elapsed, isError, includeDetail);
        }
      }

      TraceContextHolder.addBreadcrumb(isError ? "SQL_ERROR" : "SQL", sqlId + " " + elapsed + "ms");

      if (ctx != null) {
        int callCount = ctx.incrementCallCount(sqlId);
        int threshold = props.getLimit().getN1DetectionThreshold();

        if (callCount == threshold) {
          logger.warn(
              LogMarker.N1_QUERY.marker(),
              "trace_id={} sql_id={} call_count={} possible N+1 detected — consider batch fetch or"
                  + " IN clause",
              TraceContextHolder.traceId(),
              sqlId,
              callCount);
        }
      }

      SqlTraceContextHolder.setMyBatisActive(false);
      IS_LOGGING.remove();
    }
  }

  private String buildBoundSql(
      BoundSql boundSql, Configuration configuration, Object param, int maxLength) {
    String rawSql = boundSql.getSql();
    List<ParameterMapping> mappings = boundSql.getParameterMappings();

    if (mappings == null || mappings.isEmpty()) {
      return SQLUtil.normalizeSql(rawSql, maxLength);
    }

    MetaObject metaObject = (param == null) ? null : configuration.newMetaObject(param);
    Map<Integer, Object> paramsMap = new HashMap<>();
    int index = 1;

    for (ParameterMapping pm : mappings) {
      String prop = pm.getProperty();
      Object value;
      if (boundSql.hasAdditionalParameter(prop)) {
        value = boundSql.getAdditionalParameter(prop);
      } else if (metaObject != null && metaObject.hasGetter(prop)) {
        value = metaObject.getValue(prop);
      } else {
        value = null;
      }
      paramsMap.put(index++, value);
    }

    return SQLUtil.buildSqlWithParams(rawSql, paramsMap, maxLength);
  }

  private String extractSqlParam(
      BoundSql boundSql, Configuration configuration, Object param, int maxLength) {
    if (param == null) {
      return null;
    }

    List<ParameterMapping> mappings = boundSql.getParameterMappings();
    if (mappings == null || mappings.isEmpty()) {
      return null;
    }

    MetaObject metaObject = configuration.newMetaObject(param);
    StringBuilder sb = new StringBuilder("{");
    boolean first = true;

    for (ParameterMapping pm : mappings) {
      if (sb.length() >= maxLength) {
        sb.append("...(TRUNCATED)");
        break;
      }

      String prop = pm.getProperty();
      Object value;

      if (boundSql.hasAdditionalParameter(prop)) {
        value = boundSql.getAdditionalParameter(prop);
      } else if (metaObject.hasGetter(prop)) {
        value = metaObject.getValue(prop);
      } else {
        value = null;
      }

      if (!first) {
        sb.append(", ");
      }

      sb.append(prop).append("=").append(SQLUtil.formatValue(value));
      first = false;
    }

    sb.append("}");
    return sb.toString();
  }
}
