package io.github.sweetpark.apm.core.enums;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/** 로그 목적 및 카테고리를 구분하기 위한 SLF4J Marker 정의 열거형입니다. */
public enum LogMarker {
  HTTP("HTTP"),
  HTTP_DETAIL("HTTP_DETAIL"),
  NETTY("NETTY"),
  NETTY_DETAIL("NETTY_DETAIL"),
  BATCH("BATCH"),
  SQL("SQL"),
  SLOW_SQL("SLOW_SQL"),
  SQL_SLOW("SLOW_SQL"),
  SQL_EXCEPTION("SQL_EXCEPTION"),
  N1_QUERY("N1_QUERY"),
  EXCEPTION("EXCEPTION"),
  ERROR_BIZ("ERROR_BIZ"),
  ERROR_DB("ERROR_DB"),
  ERROR_EXTERNAL("ERROR_EXTERNAL"),
  ERROR_SYSTEM("ERROR_SYSTEM");

  private final String name;
  private final Marker marker;

  LogMarker(String name) {
    this.name = name;
    this.marker = MarkerFactory.getMarker(name);
  }

  public String getName() {
    return name;
  }

  public Marker marker() {
    return marker;
  }
}
