package dev.pulceo.pms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import dev.pulceo.pms.dto.metricrequests.CreateNewAbstractMetricRequestDTO;
import dev.pulceo.pms.dto.metricrequests.CreateNewMetricRequestIcmpRttDTO;
import dev.pulceo.pms.dto.metricrequests.MetricRequestDTOType;
import dev.pulceo.pms.dto.metricrequests.ShortMetricResponseDTO;
import dev.pulceo.pms.repository.MetricRequestRepository;
import dev.pulceo.pms.service.MetricsServiceIntegrationTests;
import dev.pulceo.pms.util.SimulatedPulceoNodeAgent;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.WireMockSpring;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_CLASS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
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
    }

    @AfterEach
    void after() {
        MetricsServiceIntegrationTests.wireMockServerForPRM.resetAll();
        MetricsServiceIntegrationTests.wireMockServerForPNA.resetAll();

    }

    @AfterAll
    static void clean() {
        MetricsServiceIntegrationTests.wireMockServerForPRM.shutdown();
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
        MetricsServiceIntegrationTests.wireMockServerForPNA.stubFor(WireMock.post(urlEqualTo("/api/v1/links/" + linkUUID + "/metric-requests/icmp-rtt-requests"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("metricrequests/create-new-icmp-rtt-request-response.json")));


        CreateNewAbstractMetricRequestDTO createNewMetricRequestDTO = CreateNewMetricRequestIcmpRttDTO.builder()
                .metricRequestDTOType(MetricRequestDTOType.ICMP_RTT)
                .linkUUID(linkUUID)
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
}