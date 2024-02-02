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

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class MetricsServiceIntegrationTests {

    @Autowired
    private MetricsService metricsService;

    // for some reason `dynamicPort()` is not working properly
    public static WireMockServer wireMockServerForPRM = new WireMockServer(WireMockSpring.options().bindAddress("127.0.0.1").port(7878));

    @BeforeAll
    static void setupClass() {
        SimulatedPulceoNodeAgent.createAgents(2);
        wireMockServerForPRM.start();
    }

    @AfterEach
    void after() {
        wireMockServerForPRM.resetAll();
        SimulatedPulceoNodeAgent.resetAgents();
    }

    @AfterAll
    static void clean() {
        wireMockServerForPRM.shutdown();
        SimulatedPulceoNodeAgent.stopAgents();
    }

    @Test
    public void testCreateNewMetricRequestIcmpRTT() {
        // given
        UUID linkUUID = UUID.fromString("ea9084cf-97bb-451e-8220-4bdda327839e");
        IcmpRttMetricRequest icmpRttMetricRequest = IcmpRttMetricRequest.builder()
                .linkUUID(linkUUID)
                .type("icmp-rtt")
                .recurrence("15")
                .enabled(true)
                .build();
        // mock link request to pan
        wireMockServerForPRM.stubFor(get(urlEqualTo("/api/v1/links/" + linkUUID))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("link/prm-read-link-by-uuid-response.json")));

        // mock link request to pna => done in SimulatedPnaAgent

        // when
        MetricRequest metricRequest = this.metricsService.createNewIcmpRttMetricRequest(icmpRttMetricRequest);

        // then
        // TODO: job uuid
        assertEquals(icmpRttMetricRequest.getLinkUUID(), metricRequest.getLinkUUID());
        assertEquals(icmpRttMetricRequest.getType(), metricRequest.getType());
        assertEquals(icmpRttMetricRequest.getRecurrence(), metricRequest.getRecurrence());
        assertEquals(icmpRttMetricRequest.isEnabled(), metricRequest.isEnabled());
    }


}
