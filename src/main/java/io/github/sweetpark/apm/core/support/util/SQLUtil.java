package io.github.sweetpark.apm.core.support.util;

import java.time.LocalDateTime;
import java.util.Map;

/** SQL 문장 파싱 및 파라미터 바인딩, 포맷팅을 수행하는 유틸리티 클래스입니다. */
public final class SQLUtil {

  private SQLUtil() {}

  private enum ParsingContext {
    NORMAL,
    SINGLE_QUOTE,
    LINE_COMMENT,
    BLOCK_COMMENT
  }

  /** 연속된 공백 및 줄바꿈을 단일 공백으로 치환하고 길이를 제한합니다. */
  public static String normalizeSql(String sql, int maxLength) {
    if (sql == null) {
      return null;
    }
    String normalized = sql.replaceAll("\\s+", " ").trim();
    return CommonUtil.truncate(normalized, maxLength);
  }

  /** 순서 기반 파라미터(JDBC PreparedStatement)를 기반으로 SQL 내 '?'를 치환합니다. */
  public static String buildSqlWithParams(String sql, Map<Integer, Object> params, int maxLength) {
    if (sql == null) {
      return null;
    }
    if (params == null || params.isEmpty()) {
      return normalizeSql(sql, maxLength);
    }

    String normalized = sql.replaceAll("\\s+", " ").trim();
    StringBuilder sb = new StringBuilder();
    ParsingContext ctx = ParsingContext.NORMAL;
    int paramIdx = 1;

    for (int i = 0; i < normalized.length(); i++) {
      if (sb.length() >= maxLength) {
        sb.append("...(TRUNCATED)");
        return sb.toString();
      }

      char c = normalized.charAt(i);

      if (ctx == ParsingContext.NORMAL) {
        if (c == '\'') {
          ctx = ParsingContext.SINGLE_QUOTE;
        } else if (c == '-' && i + 1 < normalized.length() && normalized.charAt(i + 1) == '-') {
          ctx = ParsingContext.LINE_COMMENT;
        } else if (c == '/' && i + 1 < normalized.length() && normalized.charAt(i + 1) == '*') {
          ctx = ParsingContext.BLOCK_COMMENT;
        }
      } else if (ctx == ParsingContext.SINGLE_QUOTE && c == '\'') {
        ctx = ParsingContext.NORMAL;
      } else if (ctx == ParsingContext.LINE_COMMENT && c == '\n') {
        ctx = ParsingContext.NORMAL;
      } else if (ctx == ParsingContext.BLOCK_COMMENT
          && c == '*'
          && i + 1 < normalized.length()
          && normalized.charAt(i + 1) == '/') {
        ctx = ParsingContext.NORMAL;
      }

      if (c == '?' && ctx == ParsingContext.NORMAL) {
        Object value = params.get(paramIdx++);
        sb.append(formatValue(value));
        continue;
      }

      sb.append(c);
    }

    return sb.toString();
  }

  /** 값을 SQL 리터럴 문자열로 포맷팅합니다. */
  public static String formatValue(Object value) {
    if (value == null) {
      return "NULL";
    }
    if (value instanceof String val) {
      return "'" + val.replace("'", "''") + "'";
    }
    if (value instanceof LocalDateTime || value instanceof java.util.Date) {
      return "'" + value + "'";
    }
    if (value instanceof Number || value instanceof Boolean) {
      return value.toString();
    }
    return "'" + value + "'";
  }
}
