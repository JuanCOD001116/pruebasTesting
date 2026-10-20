package com.example.taskmanager.acceptance;

import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("acceptance")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@Import(FixedClockTestConfig.class)
class AcceptanceTestRunner {

    @LocalServerPort
    private int port;

    @Test
    @DisplayName("Dado la API en ejecución, cuando se ejecutan los escenarios de aceptación, entonces todos pasan")
    void allAcceptanceScenariosPass() {
        // Arrange
        // -Dkarate.options (e.g. "--tags @smoke") is read by Karate itself and overrides these tags
        Runner.Builder<?> builder = Runner.path("classpath:com/example/taskmanager/acceptance")
                .configDir("classpath:com/example/taskmanager/acceptance")
                .tags("~@ignore")
                .systemProperty("karate.port", String.valueOf(port))
                .systemProperty("karate.today", FixedClockTestConfig.TODAY.toString())
                .outputHtmlReport(true)
                .outputJunitXml(true)
                .outputCucumberJson(true)
                .reportDir("build/karate-reports");

        // Act
        Results results = builder.parallel(1);

        // Assert
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
}
