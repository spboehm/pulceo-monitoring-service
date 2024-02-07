package dev.pulceo.pms.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import dev.pulceo.pms.model.metric.NodeLinkMetric;
import dev.pulceo.pms.model.metricrequests.IcmpRttMetricRequest;
import dev.pulceo.pms.model.metricrequests.MetricRequest;
import dev.pulceo.pms.model.metricrequests.TcpBwMetricRequest;
import dev.pulceo.pms.repository.NodeLinkMetricRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.WireMockSpring;
import org.springframework.data.geo.Metric;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class MetricsServiceIntegrationTests {

    @Autowired
    private MetricsService metricsService;

    @Autowired
    private NodeLinkMetricRepository nodeLinkMetricRepository;

    // for some reason `dynamicPort()` is not working properly
    public static WireMockServer wireMockServerForPRM = new WireMockServer(WireMockSpring.options().bindAddress("127.0.0.1").port(7878));
    public static WireMockServer wireMockServerForPNA = new WireMockServer(WireMockSpring.options().bindAddress("127.0.0.1").port(7676));

    @BeforeAll
    static void setupClass() throws InterruptedException {
        Thread.sleep(100);
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
        // TODO: further evaluations
        assertEquals(icmpRttMetricRequest.getLinkUUID(), metricRequest.getRemoteLinkUUID());
        assertEquals(icmpRttMetricRequest.getType(), metricRequest.getType());
        assertEquals(icmpRttMetricRequest.getRecurrence(), metricRequest.getRecurrence());
        assertEquals(icmpRttMetricRequest.isEnabled(), metricRequest.isEnabled());
    }

    @Test
    public void testCreateNewMetricRequestTcpBw () {
        // given
        UUID srcNodeUUID = UUID.fromString("0b1c6697-cb29-4377-bcf8-9fd61ac6c0f3");
        UUID linkUUID = UUID.fromString("ea9084cf-97bb-451e-8220-4bdda327839e");
        TcpBwMetricRequest tcpBwMetricRequest = TcpBwMetricRequest.builder()
                .port(5000)
                .linkUUID(linkUUID)
                .type("tcp-bw")
                .recurrence("15")
                .enabled(true)
                .build();
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
                        .withBodyFile("node/prm-read-node-by-uuid-response.json")));

        // mock start creation of Iperf3-Server
        MetricsServiceIntegrationTests.wireMockServerForPNA.stubFor(post(urlEqualTo("/api/v1/iperf3-servers"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("5000")));

        // mock metric request to prm
        MetricsServiceIntegrationTests.wireMockServerForPNA.stubFor(post(urlEqualTo("/api/v1/links/ea9084cf-97bb-451e-8220-4bcda327839e/metric-requests/tcp-bw-requests"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("metricrequests/create-new-tcp-bw-request-response.json")));

        // when
        MetricRequest metricRequest = this.metricsService.createNewTcpBwMetricRequest(tcpBwMetricRequest);

        // then
        // TODO: further evaluations
        assertEquals(tcpBwMetricRequest.getLinkUUID(), metricRequest.getRemoteLinkUUID());
        assertEquals(tcpBwMetricRequest.getType(), metricRequest.getType());
        assertEquals(tcpBwMetricRequest.getRecurrence(), metricRequest.getRecurrence());
        assertEquals(tcpBwMetricRequest.isEnabled(), metricRequest.isEnabled());
    }

    @Test
    public void testFindLastNodeLinkMetricsByLinkUUIDAndMetricType() {
        // given
        String linkUUID = "08d039b3-e4e3-4258-a9f5-5967c6a5e024";
        NodeLinkMetric firstNodeLinkMetric = NodeLinkMetric.builder()
                .metricType("icmp-rtt")
                .metricRequestUUID("0b1c6697-cb29-4377-bcf8-9fd61ac6c0f3")
                .linkUUID(linkUUID)
                .startTime("2021-08-01T00:00:00Z")
                .endTime("2021-08-01T00:00:15Z")
                .val(1.0)
                .unit("ms")
                .build();
        NodeLinkMetric secondNodeLinkMetric = NodeLinkMetric.builder()
                .metricType("udp-rtt")
                .metricRequestUUID("0b1c6697-cb29-4377-bcf8-9fd61ac6c0f3")
                .linkUUID(linkUUID)
                .startTime("2021-08-01T00:00:15Z")
                .endTime("2021-08-01T00:00:30Z")
                .val(2.0)
                .unit("ms")
                .build();
        this.nodeLinkMetricRepository.saveAll(Arrays.asList(firstNodeLinkMetric, secondNodeLinkMetric));

        // when
        List<NodeLinkMetric> nodeLinkMetrics = this.metricsService.readLastNodeLinkMetricsByLinkUUIDAndMetricType(UUID.fromString(linkUUID));

        // then
        assertEquals(2, nodeLinkMetrics.size());
    }


}
