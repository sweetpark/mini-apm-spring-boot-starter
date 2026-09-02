package io.github.sweetpark.apm.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(OutputCaptureExtension.class)
@DirtiesContext
class ServletApmTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("기본 HTTP API 호출 및 [HTTP] 요약 로그 검증")
    void testBasicApiLogging(CapturedOutput output) throws Exception {
        ResponseEntity<String> res = restTemplate.getForEntity("/test", String.class);
        assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
        Thread.sleep(150);
        assertThat(output.getOut()).contains("[HTTP]").contains("uri=/test").contains("status=200");
    }

    @Test
    @DisplayName("커스텀 X-Trace-Id 및 W3C traceparent 전파 검증")
    void testTraceHeaderPropagation(CapturedOutput output) throws Exception {
        String customTraceId = "1234567890abcdef1234567890abcdef";
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Trace-Id", customTraceId);
        headers.set("X-Interface-Id", "IF-TEST-001");

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange("/test", HttpMethod.GET, requestEntity, String.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        Thread.sleep(150);
        assertThat(output.getOut()).contains("trace_id=" + customTraceId);
        assertThat(output.getOut()).contains("interface_id=IF-TEST-001");
    }

    @Test
    @DisplayName("에러 API 호출 시 에러 코드(9999) 판정 및 [EXCEPTION] 로그 검증")
    void testErrorApiLogging(CapturedOutput output) throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity("/error-api", String.class);
        assertThat(response.getBody()).contains("9999");
        Thread.sleep(150);
        assertThat(output.getOut()).contains("[EXCEPTION]").contains("9999");
    }

    @Test
    @DisplayName("바이너리 파일 다운로드 시 메모리 안전성 및 바이너리 표시 검증")
    void testBinaryDownload(CapturedOutput output) {
        ResponseEntity<byte[]> response = restTemplate.getForEntity("/download", byte[].class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
    }
}