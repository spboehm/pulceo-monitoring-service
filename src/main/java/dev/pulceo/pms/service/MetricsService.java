package dev.pulceo.pms.service;

import dev.pulceo.pms.dto.link.NodeLinkDTO;
import dev.pulceo.pms.dto.metricrequests.CreateNewMetricRequestIcmpRttDTO;
import dev.pulceo.pms.dto.node.NodeDTO;
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

import java.util.UUID;

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
                .uri("/api/v1/links/" + icmpRttMetricRequest.getLinkUUID()) // on cloud
                .retrieve()
                .bodyToMono(NodeLinkDTO.class)
                .onErrorResume(error -> {
                    throw new RuntimeException(new MetricsServiceException("Can not create link: Link with id %s does not exist!".formatted(icmpRttMetricRequest.getLinkUUID())));
                })
                .block();

        // Instruct pna to create a new ICMP RTT metric request
        // determine srcNode of the link to send the request to the correct pna

        // first obtain the hostname
        UUID srcNodeUUID = nodeLinkDTO.getSrcNodeUUID();
        NodeDTO srcNode = webClient.get()
                .uri("/api/v1/nodes/" + srcNodeUUID)
                .retrieve()
                .bodyToMono(NodeDTO.class)
                .onErrorResume(error -> {
                    throw new RuntimeException(new MetricsServiceException("Can not create link: Source node with id %s does not exist!".formatted(srcNodeUUID)));
                })
                .block();

        // then send the request to the correct pna
        CreateNewMetricRequestIcmpRttDTO createNewMetricRequestIcmpRttDTO = CreateNewMetricRequestIcmpRttDTO.builder()
                .linkUUID(nodeLinkDTO.getRemoteNodeLinkUUID()) // replace by the remote link UUID, otherwise the id cannot be found
                .type(icmpRttMetricRequest.getType())
                .recurrence(icmpRttMetricRequest.getRecurrence())
                .enabled(icmpRttMetricRequest.isEnabled())
                .build();

        webClient = WebClient.create("http://" + srcNode.getHostname() + ":7676");
        MetricRequest metricRequest = webClient.post()
                .uri("/api/v1/links/" + nodeLinkDTO.getRemoteNodeLinkUUID() + "/metric-requests/icmp-rtt-requests")
                .bodyValue(createNewMetricRequestIcmpRttDTO)
                .retrieve()
                .bodyToMono(MetricRequest.class)
                .onErrorResume(error -> {
                    throw new RuntimeException(new MetricsServiceException("Can not create metric request!"));
                })
                .block();
        // TODO: do conversion to DTO and persist then in database
        return this.metricRequestRepository.save(metricRequest);
    }

}
