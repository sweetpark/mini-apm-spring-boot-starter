package io.github.sweetpark.apm.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(OutputCaptureExtension.class)
@DirtiesContext
class BatchApmTest {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job testBatchJob;

    @Test
    @DisplayName("Spring Batch 실행 및 [BATCH] Job/Step 요약 로그 검증")
    void testBatchLogging(CapturedOutput output) throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(testBatchJob, params);

        assertThat(output.getOut()).contains("[BATCH]");
        assertThat(output.getOut()).contains("job_name=testBatchJob");
        assertThat(output.getOut()).contains("step_name=testBatchStep");
    }
}