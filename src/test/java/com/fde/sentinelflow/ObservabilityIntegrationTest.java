package com.fde.sentinelflow;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ObservabilityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void shouldIncrementPublishedEventsMetric() throws Exception {
        String jsonPayload = """
            {
                "eventType": "SECURITY_ALERT",
                "sourceIp": "10.0.0.5",
                "payload": "Unauthorized access attempt detected"
            }
            """;

        // 1. Perform ingestion request
        mockMvc.perform(post("/api/v1/events")
                        .header("X-API-Key", "sf_live_testkey123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isAccepted());

        // 2. Verify that our custom Micrometer counter incremented
        double count = meterRegistry.get("sentinelflow.events.published")
                .tag("event.type", "SECURITY_ALERT")
                .counter()
                .count();

        assertThat(count).isGreaterThan(0.0);
    }
}