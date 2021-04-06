package com.tietoevry.quarkus.resteasy.problem.postprocessing;

import static com.tietoevry.quarkus.resteasy.problem.postprocessing.ProblemContextMother.simpleContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tietoevry.quarkus.resteasy.problem.javax.Violation;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;

class ProblemLoggerTest {

    Logger logger = mock(Logger.class);
    ProblemLogger processor = new ProblemLogger(logger);

    @BeforeEach
    void init() {
        when(logger.isErrorEnabled()).thenReturn(true);
        when(logger.isInfoEnabled()).thenReturn(true);
    }

    @Test
    void shouldPrintOnlyNotNullFields() {
        Problem problem = Problem.builder()
                .withTitle("your fault")
                .withStatus(Status.BAD_REQUEST)
                .build();

        processor.apply(problem, simpleContext());

        assertThat(capturedInfoMessage()).isEqualTo("status=400, title=\"your fault\"");
    }

    @Test
    void shouldPrintCustomParameters() {
        Problem problem = Problem.builder()
                .withTitle("your fault")
                .withStatus(Status.BAD_REQUEST)
                .with("custom-field", "123")
                .with("violations", Collections.singletonList(new Violation("too small", "key")))
                .build();

        processor.apply(problem, simpleContext());

        assertThat(capturedInfoMessage())
                .contains(
                        "custom-field=\"123\"",
                        "violations=[{\"error\":\"too small\",\"field\":\"key\"}]");
    }

    @Test
    void shouldPrintStackTraceFor500s() {
        Problem problem = Problem.builder()
                .withTitle("my fault")
                .withStatus(Status.INTERNAL_SERVER_ERROR)
                .build();
        RuntimeException cause = new RuntimeException("hey");

        processor.apply(problem, ProblemContextMother.withCause(cause));

        assertThat(capturedErrorMessage()).isEqualTo("status=500, title=\"my fault\"");
        assertThat(capturedErrorException()).isEqualTo(cause);
    }

    private String capturedInfoMessage() {
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger).info(messageCaptor.capture());
        return messageCaptor.getValue();
    }

    private String capturedErrorMessage() {
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger).error(messageCaptor.capture(), ArgumentCaptor.forClass(RuntimeException.class).capture());
        return messageCaptor.getValue();
    }

    private RuntimeException capturedErrorException() {
        ArgumentCaptor<RuntimeException> errorCaptor = ArgumentCaptor.forClass(RuntimeException.class);
        verify(logger).error(ArgumentCaptor.forClass(String.class).capture(), errorCaptor.capture());
        return errorCaptor.getValue();
    }
}