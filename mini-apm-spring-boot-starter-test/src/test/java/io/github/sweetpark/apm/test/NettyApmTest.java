package io.github.sweetpark.apm.test;

import io.github.sweetpark.apm.test.config.TestNettyConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.annotation.DirtiesContext;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(OutputCaptureExtension.class)
@DirtiesContext
class NettyApmTest {

    @Autowired
    private TestNettyConfig nettyConfig;

    @Test
    @DisplayName("Netty TCP 클라이언트 요청 및 [NETTY] 요약 로그 검증")
    void testNettyLogging(CapturedOutput output) throws Exception {
        int port = nettyConfig.getPort();

        try (Socket socket = new Socket("127.0.0.1", port);
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            writer.println("Hello Netty");
            String line = reader.readLine();

            assertThat(line).isEqualTo("Echo: Hello Netty");
        }

        // 약간의 비동기 로깅 지연 감안
        Thread.sleep(300);

        assertThat(output.getOut()).contains("[NETTY]");
        assertThat(output.getOut()).contains("interface_id=NETTY_TCP");
    }
}