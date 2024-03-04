package dev.pulceo.pms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import dev.pulceo.pms.dto.metricrequests.*;
import dev.pulceo.pms.repository.MetricRequestRepository;
import dev.pulceo.pms.service.MetricsServiceIntegrationTests;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = { "webclient.scheme=http"})
@AutoConfigureMockMvc
public class MetricsControllerTests {

    @Autowired
    private MetricsController metricsController;

    @Autowired
    private MetricRequestRepository metricRequestRepository;

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void before() {
        this.metricRequestRepository.deleteAll();
    }

    @BeforeAll
    static void setupClass() {
        MetricsServiceIntegrationTests.wireMockServerForPRM.start();
        MetricsServiceIntegrationTests.wireMockServerForPNA.start();
        MetricsServiceIntegrationTests.wireMockServerForDestPNA.start();
    }

    @AfterEach
    void after() {
        MetricsServiceIntegrationTests.wireMockServerForPRM.resetAll();
        MetricsServiceIntegrationTests.wireMockServerForPNA.resetAll();
        MetricsServiceIntegrationTests.wireMockServerForDestPNA.resetAll();
    }

    @AfterAll
    static void clean() {
        MetricsServiceIntegrationTests.wireMockServerForPRM.shutdown();
        MetricsServiceIntegrationTests.wireMockServerForPNA.shutdown();
        MetricsServiceIntegrationTests.wireMockServerForPNA.shutdown();
    }

    @Test
    public void testCreateMetricRequestIcmpRtt() throws Exception {
        // mock link request to pan
        UUID srcNodeUUID = UUID.fromString("0b1c6697-cb29-4377-bcf8-9fd61ac6c0f3");
        UUID linkUUID = UUID.fromString("ea9084cf-97bb-451e-8220-4bdda327839e");

        // mock link request to pan
        MetricsServiceIntegrationTests.wireMockServerForPRM.stubFor(get(urlEqualTo("/api/v1/links/" + linkUUID))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("link/prm-read-link-by-uuid-response.json")));

        // mock link request to pna => done in SimulatedPnaAgent
        MetricsServiceIntegrationTests.wireMockServerForPRM.stubFor(get(urlEqualTo("/api/v1/nodes/" + srcNodeUUID))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("node/prm-read-node-by-uuid-response.json")));

        // mock metric request to prm
        MetricsServiceIntegrationTests.wireMockServerForPNA.stubFor(WireMock.post(urlEqualTo("/api/v1/links/ea9084cf-97bb-451e-8220-4bcda327839e/metric-requests/icmp-rtt-requests"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("metricrequests/create-new-icmp-rtt-request-response.json")));

        // mock metric request to prm (pna-token)
        MetricsServiceIntegrationTests.wireMockServerForPRM.stubFor(WireMock.get(urlEqualTo("/api/v1/nodes/0b1c6697-cb29-4377-bcf8-9fd61ac6c0f3/pna-token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("")));

        // mock metric request to prm (pna-token)
        MetricsServiceIntegrationTests.wireMockServerForPRM.stubFor(WireMock.get(urlEqualTo("/api/v1/nodes/d6421210-6759-4973-bad3-7f47bcb133c1/pna-token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("")));


        CreateNewAbstractMetricRequestDTO createNewMetricRequestDTO = CreateNewMetricRequestIcmpRttDTO.builder()
                .linkId(linkUUID.toString())
                .type("icmp-rtt")
                .recurrence("15")
                .build();
        String createNewMetricRequestDTOAsJson = this.objectMapper.writeValueAsString(createNewMetricRequestDTO);

        // when and then
        MvcResult mvcResult = this.mockMvc.perform(post("/api/v1/metric-requests")
                    .contentType("application/json")
                    .accept("application/json")
                    .content(createNewMetricRequestDTOAsJson))
                    .andExpect(status().isCreated())
                    .andReturn();
        ShortMetricResponseDTO shortMetricResponseDTO = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ShortMetricResponseDTO.class);
        assert(linkUUID.equals(shortMetricResponseDTO.getLinkUUID()));
        assert("icmp-rtt".equals(shortMetricResponseDTO.getType()));
        assert("15".equals(shortMetricResponseDTO.getRecurrence()));
        assertTrue(shortMetricResponseDTO.isEnabled());
    }

    @Test
    public void testCreateMetricRequestTcpBw() throws Exception {
        // mock link request to pan
        UUID srcNodeUUID = UUID.fromString("0b1c6697-cb29-4377-bcf8-9fd61ac6c0f3");
        UUID linkUUID = UUID.fromString("ea9084cf-97bb-451e-8220-4bdda327839e");
        UUID destNodeUUID = UUID.fromString("d6421210-6759-4973-bad3-7f47bcb133c1");
        // mock link request to pna
        MetricsServiceIntegrationTests.wireMockServerForPRM.stubFor(get(urlEqualTo("/api/v1/links/" + linkUUID))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("link/prm-read-link-by-uuid-response.json")));

        // mock link request to pna => done in SimulatedPnaAgent
        MetricsServiceIntegrationTests.wireMockServerForPRM.stubFor(get(urlEqualTo("/api/v1/nodes/" + srcNodeUUID))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("node/prm-read-node-by-uuid-1-response.json")));

        MetricsServiceIntegrationTests.wireMockServerForPRM.stubFor(get(urlEqualTo("/api/v1/nodes/" + destNodeUUID))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("node/prm-read-node-by-uuid-2-response.json")));

        // mock start creation of Iperf3-Server
        MetricsServiceIntegrationTests.wireMockServerForDestPNA.stubFor(WireMock.post(urlEqualTo("/api/v1/iperf3-servers"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("5000")));

        // mock metric request to prm
        MetricsServiceIntegrationTests.wireMockServerForPNA.stubFor(WireMock.post(urlEqualTo("/api/v1/links/ea9084cf-97bb-451e-8220-4bcda327839e/metric-requests/tcp-bw-requests"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("metricrequests/create-new-tcp-bw-request-response.json")));

        // mock metric request to prm (pna-token)
        MetricsServiceIntegrationTests.wireMockServerForPRM.stubFor(WireMock.get(urlEqualTo("/api/v1/nodes/0b1c6697-cb29-4377-bcf8-9fd61ac6c0f3/pna-token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("")));

        // mock metric request to prm (pna-token)
        MetricsServiceIntegrationTests.wireMockServerForPRM.stubFor(WireMock.get(urlEqualTo("/api/v1/nodes/d6421210-6759-4973-bad3-7f47bcb133c1/pna-token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("")));

        CreateNewMetricRequestTcpBwDTO createNewMetricRequestTcpBwDTO = CreateNewMetricRequestTcpBwDTO.builder()
                .linkUUID(linkUUID)
                .type("tcp-bw")
                .recurrence("15")
                .enabled(true)
                .build();
        String createNewMetricRequestTcpBwDTOAsJson = this.objectMapper.writeValueAsString(createNewMetricRequestTcpBwDTO);
        MvcResult mvcResult = this.mockMvc.perform(post("/api/v1/metric-requests")
                .contentType("application/json")
                .accept("application/json")
                .content(createNewMetricRequestTcpBwDTOAsJson))
                .andExpect(status().isCreated())
                .andReturn();
        ShortMetricResponseDTO shortMetricResponseDTO = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ShortMetricResponseDTO.class);
        assert(linkUUID.equals(shortMetricResponseDTO.getLinkUUID()));
        assert("tcp-bw".equals(shortMetricResponseDTO.getType()));
        assert("15".equals(shortMetricResponseDTO.getRecurrence()));
        assertTrue(shortMetricResponseDTO.isEnabled());
    }


}
