package dev.pulceo.pms.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import dev.pulceo.pms.api.dto.orchestration.OrchestrationContextFromPsmDTO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.WireMockSpring;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(properties = {"webclient.scheme=http"})
public class PsmApiTests {

    @Autowired
    private PsmApi psmApi;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private WireMockServer wireMockServerForPSM;

    @BeforeAll
    void setupClass() throws InterruptedException {
        Thread.sleep(1000);
        wireMockServerForPSM = new WireMockServer(WireMockSpring.options().bindAddress("127.0.0.1").port(7979));
        wireMockServerForPSM.start();
    }

    @BeforeEach
    void setup() {
        wireMockServerForPSM.resetRequests();
    }

    @AfterAll
    void clean() {
        wireMockServerForPSM.shutdown();
    }

    @Test
    public void testRetrieveOrchestrationContext() {
        // given
        this.wireMockServerForPSM.stubFor(get(urlEqualTo("/api/v1/orchestration-context"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("api/psmapi-get-orchestration-context.json")));
        // when
        OrchestrationContextFromPsmDTO orchestrationContextFromPsmDTO = this.psmApi.getOrchestrationContext();

        // then
        assertNotNull(orchestrationContextFromPsmDTO);
        assertEquals("535aab93-b6e6-4e7c-b0a2-5cfca310e68d", orchestrationContextFromPsmDTO.getUuid());
        assertEquals("default", orchestrationContextFromPsmDTO.getName());
    }

    @Test
    public void testRetrieveOrchestrationConextWhenAPIRequestFails() {
        // given
        this.wireMockServerForPSM.stubFor(get(urlEqualTo("/api/v1/orchestration-context"))
                .willReturn(aResponse()
                        .withStatus(500)));
        // when
        OrchestrationContextFromPsmDTO orchestrationContextFromPsmDTO = this.psmApi.getOrchestrationContext();

        // then
        assertNotNull(orchestrationContextFromPsmDTO);
        assertEquals("00000000-0000-0000-0000-000000000000", orchestrationContextFromPsmDTO.getUuid());
        assertEquals("default", orchestrationContextFromPsmDTO.getName());
    }

}
