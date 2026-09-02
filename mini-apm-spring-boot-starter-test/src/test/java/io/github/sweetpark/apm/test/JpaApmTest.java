package io.github.sweetpark.apm.test;

import io.github.sweetpark.apm.test.repository.UserRepository;
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
class JpaApmTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Spring Data JPA 엔티티 저장 및 조회 시 DataSource Proxy SQL 추적 검증")
    void testJpaSqlInterception(CapturedOutput output) throws Exception {
        restTemplate.getForEntity("/jpa/users?name=sweetpark_user", String.class);
        Thread.sleep(150);

        // DataSource Proxy에 의해 가로채진 SQL 로그 검증
        assertThat(output.getOut()).contains("[SQL]");
        assertThat(output.getOut()).containsIgnoringCase("users");
        assertThat(output.getOut()).contains("sweetpark_user");
    }
}