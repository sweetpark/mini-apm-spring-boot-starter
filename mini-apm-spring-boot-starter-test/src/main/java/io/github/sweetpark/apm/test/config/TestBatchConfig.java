package io.github.sweetpark.apm.test.config;

import io.github.sweetpark.apm.support.batch.LoggingBatchListener;
import io.github.sweetpark.apm.support.batch.LoggingTaskDecorator;
import io.github.sweetpark.apm.test.mapper.TestMapper;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class TestBatchConfig {

    private final LoggingBatchListener loggingBatchListener;
    private final TestMapper testMapper;

    public TestBatchConfig(LoggingBatchListener loggingBatchListener, TestMapper testMapper) {
        this.loggingBatchListener = loggingBatchListener;
        this.testMapper = testMapper;
    }

    @Bean
    public Job testBatchJob(JobRepository jobRepository, Step testBatchStep) {
        return new JobBuilder("testBatchJob", jobRepository)
                .listener(loggingBatchListener)
                .start(testBatchStep)
                .build();
    }

    @Bean
    public Step testBatchStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
        executor.setTaskDecorator(new LoggingTaskDecorator());

        return new StepBuilder("testBatchStep", jobRepository)
                .listener(loggingBatchListener)
                .tasklet((contribution, chunkContext) -> {
                    testMapper.selectOne();
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .taskExecutor(executor)
                .build();
    }
}