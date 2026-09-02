package io.github.sweetpark.apm.support.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Marker;

/** 특정 LogMarker가 부착된 로그만을 선별하여 출력할 수 있도록 지원하는 Logback 필터입니다. */
public class MetricMarkerFilter extends Filter<ILoggingEvent> {

  private final Set<String> markersToMatch = new HashSet<>();

  public void setMarkers(String markers) {
    if (markers != null && !markers.isBlank()) {
      for (String m : markers.split(",")) {
        markersToMatch.add(m.trim());
      }
    }
  }

  @Override
  public FilterReply decide(ILoggingEvent event) {
    if (!isStarted()) {
      return FilterReply.NEUTRAL;
    }

    List<Marker> markers = event.getMarkerList();
    if (markers != null) {
      for (Marker m : markers) {
        if (markersToMatch.contains(m.getName())) {
          return FilterReply.ACCEPT;
        }
      }
    }

    return FilterReply.DENY;
  }
}
