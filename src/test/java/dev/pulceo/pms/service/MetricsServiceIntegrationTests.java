package dev.pulceo.pms.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import dev.pulceo.pms.model.metricrequests.IcmpRttMetricRequest;
import dev.pulceo.pms.model.metricrequests.MetricRequest;
import dev.pulceo.pms.util.SimulatedPulceoNodeAgent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.WireMockSpring;
import org.springframework.test.annotation.DirtiesContext;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_CLASS;

@SpringBootTest
public class MetricsServiceIntegrationTests {

    @Autowired
    private MetricsService metricsService;

    // for some reason `dynamicPort()` is not working properly
    public static WireMockServer wireMockServerForPRM = new WireMockServer(WireMockSpring.options().bindAddress("127.0.0.1").port(7878));
    public static WireMockServer wireMockServerForPNA = new WireMockServer(WireMockSpring.options().bindAddress("127.0.0.1").port(7676));

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
        MetricsServiceIntegrationTests.wireMockServerForPRM.shutdown();
    }

    @Test
    public void testCreateNewMetricRequestIcmpRTT() {
        // given
        UUID srcNodeUUID = UUID.fromString("0b1c6697-cb29-4377-bcf8-9fd61ac6c0f3");
        UUID linkUUID = UUID.fromString("ea9084cf-97bb-451e-8220-4bdda327839e");
        IcmpRttMetricRequest icmpRttMetricRequest = IcmpRttMetricRequest.builder()
                .linkUUID(linkUUID)
                .type("icmp-rtt")
                .recurrence("15")
                .enabled(true)
                .build();
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
        MetricsServiceIntegrationTests.wireMockServerForPNA.stubFor(post(urlEqualTo("/api/v1/links/ea9084cf-97bb-451e-8220-4bcda327839e/metric-requests/icmp-rtt-requests"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("metricrequests/create-new-icmp-rtt-request-response.json")));

        // when
        MetricRequest metricRequest = this.metricsService.createNewIcmpRttMetricRequest(icmpRttMetricRequest);

        // then
        assertEquals(icmpRttMetricRequest.getLinkUUID(), metricRequest.getLinkUUID());
        assertEquals(icmpRttMetricRequest.getType(), metricRequest.getType());
        assertEquals(icmpRttMetricRequest.getRecurrence(), metricRequest.getRecurrence());
        assertEquals(icmpRttMetricRequest.isEnabled(), metricRequest.isEnabled());
    }


}
