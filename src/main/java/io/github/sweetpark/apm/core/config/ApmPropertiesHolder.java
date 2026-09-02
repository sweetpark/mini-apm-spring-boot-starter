package io.github.sweetpark.apm.core.config;

/** Spring DI 컨텍스트 외부에서 ApmProperties에 접근할 수 있도록 돕는 정적 홀더 클래스입니다. */
public final class ApmPropertiesHolder {

  private static volatile ApmProperties properties = new ApmProperties();

  private ApmPropertiesHolder() {}

  public static void setProperties(ApmProperties props) {
    properties = (props != null) ? props : new ApmProperties();
  }

  public static ApmProperties getProperties() {
    return properties;
  }
}
