package com.fde.sentinelflow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TenantIsolationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRejectRequestWithInvalidApiKey() throws Exception {
        String jsonPayload = """
            {
                "eventType": "UNAUTHORIZED_TEST",
                "sourceIp": "127.0.0.1",
                "payload": "Test payload"
            }
            """;

        mockMvc.perform(post("/api/v1/events")
                        .header("X-API-Key", "sf_invalid_fake_key_999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isUnauthorized());
    }
}