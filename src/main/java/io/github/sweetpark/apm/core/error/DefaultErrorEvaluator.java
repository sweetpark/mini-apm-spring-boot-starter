package io.github.sweetpark.apm.core.error;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.sweetpark.apm.core.config.ApmProperties;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * 기본 에러 판정 구현체입니다.
 *
 * <ol>
 *   <li>예외 객체가 존재하는 경우
 *   <li>HTTP 상태 코드가 임계값(기본 400) 이상인 경우
 *   <li>JSON 응답 본문 내 에러 키(resCode, code 등)의 값이 에러 코드 목록(9999, ERROR 등)에 포함된 경우
 * </ol>
 */
public class DefaultErrorEvaluator implements ErrorEvaluator {

  private final ApmProperties properties;
  private final ObjectMapper objectMapper;

  public DefaultErrorEvaluator(ApmProperties properties) {
    this.properties = properties;
    this.objectMapper = new ObjectMapper();
  }

  @Override
  public boolean isError(int httpStatusCode, String responseBody, Throwable exception) {
    if (exception != null) {
      return true;
    }

    if (httpStatusCode >= properties.getError().getHttpStatusThreshold()) {
      return true;
    }

    return hasErrorCodeInBody(responseBody);
  }

  public boolean hasErrorCodeInBody(String body) {
    if (body == null || body.isBlank()) {
      return false;
    }

    try {
      JsonNode root = objectMapper.readTree(body);
      return containsErrorCode(root);
    } catch (Exception e) {
      return false;
    }
  }

  private boolean containsErrorCode(JsonNode node) {
    if (node == null) {
      return false;
    }

    if (node.isObject()) {
      return containsErrorInObject(node);
    }

    if (node.isArray()) {
      return containsErrorInArray(node);
    }

    return false;
  }

  private boolean containsErrorInObject(JsonNode node) {
    Set<String> keys = properties.getError().getErrorCodeKeys();
    Set<String> errorCodes = properties.getError().getErrorCodes();

    Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> entry = fields.next();
      String key = entry.getKey();
      JsonNode value = entry.getValue();

      if ("success".equalsIgnoreCase(key) && value.isBoolean() && !value.asBoolean()) {
        return true;
      }

      if (keys.contains(key)) {
        String valText = value.asText();
        if (errorCodes.contains(valText)) {
          return true;
        }
      }

      if (containsErrorCode(value)) {
        return true;
      }
    }

    return false;
  }

  private boolean containsErrorInArray(JsonNode node) {
    for (JsonNode child : node) {
      if (containsErrorCode(child)) {
        return true;
      }
    }
    return false;
  }
}
