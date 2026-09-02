package io.github.sweetpark.apm.interceptor.jpa;

import io.github.sweetpark.apm.core.config.ApmProperties;
import io.github.sweetpark.apm.core.context.TraceContextHolder;
import io.github.sweetpark.apm.core.enums.LogMarker;
import io.github.sweetpark.apm.core.sql.SqlTraceContext;
import io.github.sweetpark.apm.core.sql.SqlTraceContextHolder;
import io.github.sweetpark.apm.core.support.util.SQLUtil;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** JDBC PreparedStatement를 프록시하여 바인딩 파라미터와 쿼리 실행 시간을 측정하는 래퍼 클래스입니다. */
public class ApmProxyPreparedStatement implements PreparedStatement {

  private static final Logger logger = LoggerFactory.getLogger("ApmLog");

  private final PreparedStatement delegate;
  private final String rawSql;
  private final ApmProperties properties;
  private final Map<Integer, Object> parameters = new HashMap<>();

  public ApmProxyPreparedStatement(
      PreparedStatement delegate, String rawSql, ApmProperties properties) {
    this.delegate = delegate;
    this.rawSql = rawSql;
    this.properties = properties;
  }

  private void trackExecution(long startMs, boolean isError) {
    if (SqlTraceContextHolder.isMyBatisActive()) {
      return;
    }

    long elapsed = System.currentTimeMillis() - startMs;
    String sqlId = extractSqlId(rawSql);

    ApmProperties props = properties != null ? properties : new ApmProperties();
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

          sql = SQLUtil.buildSqlWithParams(rawSql, parameters, maxSqlLen);
          sqlParam = extractParametersString(maxParamLen);
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
            "trace_id={} sql_id={} call_count={} possible N+1 detected — consider fetch join or"
                + " batch size",
            TraceContextHolder.traceId(),
            sqlId,
            callCount);
      }
    }
  }

  private String extractSqlId(String sql) {
    if (sql == null || sql.isBlank()) {
      return "JDBC:QUERY";
    }
    String normalized = sql.replaceAll("\\s+", " ").trim();
    String[] tokens = normalized.split(" ");
    if (tokens.length >= 3) {
      return (tokens[0] + " " + tokens[1] + " " + tokens[2]).toUpperCase();
    }
    return tokens[0].toUpperCase();
  }

  private String extractParametersString(int maxLength) {
    if (parameters.isEmpty()) {
      return null;
    }
    StringBuilder sb = new StringBuilder("{");
    boolean first = true;
    for (Map.Entry<Integer, Object> entry : parameters.entrySet()) {
      if (sb.length() >= maxLength) {
        sb.append("...(TRUNCATED)");
        break;
      }
      if (!first) {
        sb.append(", ");
      }
      sb.append(entry.getKey()).append("=").append(SQLUtil.formatValue(entry.getValue()));
      first = false;
    }
    sb.append("}");
    return sb.toString();
  }

  @Override
  public ResultSet executeQuery() throws SQLException {
    long start = System.currentTimeMillis();
    boolean error = false;
    try {
      return delegate.executeQuery();
    } catch (SQLException e) {
      error = true;
      throw e;
    } finally {
      trackExecution(start, error);
    }
  }

  @Override
  public int executeUpdate() throws SQLException {
    long start = System.currentTimeMillis();
    boolean error = false;
    try {
      return delegate.executeUpdate();
    } catch (SQLException e) {
      error = true;
      throw e;
    } finally {
      trackExecution(start, error);
    }
  }

  @Override
  public boolean execute() throws SQLException {
    long start = System.currentTimeMillis();
    boolean error = false;
    try {
      return delegate.execute();
    } catch (SQLException e) {
      error = true;
      throw e;
    } finally {
      trackExecution(start, error);
    }
  }

  @Override
  public int[] executeBatch() throws SQLException {
    long start = System.currentTimeMillis();
    boolean error = false;
    try {
      return delegate.executeBatch();
    } catch (SQLException e) {
      error = true;
      throw e;
    } finally {
      trackExecution(start, error);
    }
  }

  @Override
  public long executeLargeUpdate() throws SQLException {
    long start = System.currentTimeMillis();
    boolean error = false;
    try {
      return delegate.executeLargeUpdate();
    } catch (SQLException e) {
      error = true;
      throw e;
    } finally {
      trackExecution(start, error);
    }
  }

  @Override
  public void setNull(int parameterIndex, int sqlType) throws SQLException {
    parameters.put(parameterIndex, null);
    delegate.setNull(parameterIndex, sqlType);
  }

  @Override
  public void setBoolean(int parameterIndex, boolean x) throws SQLException {
    parameters.put(parameterIndex, x);
    delegate.setBoolean(parameterIndex, x);
  }

  @Override
  public void setByte(int parameterIndex, byte x) throws SQLException {
    parameters.put(parameterIndex, x);
    delegate.setByte(parameterIndex, x);
  }

  @Override
  public void setShort(int parameterIndex, short x) throws SQLException {
    parameters.put(parameterIndex, x);
    delegate.setShort(parameterIndex, x);
  }

  @Override
  public void setInt(int parameterIndex, int x) throws SQLException {
    parameters.put(parameterIndex, x);
    delegate.setInt(parameterIndex, x);
  }

  @Override
  public void setLong(int parameterIndex, long x) throws SQLException {
    parameters.put(parameterIndex, x);
    delegate.setLong(parameterIndex, x);
  }

  @Override
  public void setFloat(int parameterIndex, float x) throws SQLException {
    parameters.put(parameterIndex, x);
    delegate.setFloat(parameterIndex, x);
  }

  @Override
  public void setDouble(int parameterIndex, double x) throws SQLException {
    parameters.put(parameterIndex, x);
    delegate.setDouble(parameterIndex, x);
  }

  @Override
  public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
    parameters.put(parameterIndex, x);
    delegate.setBigDecimal(parameterIndex, x);
  }

  @Override
  public void setString(int parameterIndex, String x) throws SQLException {
    parameters.put(parameterIndex, x);
    delegate.setString(parameterIndex, x);
  }

  @Override
  public void setBytes(int parameterIndex, byte[] x) throws SQLException {
    parameters.put(parameterIndex, "[BYTES]");
    delegate.setBytes(parameterIndex, x);
  }

  @Override
  public void setDate(int parameterIndex, Date x) throws SQLException {
    parameters.put(parameterIndex, x);
    delegate.setDate(parameterIndex, x);
  }

  @Override
  public void setTime(int parameterIndex, Time x) throws SQLException {
    parameters.put(parameterIndex, x);
    delegate.setTime(parameterIndex, x);
  }

  @Override
  public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
    parameters.put(parameterIndex, x);
    delegate.setTimestamp(parameterIndex, x);
  }

  @Override
  public void setObject(int parameterIndex, Object x) throws SQLException {
    parameters.put(parameterIndex, x);
    delegate.setObject(parameterIndex, x);
  }

  @Override
  public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
    parameters.put(parameterIndex, x);
    delegate.setObject(parameterIndex, x, targetSqlType);
  }

  @Override
  public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength)
      throws SQLException {
    parameters.put(parameterIndex, x);
    delegate.setObject(parameterIndex, x, targetSqlType, scaleOrLength);
  }

  @Override
  public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
    parameters.put(parameterIndex, null);
    delegate.setNull(parameterIndex, sqlType, typeName);
  }

  @Override
  public void clearParameters() throws SQLException {
    parameters.clear();
    delegate.clearParameters();
  }

  @Override
  public void close() throws SQLException {
    delegate.close();
  }

  @Override
  public boolean isClosed() throws SQLException {
    return delegate.isClosed();
  }

  @Override
  public Connection getConnection() throws SQLException {
    return delegate.getConnection();
  }

  @Override
  public ResultSet getResultSet() throws SQLException {
    return delegate.getResultSet();
  }

  @Override
  public int getUpdateCount() throws SQLException {
    return delegate.getUpdateCount();
  }

  @Override
  public boolean getMoreResults() throws SQLException {
    return delegate.getMoreResults();
  }

  @Override
  public boolean getMoreResults(int current) throws SQLException {
    return delegate.getMoreResults(current);
  }

  @Override
  public ResultSet getGeneratedKeys() throws SQLException {
    return delegate.getGeneratedKeys();
  }

  @Override
  public void cancel() throws SQLException {
    delegate.cancel();
  }

  @Override
  public SQLWarning getWarnings() throws SQLException {
    return delegate.getWarnings();
  }

  @Override
  public void clearWarnings() throws SQLException {
    delegate.clearWarnings();
  }

  @Override
  public void setCursorName(String name) throws SQLException {
    delegate.setCursorName(name);
  }

  @Override
  public int getMaxFieldSize() throws SQLException {
    return delegate.getMaxFieldSize();
  }

  @Override
  public void setMaxFieldSize(int max) throws SQLException {
    delegate.setMaxFieldSize(max);
  }

  @Override
  public int getMaxRows() throws SQLException {
    return delegate.getMaxRows();
  }

  @Override
  public void setMaxRows(int max) throws SQLException {
    delegate.setMaxRows(max);
  }

  @Override
  public void setEscapeProcessing(boolean enable) throws SQLException {
    delegate.setEscapeProcessing(enable);
  }

  @Override
  public int getQueryTimeout() throws SQLException {
    return delegate.getQueryTimeout();
  }

  @Override
  public void setQueryTimeout(int seconds) throws SQLException {
    delegate.setQueryTimeout(seconds);
  }

  @Override
  public void setFetchDirection(int direction) throws SQLException {
    delegate.setFetchDirection(direction);
  }

  @Override
  public int getFetchDirection() throws SQLException {
    return delegate.getFetchDirection();
  }

  @Override
  public void setFetchSize(int rows) throws SQLException {
    delegate.setFetchSize(rows);
  }

  @Override
  public int getFetchSize() throws SQLException {
    return delegate.getFetchSize();
  }

  @Override
  public int getResultSetConcurrency() throws SQLException {
    return delegate.getResultSetConcurrency();
  }

  @Override
  public int getResultSetType() throws SQLException {
    return delegate.getResultSetType();
  }

  @Override
  public void addBatch() throws SQLException {
    delegate.addBatch();
  }

  @Override
  public void addBatch(String sql) throws SQLException {
    delegate.addBatch(sql);
  }

  @Override
  public void clearBatch() throws SQLException {
    delegate.clearBatch();
  }

  @Override
  public ResultSetMetaData getMetaData() throws SQLException {
    return delegate.getMetaData();
  }

  @Override
  public ParameterMetaData getParameterMetaData() throws SQLException {
    return delegate.getParameterMetaData();
  }

  @Override
  public void setPoolable(boolean poolable) throws SQLException {
    delegate.setPoolable(poolable);
  }

  @Override
  public boolean isPoolable() throws SQLException {
    return delegate.isPoolable();
  }

  @Override
  public void closeOnCompletion() throws SQLException {
    delegate.closeOnCompletion();
  }

  @Override
  public boolean isCloseOnCompletion() throws SQLException {
    return delegate.isCloseOnCompletion();
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    if (iface.isInstance(this)) {
      return iface.cast(this);
    }
    if (iface.isInstance(delegate)) {
      return iface.cast(delegate);
    }
    return delegate.unwrap(iface);
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return iface.isInstance(this) || iface.isInstance(delegate) || delegate.isWrapperFor(iface);
  }

  @Override
  public ResultSet executeQuery(String sql) throws SQLException {
    long start = System.currentTimeMillis();
    boolean error = false;
    try {
      return delegate.executeQuery(sql);
    } catch (SQLException e) {
      error = true;
      throw e;
    } finally {
      trackExecution(start, error);
    }
  }

  @Override
  public int executeUpdate(String sql) throws SQLException {
    long start = System.currentTimeMillis();
    boolean error = false;
    try {
      return delegate.executeUpdate(sql);
    } catch (SQLException e) {
      error = true;
      throw e;
    } finally {
      trackExecution(start, error);
    }
  }

  @Override
  public boolean execute(String sql) throws SQLException {
    long start = System.currentTimeMillis();
    boolean error = false;
    try {
      return delegate.execute(sql);
    } catch (SQLException e) {
      error = true;
      throw e;
    } finally {
      trackExecution(start, error);
    }
  }

  @Override
  public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
    long start = System.currentTimeMillis();
    boolean error = false;
    try {
      return delegate.executeUpdate(sql, autoGeneratedKeys);
    } catch (SQLException e) {
      error = true;
      throw e;
    } finally {
      trackExecution(start, error);
    }
  }

  @Override
  public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
    long start = System.currentTimeMillis();
    boolean error = false;
    try {
      return delegate.executeUpdate(sql, columnIndexes);
    } catch (SQLException e) {
      error = true;
      throw e;
    } finally {
      trackExecution(start, error);
    }
  }

  @Override
  public int executeUpdate(String sql, String[] columnNames) throws SQLException {
    long start = System.currentTimeMillis();
    boolean error = false;
    try {
      return delegate.executeUpdate(sql, columnNames);
    } catch (SQLException e) {
      error = true;
      throw e;
    } finally {
      trackExecution(start, error);
    }
  }

  @Override
  public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
    long start = System.currentTimeMillis();
    boolean error = false;
    try {
      return delegate.execute(sql, autoGeneratedKeys);
    } catch (SQLException e) {
      error = true;
      throw e;
    } finally {
      trackExecution(start, error);
    }
  }

  @Override
  public boolean execute(String sql, int[] columnIndexes) throws SQLException {
    long start = System.currentTimeMillis();
    boolean error = false;
    try {
      return delegate.execute(sql, columnIndexes);
    } catch (SQLException e) {
      error = true;
      throw e;
    } finally {
      trackExecution(start, error);
    }
  }

  @Override
  public boolean execute(String sql, String[] columnNames) throws SQLException {
    long start = System.currentTimeMillis();
    boolean error = false;
    try {
      return delegate.execute(sql, columnNames);
    } catch (SQLException e) {
      error = true;
      throw e;
    } finally {
      trackExecution(start, error);
    }
  }

  @Override
  public int getResultSetHoldability() throws SQLException {
    return delegate.getResultSetHoldability();
  }

  @Override
  public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
    parameters.put(parameterIndex, "[ASCII_STREAM]");
    delegate.setAsciiStream(parameterIndex, x, length);
  }

  @Override
  public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
    parameters.put(parameterIndex, "[UNICODE_STREAM]");
    delegate.setUnicodeStream(parameterIndex, x, length);
  }

  @Override
  public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
    parameters.put(parameterIndex, "[BINARY_STREAM]");
    delegate.setBinaryStream(parameterIndex, x, length);
  }

  @Override
  public void setCharacterStream(int parameterIndex, Reader reader, int length)
      throws SQLException {
    parameters.put(parameterIndex, "[CHAR_STREAM]");
    delegate.setCharacterStream(parameterIndex, reader, length);
  }

  @Override
  public void setRef(int parameterIndex, Ref x) throws SQLException {
    parameters.put(parameterIndex, x);
    delegate.setRef(parameterIndex, x);
  }

  @Override
  public void setBlob(int parameterIndex, Blob x) throws SQLException {
    parameters.put(parameterIndex, "[BLOB]");
    delegate.setBlob(parameterIndex, x);
  }

  @Override
  public void setClob(int parameterIndex, Clob x) throws SQLException {
    parameters.put(parameterIndex, "[CLOB]");
    delegate.setClob(parameterIndex, x);
  }

  @Override
  public void setArray(int parameterIndex, Array x) throws SQLException {
    parameters.put(parameterIndex, x);
    delegate.setArray(parameterIndex, x);
  }

  @Override
  public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
    parameters.put(parameterIndex, x);
    delegate.setDate(parameterIndex, x, cal);
  }

  @Override
  public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
    parameters.put(parameterIndex, x);
    delegate.setTime(parameterIndex, x, cal);
  }

  @Override
  public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
    parameters.put(parameterIndex, x);
    delegate.setTimestamp(parameterIndex, x, cal);
  }

  @Override
  public void setURL(int parameterIndex, URL x) throws SQLException {
    parameters.put(parameterIndex, x);
    delegate.setURL(parameterIndex, x);
  }

  @Override
  public void setRowId(int parameterIndex, RowId x) throws SQLException {
    parameters.put(parameterIndex, x);
    delegate.setRowId(parameterIndex, x);
  }

  @Override
  public void setNString(int parameterIndex, String value) throws SQLException {
    parameters.put(parameterIndex, value);
    delegate.setNString(parameterIndex, value);
  }

  @Override
  public void setNCharacterStream(int parameterIndex, Reader value, long length)
      throws SQLException {
    parameters.put(parameterIndex, "[NCHAR_STREAM]");
    delegate.setNCharacterStream(parameterIndex, value, length);
  }

  @Override
  public void setNClob(int parameterIndex, NClob value) throws SQLException {
    parameters.put(parameterIndex, "[NCLOB]");
    delegate.setNClob(parameterIndex, value);
  }

  @Override
  public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
    parameters.put(parameterIndex, "[CLOB_READER]");
    delegate.setClob(parameterIndex, reader, length);
  }

  @Override
  public void setBlob(int parameterIndex, InputStream inputStream, long length)
      throws SQLException {
    parameters.put(parameterIndex, "[BLOB_STREAM]");
    delegate.setBlob(parameterIndex, inputStream, length);
  }

  @Override
  public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
    parameters.put(parameterIndex, "[NCLOB_READER]");
    delegate.setNClob(parameterIndex, reader, length);
  }

  @Override
  public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
    parameters.put(parameterIndex, "[SQLXML]");
    delegate.setSQLXML(parameterIndex, xmlObject);
  }

  @Override
  public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
    parameters.put(parameterIndex, "[ASCII_STREAM]");
    delegate.setAsciiStream(parameterIndex, x, length);
  }

  @Override
  public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
    parameters.put(parameterIndex, "[BINARY_STREAM]");
    delegate.setBinaryStream(parameterIndex, x, length);
  }

  @Override
  public void setCharacterStream(int parameterIndex, Reader reader, long length)
      throws SQLException {
    parameters.put(parameterIndex, "[CHAR_STREAM]");
    delegate.setCharacterStream(parameterIndex, reader, length);
  }

  @Override
  public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
    parameters.put(parameterIndex, "[ASCII_STREAM]");
    delegate.setAsciiStream(parameterIndex, x);
  }

  @Override
  public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
    parameters.put(parameterIndex, "[BINARY_STREAM]");
    delegate.setBinaryStream(parameterIndex, x);
  }

  @Override
  public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
    parameters.put(parameterIndex, "[CHAR_STREAM]");
    delegate.setCharacterStream(parameterIndex, reader);
  }

  @Override
  public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
    parameters.put(parameterIndex, "[NCHAR_STREAM]");
    delegate.setNCharacterStream(parameterIndex, value);
  }

  @Override
  public void setClob(int parameterIndex, Reader reader) throws SQLException {
    parameters.put(parameterIndex, "[CLOB_READER]");
    delegate.setClob(parameterIndex, reader);
  }

  @Override
  public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
    parameters.put(parameterIndex, "[BLOB_STREAM]");
    delegate.setBlob(parameterIndex, inputStream);
  }

  @Override
  public void setNClob(int parameterIndex, Reader reader) throws SQLException {
    parameters.put(parameterIndex, "[NCLOB_READER]");
    delegate.setNClob(parameterIndex, reader);
  }
}
