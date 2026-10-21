package com.fde.sentinelflow;

import com.fde.sentinelflow.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.kafka.consumer.group-id=test-group-${random.value}")
class IngestionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditLogRepository auditLogRepository;

    // Dynamically point Spring to your already-running local Docker Redis container
    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> 6379);
    }

    @Test
    void shouldAcceptValidEventPayload() throws Exception {
        String jsonPayload = """
            {
                "eventType": "USER_LOGIN",
                "sourceIp": "192.168.1.10",
                "payload": "Successful login attempt"
            }
            """;

        mockMvc.perform(post("/api/v1/events")
                        .header("X-API-Key", "sf_live_testkey123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isAccepted());
// 2. Wait up to 5 seconds for the background Kafka consumer to write to PostgreSQL
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            assertThat(auditLogRepository.count()).isGreaterThan(0);
        });
    }

    @Test
    void shouldRejectRequestWhenApiKeyIsMissing() throws Exception {
        String jsonPayload = """
            {
                "eventType": "USER_LOGIN",
                "sourceIp": "192.168.1.10"
            }
            """;

        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest());
    }
}