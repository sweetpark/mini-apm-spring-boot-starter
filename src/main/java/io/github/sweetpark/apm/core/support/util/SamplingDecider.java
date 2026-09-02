package io.github.sweetpark.apm.core.support.util;

import io.github.sweetpark.apm.core.config.ApmProperties;
import java.util.concurrent.ThreadLocalRandom;

/** 확률 기반 샘플링(Sampling) 여부를 결정하는 유틸리티 클래스입니다. */
public final class SamplingDecider {

  private SamplingDecider() {}

  public static boolean shouldForceTrace(ApmProperties properties, boolean forceTraceFromHeader) {
    if (forceTraceFromHeader) {
      return true;
    }

    if (properties == null || properties.getCapture() == null) {
      return false;
    }

    if (properties.getCapture().getBody() == ApmProperties.CaptureMode.SAMPLE
        || properties.getCapture().getSql() == ApmProperties.CaptureMode.SAMPLE) {
      double rate = properties.getCapture().getSampleRate();
      return ThreadLocalRandom.current().nextDouble() < rate;
    }

    return false;
  }
}
