package dev.pulceo.pms.service;

import dev.pulceo.pms.dto.link.NodeLinkDTO;
import dev.pulceo.pms.dto.metricrequests.CreateNewMetricRequestIcmpRttDTO;
import dev.pulceo.pms.exception.MetricsServiceException;
import dev.pulceo.pms.model.metricrequests.IcmpRttMetricRequest;
import dev.pulceo.pms.model.metricrequests.MetricRequest;
import dev.pulceo.pms.repository.MetricRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.geo.Metric;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class MetricsService {

    private final MetricRequestRepository metricRequestRepository;
    // ws
    private final SimpMessagingTemplate simpMessagingTemplate;

    @Value("${prm.endpoint}")
    private String prmEndpoint;

    @Autowired
    public MetricsService(MetricRequestRepository metricRequestRepository, SimpMessagingTemplate simpMessagingTemplate) {
        this.metricRequestRepository = metricRequestRepository;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    // ws
    private void process() {
        simpMessagingTemplate.convertAndSend("/metrics/", "content");
    }


    public MetricRequest createNewIcmpRttMetricRequest(IcmpRttMetricRequest icmpRttMetricRequest) {
        // linkUUID - on device
        // TODO: check if link does already exist
        WebClient webClient = WebClient.create(this.prmEndpoint);
        NodeLinkDTO nodeLinkDTO = webClient.get()
                .uri("/api/v1/links/" + icmpRttMetricRequest.getLinkUUID())
                .retrieve()
                .bodyToMono(NodeLinkDTO.class)
                .onErrorResume(error -> {
                    throw new RuntimeException(new MetricsServiceException("Link with id %s does not exist!".formatted(icmpRttMetricRequest.getLinkUUID())));
                })
                .block();

        // TODO: perform a request to pna with ICMP RTT metric request
        CreateNewMetricRequestIcmpRttDTO createNewMetricRequestIcmpRttDTO = CreateNewMetricRequestIcmpRttDTO.builder()
                .linkUUID(icmpRttMetricRequest.getLinkUUID())
                .type(icmpRttMetricRequest.getType())
                .recurrence(icmpRttMetricRequest.getRecurrence())
                .enabled(icmpRttMetricRequest.isEnabled())
                .build();
        // Instruct

        // TODO: persist the metric request in the database
        // TODO: store the icmpmetricrequest in the databas
        MetricRequest metricRequest = MetricRequest.builder()
                .uuid(icmpRttMetricRequest.getUuid())
                .linkUUID(icmpRttMetricRequest.getLinkUUID())
                .type(icmpRttMetricRequest.getType())
                .recurrence(icmpRttMetricRequest.getRecurrence())
                .enabled(icmpRttMetricRequest.isEnabled())
                .build();

        return this.metricRequestRepository.save(metricRequest);
    }

}
