package dev.pulceo.pms.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import dev.pulceo.pms.exception.MetricsServiceException;
import dev.pulceo.pms.model.metric.NodeLinkMetric;
import dev.pulceo.pms.model.metricrequests.*;
import dev.pulceo.pms.repository.NodeLinkMetricRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.WireMockSpring;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = { "webclient.scheme=http"})
public class MetricsServiceIntegrationTests {

    @Autowired
    private MetricsService metricsService;

    @Autowired
    private NodeLinkMetricRepository nodeLinkMetricRepository;

    // for some reason `dynamicPort()` is not working properly
    public static WireMockServer wireMockServerForPRM = new WireMockServer(WireMockSpring.options().bindAddress("127.0.0.1").port(7878));
    public static WireMockServer wireMockServerForPNA = new WireMockServer(WireMockSpring.options().bindAddress("127.0.0.1").port(7676));
    public static WireMockServer wireMockServerForDestPNA = new WireMockServer(WireMockSpring.options().bindAddress("127.0.0.2").port(7676));

    @BeforeAll
    static void setupClass() throws InterruptedException {
        Thread.sleep(100);
        MetricsServiceIntegrationTests.wireMockServerForPRM.start();
        MetricsServiceIntegrationTests.wireMockServerForPNA.start();
        MetricsServiceIntegrationTests.wireMockServerForDestPNA.start();
    }

    @AfterEach
    void after() {
//        MetricsServiceIntegrationTests.wireMockServerForPRM.resetAll();
//        MetricsServiceIntegrationTests.wireMockServerForPNA.resetAll();
//        MetricsServiceIntegrationTests.wireMockServerForDestPNA.resetAll();
    }

    @AfterAll
    static void clean() {
        MetricsServiceIntegrationTests.wireMockServerForPRM.shutdown();
        MetricsServiceIntegrationTests.wireMockServerForPNA.shutdown();
        MetricsServiceIntegrationTests.wireMockServerForDestPNA.shutdown();
    }

    @Test
    public void testCreateNewMetricRequestIcmpRTT() throws InterruptedException {
        // given
        UUID srcNodeUUID = UUID.fromString("0b1c6697-cb29-4377-bcf8-9fd61ac6c0f3");
        UUID linkUUID = UUID.fromString("ea9084cf-97bb-451e-8220-4bdda327839e");
        IcmpRttMetricRequest icmpRttMetricRequest = IcmpRttMetricRequest.builder()
                .linkId(String.valueOf(linkUUID))
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

        // mock metric request to prm (pna-token)
        MetricsServiceIntegrationTests.wireMockServerForPRM.stubFor(WireMock.get(urlEqualTo("/api/v1/nodes/0b1c6697-cb29-4377-bcf8-9fd61ac6c0f3/pna-token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("")));

        // mock metric request to prm (pna-token)
        MetricsServiceIntegrationTests.wireMockServerForPRM.stubFor(WireMock.get(urlEqualTo("/api/v1/nodes/ea9084cf-97bb-451e-8220-4bdda327839e/pna-token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("")));

        // when
        MetricRequest metricRequest = this.metricsService.createNewIcmpRttMetricRequest(icmpRttMetricRequest);

        // then
        // TODO: further evaluations
        assertEquals(icmpRttMetricRequest.getLinkId(), metricRequest.getRemoteLinkUUID().toString());
        assertEquals(icmpRttMetricRequest.getType(), metricRequest.getType());
        assertEquals(icmpRttMetricRequest.getRecurrence(), metricRequest.getRecurrence());
        assertEquals(icmpRttMetricRequest.isEnabled(), metricRequest.isEnabled());
    }

    @Test
    public void testCreateNewMetricRequestTcpUdpWithTCP() throws InterruptedException {
        // given
        UUID srcNodeUUID = UUID.fromString("0b1c6697-cb29-4377-bcf8-9fd61ac6c0f3");
        UUID linkUUID = UUID.fromString("ea9084cf-97bb-451e-8220-4bdda327839e");
        TcpUdpRttMetricRequest tcpUdpRttMetricRequest = TcpUdpRttMetricRequest.builder()
                .linkId(String.valueOf(linkUUID))
                .type("tcp-rtt")
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
        MetricsServiceIntegrationTests.wireMockServerForPNA.stubFor(post(urlEqualTo("/api/v1/links/ea9084cf-97bb-451e-8220-4bcda327839e/metric-requests/tcp-rtt-requests"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("metricrequests/create-new-tcp-rtt-request-response.json")));

        // mock metric request to prm (pna-token)
        MetricsServiceIntegrationTests.wireMockServerForPRM.stubFor(WireMock.get(urlEqualTo("/api/v1/nodes/0b1c6697-cb29-4377-bcf8-9fd61ac6c0f3/pna-token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("")));

        // mock metric request to prm (pna-token)
        MetricsServiceIntegrationTests.wireMockServerForPRM.stubFor(WireMock.get(urlEqualTo("/api/v1/nodes/ea9084cf-97bb-451e-8220-4bdda327839e/pna-token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("")));

        // when
        MetricRequest metricRequest = this.metricsService.createNewTcpUdpRttMetricRequest(tcpUdpRttMetricRequest);

        // then
        // TODO: further evaluations
        assertEquals(tcpUdpRttMetricRequest.getLinkId(), metricRequest.getRemoteLinkUUID().toString());
        assertEquals(tcpUdpRttMetricRequest.getType(), metricRequest.getType());
        assertEquals(tcpUdpRttMetricRequest.getRecurrence(), metricRequest.getRecurrence());
        assertEquals(tcpUdpRttMetricRequest.isEnabled(), metricRequest.isEnabled());
    }

    @Test
    public void testCreateNewMetricRequestTcpUdpWithUDP() throws InterruptedException {
        // given
        UUID srcNodeUUID = UUID.fromString("0b1c6697-cb29-4377-bcf8-9fd61ac6c0f3");
        UUID linkUUID = UUID.fromString("ea9084cf-97bb-451e-8220-4bdda327839e");
        TcpUdpRttMetricRequest tcpUdpRttMetricRequest = TcpUdpRttMetricRequest.builder()
                .linkId(String.valueOf(linkUUID))
                .type("udp-rtt")
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
        MetricsServiceIntegrationTests.wireMockServerForPNA.stubFor(post(urlEqualTo("/api/v1/links/ea9084cf-97bb-451e-8220-4bcda327839e/metric-requests/udp-rtt-requests"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("metricrequests/create-new-udp-rtt-request-response.json")));

        // mock metric request to prm (pna-token)
        MetricsServiceIntegrationTests.wireMockServerForPRM.stubFor(WireMock.get(urlEqualTo("/api/v1/nodes/0b1c6697-cb29-4377-bcf8-9fd61ac6c0f3/pna-token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("")));

        // mock metric request to prm (pna-token)
        MetricsServiceIntegrationTests.wireMockServerForPRM.stubFor(WireMock.get(urlEqualTo("/api/v1/nodes/ea9084cf-97bb-451e-8220-4bdda327839e/pna-token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("")));

        // when
        MetricRequest metricRequest = this.metricsService.createNewTcpUdpRttMetricRequest(tcpUdpRttMetricRequest);

        // then
        // TODO: further evaluations
        assertEquals(tcpUdpRttMetricRequest.getLinkId(), metricRequest.getRemoteLinkUUID().toString());
        assertEquals(tcpUdpRttMetricRequest.getType(), metricRequest.getType());
        assertEquals(tcpUdpRttMetricRequest.getRecurrence(), metricRequest.getRecurrence());
        assertEquals(tcpUdpRttMetricRequest.isEnabled(), metricRequest.isEnabled());
    }

    @Test
    public void testCreateNewMetricRequestTcpBw () throws InterruptedException {
        // given
        UUID srcNodeUUID = UUID.fromString("0b1c6697-cb29-4377-bcf8-9fd61ac6c0f3");
        UUID destNodeUUID = UUID.fromString("d6421210-6759-4973-bad3-7f47bcb133c1");
        UUID linkUUID = UUID.fromString("ea9084cf-97bb-451e-8220-4bdda327839e");
        TcpBwMetricRequest tcpBwMetricRequest = TcpBwMetricRequest.builder()
                .port(5000)
                .linkId(String.valueOf(linkUUID))
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
                        .withBodyFile("node/prm-read-node-by-uuid-1-response.json")));

        MetricsServiceIntegrationTests.wireMockServerForPRM.stubFor(get(urlEqualTo("/api/v1/nodes/" + destNodeUUID))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("node/prm-read-node-by-uuid-2-response.json")));

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

        // mock start creation of Iperf3-Server
        MetricsServiceIntegrationTests.wireMockServerForDestPNA.stubFor(post(urlEqualTo("/api/v1/iperf3-servers"))
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
        assertEquals(tcpBwMetricRequest.getLinkId(), metricRequest.getRemoteLinkUUID().toString());
        assertEquals(tcpBwMetricRequest.getType(), metricRequest.getType());
        assertEquals(tcpBwMetricRequest.getRecurrence(), metricRequest.getRecurrence());
        assertEquals(tcpBwMetricRequest.isEnabled(), metricRequest.isEnabled());
    }

    // TODO: add tcp

    @Test
    public void testCreateNewCpuUtilMetricRequest() throws MetricsServiceException, InterruptedException {
        // given
        UUID srcNodeUUID = UUID.fromString("0b1c6697-cb29-4377-bcf8-9fd61ac6c0f3");
        ResourceUtilizationMetricRequest resourceUtilizationMetricRequest = ResourceUtilizationMetricRequest.builder()
                .nodeId(String.valueOf(srcNodeUUID))
                .resourceType("node")
                .type("cpu-util")
                .recurrence("15")
                .enabled(true)
                .build();

        // TODO: add mocks
        // mock link request to pna => done in SimulatedPnaAgent
        MetricsServiceIntegrationTests.wireMockServerForPRM.stubFor(get(urlEqualTo("/api/v1/nodes/" + srcNodeUUID))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("node/prm-read-node-by-uuid-1-response.json")));

        // mock metric request to prm (pna-token)
        MetricsServiceIntegrationTests.wireMockServerForPRM.stubFor(WireMock.get(urlEqualTo("/api/v1/nodes/0b1c6697-cb29-4377-bcf8-9fd61ac6c0f3/pna-token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("")));

        // mock metric request to pna with cpu-util-request
        MetricsServiceIntegrationTests.wireMockServerForPNA.stubFor(post(urlEqualTo("/api/v1/nodes/localNode/metric-requests"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("metricrequests/create-new-cpu-util-request-response.json")));

        // when
        MetricRequest metricRequest = this.metricsService.createNewResourceUtilizationRequest(resourceUtilizationMetricRequest);

        // then
        assertEquals(resourceUtilizationMetricRequest.getNodeId(), metricRequest.getRemoteLinkUUID().toString());
        assertEquals(resourceUtilizationMetricRequest.getType(), metricRequest.getType());
        assertEquals(resourceUtilizationMetricRequest.getRecurrence(), metricRequest.getRecurrence());
        assertEquals(resourceUtilizationMetricRequest.isEnabled(), metricRequest.isEnabled());

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
