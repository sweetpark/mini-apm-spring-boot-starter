package io.github.sweetpark.apm.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(OutputCaptureExtension.class)
@DirtiesContext
class MyBatisApmTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("MyBatis 쿼리 실행 시 완성형 SQL 및 파라미터 바인딩 검증")
    void testMyBatisSqlTracing(CapturedOutput output) throws Exception {
        restTemplate.getForEntity("/test-param?value=hello_apm", String.class);
        Thread.sleep(150);

        assertThat(output.getOut()).contains("[SQL]");
        assertThat(output.getOut()).contains("hello_apm");
    }
}