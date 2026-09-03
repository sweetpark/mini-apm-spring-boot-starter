package io.github.sweetpark.apm.support.servlet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.sweetpark.apm.core.config.ApmProperties;
import io.github.sweetpark.apm.core.error.DefaultErrorEvaluator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class LoggingFilterTest {

  private LoggingFilter newFilter(ApmProperties properties) {
    return new LoggingFilter(properties, new DefaultErrorEvaluator(properties));
  }

  @Test
  @DisplayName("정상 요청은 응답 상태와 본문을 그대로 전달한다")
  void testNormalRequestPassesThrough() throws ServletException, IOException {
    LoggingFilter filter = newFilter(new ApmProperties());
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
    request.setContentType("application/json");
    MockHttpServletResponse response = new MockHttpServletResponse();

    FilterChain chain =
        (req, res) -> {
          res.setContentType("application/json");
          res.getOutputStream().write("{\"ok\":true}".getBytes(StandardCharsets.UTF_8));
        };

    filter.doFilterInternal(request, response, chain);

    assertThat(response.getContentAsString()).isEqualTo("{\"ok\":true}");
  }

  @Test
  @DisplayName("W3C traceparent 헤더에서 traceId를 추출한다")
  void testTraceparentHeaderParsing() throws ServletException, IOException {
    LoggingFilter filter = newFilter(new ApmProperties());
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
    request.addHeader("traceparent", "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");
    MockHttpServletResponse response = new MockHttpServletResponse();

    FilterChain chain = (req, res) -> {};

    filter.doFilterInternal(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  @DisplayName("커스텀 트레이스 헤더가 있으면 이를 traceId로 사용한다")
  void testCustomTraceHeader() throws ServletException, IOException {
    ApmProperties props = new ApmProperties();
    LoggingFilter filter = newFilter(props);
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
    request.addHeader("X-Trace-Id", "my-trace-id");
    MockHttpServletResponse response = new MockHttpServletResponse();

    FilterChain chain = (req, res) -> {};

    filter.doFilterInternal(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  @DisplayName("X-Debug-Trace 헤더로 강제 추적을 활성화할 수 있다")
  void testForceTraceHeader() throws ServletException, IOException {
    LoggingFilter filter = newFilter(new ApmProperties());
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
    request.addHeader("X-Debug-Trace", "true");
    MockHttpServletResponse response = new MockHttpServletResponse();

    FilterChain chain = (req, res) -> {};

    filter.doFilterInternal(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  @DisplayName("trace 쿼리 파라미터로도 강제 추적을 활성화할 수 있다")
  void testForceTraceQueryParam() throws ServletException, IOException {
    LoggingFilter filter = newFilter(new ApmProperties());
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
    request.setParameter("trace", "true");
    MockHttpServletResponse response = new MockHttpServletResponse();

    FilterChain chain = (req, res) -> {};

    filter.doFilterInternal(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  @DisplayName("multipart 요청은 바디 캐싱 없이 원본 스트림 그대로 전달된다")
  void testBinaryMultipartRequestSkipsWrapping() throws ServletException, IOException {
    LoggingFilter filter = newFilter(new ApmProperties());
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/upload");
    request.setContentType("multipart/form-data; boundary=X");
    MockHttpServletResponse response = new MockHttpServletResponse();

    FilterChain chain =
        (req, res) -> {
          assertThat(req).isNotInstanceOf(RequestWrapper.class);
        };

    filter.doFilterInternal(request, response, chain);
  }

  @Test
  @DisplayName("Range 헤더가 있는 요청도 바이너리로 취급되어 래핑되지 않는다")
  void testRangeHeaderRequestSkipsWrapping() throws ServletException, IOException {
    LoggingFilter filter = newFilter(new ApmProperties());
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/files/1");
    request.addHeader("Range", "bytes=0-100");
    MockHttpServletResponse response = new MockHttpServletResponse();

    FilterChain chain = (req, res) -> assertThat(req).isNotInstanceOf(RequestWrapper.class);

    filter.doFilterInternal(request, response, chain);
  }

  @Test
  @DisplayName("application/octet-stream Accept 헤더 요청도 바이너리로 취급된다")
  void testAcceptOctetStreamSkipsWrapping() throws ServletException, IOException {
    LoggingFilter filter = newFilter(new ApmProperties());
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/files/1");
    request.addHeader("Accept", "application/octet-stream");
    MockHttpServletResponse response = new MockHttpServletResponse();

    FilterChain chain = (req, res) -> assertThat(req).isNotInstanceOf(RequestWrapper.class);

    filter.doFilterInternal(request, response, chain);
  }

  @Test
  @DisplayName("Content-Disposition attachment 응답은 바이너리 응답으로 처리된다")
  void testAttachmentResponseTreatedAsBinary() throws ServletException, IOException {
    LoggingFilter filter = newFilter(new ApmProperties());
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/files/1/download");
    MockHttpServletResponse response = new MockHttpServletResponse();

    FilterChain chain =
        (req, res) -> {
          var httpRes = (jakarta.servlet.http.HttpServletResponse) res;
          httpRes.setHeader("Content-Disposition", "attachment; filename=report.pdf");
          httpRes.setContentType("application/pdf");
          httpRes.getOutputStream().write(new byte[] {1, 2, 3});
        };

    filter.doFilterInternal(request, response, chain);

    assertThat(response.getContentAsByteArray()).containsExactly(1, 2, 3);
  }

  @Test
  @DisplayName("체인에서 예외가 발생하면 예외를 그대로 전파한다 (Fail-safe 로깅과 무관)")
  void testChainExceptionPropagates() {
    LoggingFilter filter = newFilter(new ApmProperties());
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/boom");
    MockHttpServletResponse response = new MockHttpServletResponse();

    FilterChain chain =
        (req, res) -> {
          throw new IllegalStateException("downstream failure");
        };

    assertThrows(
        IllegalStateException.class, () -> filter.doFilterInternal(request, response, chain));
  }

  @Test
  @DisplayName("TRACE 레벨에서는 인터페이스 헤더 및 IFID로 interfaceId를 조회한다")
  void testInterfaceIdHeaderAndIfidFallback() throws ServletException, IOException {
    ApmProperties props = new ApmProperties();
    props.getTrace().setLevel(io.github.sweetpark.apm.core.enums.TraceLevel.TRACE);
    LoggingFilter filter = newFilter(props);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
    request.addHeader("IFID", "IF-100");
    MockHttpServletRequest requestBody = request;
    requestBody.setContent("{\"q\":1}".getBytes(StandardCharsets.UTF_8));
    requestBody.setContentType("application/json");
    MockHttpServletResponse response = new MockHttpServletResponse();

    FilterChain chain =
        (req, res) -> {
          res.setContentType("application/json");
          res.getOutputStream().write("{\"a\":1}".getBytes(StandardCharsets.UTF_8));
        };

    filter.doFilterInternal(request, response, chain);

    assertThat(response.getContentAsString()).isEqualTo("{\"a\":1}");
  }
}
