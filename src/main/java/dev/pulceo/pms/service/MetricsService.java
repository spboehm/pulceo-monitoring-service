package dev.pulceo.pms.service;

import dev.pulceo.pms.dto.link.NodeLinkDTO;
import dev.pulceo.pms.dto.metricrequests.CreateNewMetricRequestIcmpRttDTO;
import dev.pulceo.pms.dto.metricrequests.CreateNewMetricRequestTcpBwDTO;
import dev.pulceo.pms.dto.metricrequests.pna.CreateNewResourceUtilizationDTO;
import dev.pulceo.pms.dto.metricrequests.pna.ShortNodeMetricResponseDTO;
import dev.pulceo.pms.dto.node.NodeDTO;
import dev.pulceo.pms.exception.MetricsServiceException;
import dev.pulceo.pms.model.metric.NodeLinkMetric;
import dev.pulceo.pms.model.metricrequests.ResourceUtilizationMetricRequest;
import dev.pulceo.pms.model.metricrequests.IcmpRttMetricRequest;
import dev.pulceo.pms.model.metricrequests.MetricRequest;
import dev.pulceo.pms.model.metricrequests.TcpBwMetricRequest;
import dev.pulceo.pms.repository.MetricRequestRepository;
import dev.pulceo.pms.repository.NodeLinkMetricRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class MetricsService {

    private final MetricRequestRepository metricRequestRepository;
    private final NodeLinkMetricRepository nodeLinkMetricRepository;
    // ws
    private final SimpMessagingTemplate simpMessagingTemplate;

    private final InfluxDBService influxDBService;

    @Value("${prm.endpoint}")
    private String prmEndpoint;

    @Autowired
    public MetricsService(MetricRequestRepository metricRequestRepository, SimpMessagingTemplate simpMessagingTemplate, InfluxDBService influxDBService, NodeLinkMetricRepository nodeLinkMetricRepository) {
        this.metricRequestRepository = metricRequestRepository;
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.influxDBService = influxDBService;
        this.nodeLinkMetricRepository = nodeLinkMetricRepository;
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
                    throw new RuntimeException(new MetricsServiceException("Can not create metric request: Link with id %s does not exist!".formatted(icmpRttMetricRequest.getLinkUUID())));
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
        // TODO: proper DTO conversion
        MetricRequest metricRequest = webClient.post()
                .uri("/api/v1/links/" + nodeLinkDTO.getRemoteNodeLinkUUID() + "/metric-requests/icmp-rtt-requests")
                .header("Authorization", "Basic " + getPnaTokenByNodeUUID(srcNodeUUID))
                .bodyValue(createNewMetricRequestIcmpRttDTO)
                .retrieve()
                .bodyToMono(MetricRequest.class)
                .onErrorResume(error -> {
                    throw new RuntimeException(new MetricsServiceException("Can not create metric request!"));
                })
                .block();
        // TODO: set link UUID to achieve an appropriate mapping
        metricRequest.setLinkUUID(icmpRttMetricRequest.getLinkUUID());
        MetricRequest savedMetricRequest = this.metricRequestRepository.save(metricRequest);
        // TODO: do conversion to DTO and persist then in database
        this.influxDBService.notifyAboutNewMetricRequest(savedMetricRequest);
        return savedMetricRequest;
    }

    public MetricRequest createNewTcpBwMetricRequest(TcpBwMetricRequest tcpBwMetricRequest) {
        // linkUUID - on device
        // TODO: check if link does already exist
        WebClient webClientToPRM = WebClient.create(this.prmEndpoint);
        NodeLinkDTO nodeLinkDTO = webClientToPRM.get()
                .uri("/api/v1/links/" + tcpBwMetricRequest.getLinkUUID()) // on cloud
                .retrieve()
                .bodyToMono(NodeLinkDTO.class)
                .onErrorResume(error -> {
                    throw new RuntimeException(new MetricsServiceException("Can not create metric request: Link with id %s does not exist!".formatted(tcpBwMetricRequest.getLinkUUID())));
                })
                .block();
        // Instruct pna to create a new ICMP RTT metric request
        // determine srcNode of the link to send the request to the correct pna

        // first obtain the hostname
        UUID srcNodeUUID = nodeLinkDTO.getSrcNodeUUID();
        NodeDTO srcNode = webClientToPRM.get()
                .uri("/api/v1/nodes/" + srcNodeUUID)
                .retrieve()
                .bodyToMono(NodeDTO.class)
                .onErrorResume(error -> {
                    throw new RuntimeException(new MetricsServiceException("Can not create link: Source node with id %s does not exist!".formatted(srcNodeUUID)));
                })
                .block();


        // destNode start iperf3 server
        UUID destNodeUUID = nodeLinkDTO.getDestNodeUUID();
        NodeDTO destNode = webClientToPRM.get()
                .uri("/api/v1/nodes/" + destNodeUUID)
                .retrieve()
                .bodyToMono(NodeDTO.class)
                .onErrorResume(error -> {
                    throw new RuntimeException(new MetricsServiceException("Can not create link: Source node with id %s does not exist!".formatted(srcNodeUUID)));
                })
                .block();

        // create new iperf-server using iperf3 server controller
        WebClient webClientToDestNode = WebClient.create("http://" + destNode.getHostname() + ":7676");
                String portOfRemoteIperfServer = webClientToDestNode.post()
                .uri("/api/v1/iperf3-servers")
                .header("Authorization", "Basic " + getPnaTokenByNodeUUID(destNodeUUID))
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(error -> {
                    throw new RuntimeException(new MetricsServiceException("Can not create iperf3 server!"));
                })
                .block();

        // inform src Node about the new iperf server
        CreateNewMetricRequestTcpBwDTO createNewMetricRequestTcpBwDTO = CreateNewMetricRequestTcpBwDTO.builder()
                .linkUUID(nodeLinkDTO.getRemoteNodeLinkUUID()) // replace by the remote link UUID, otherwise the id cannot be found
                .port(Long.valueOf(portOfRemoteIperfServer))
                .type(tcpBwMetricRequest.getType())
                .recurrence(tcpBwMetricRequest.getRecurrence())
                .enabled(tcpBwMetricRequest.isEnabled())
                .build();

        WebClient webclientToPNA = WebClient.create("http://" + srcNode.getHostname() + ":7676");
        MetricRequest metricRequest = webclientToPNA.post()
                .uri("/api/v1/links/" + nodeLinkDTO.getRemoteNodeLinkUUID() + "/metric-requests/tcp-bw-requests")
                .header("Authorization", "Basic " + getPnaTokenByNodeUUID(srcNodeUUID))
                .bodyValue(createNewMetricRequestTcpBwDTO)
                .retrieve()
                .bodyToMono(MetricRequest.class)
                .onErrorResume(error -> {
                    throw new RuntimeException(new MetricsServiceException("Can not create metric request!"));
                })
                .block();
        // TODO: set link UUID to achieve an appropriate mapping in cloud
        metricRequest.setLinkUUID(tcpBwMetricRequest.getLinkUUID());
        // TODO: do conversion to DTO and persist then in database
        MetricRequest savedMetricRequest = this.metricRequestRepository.save(metricRequest);
        // then send the request to the correct pna
        this.influxDBService.notifyAboutNewMetricRequest(savedMetricRequest);
        return savedMetricRequest;
    }

    private String getPnaTokenByNodeUUID(UUID nodeUUID) {
        WebClient webClient = WebClient.create(this.prmEndpoint);
        String pnaToken = webClient.get()
                .uri("/api/v1/nodes/" + nodeUUID + "/pna-token")
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(error -> {
                    throw new RuntimeException(new MetricsServiceException("Can not create link: Source node with id %s does not exist!".formatted(nodeUUID)));
                })
                .block();
        return pnaToken;
    }

    @PostConstruct
    void init() {
        // inform InfluxDBService about metric requests
        this.metricRequestRepository.findAll().forEach(metricRequest -> {
            this.influxDBService.notifyAboutNewMetricRequest(metricRequest);
        });
    }

    public List<NodeLinkMetric> readLastNodeLinkMetricsByLinkUUIDAndMetricType(UUID linkUUID) {
        List<String> findDistinctMetricTypes = this.nodeLinkMetricRepository.findDistinctMetricTypes();
        List<NodeLinkMetric> resultList = new ArrayList<>();
        for (String type : findDistinctMetricTypes) {
            resultList.add(this.nodeLinkMetricRepository.findLastLinkUUIDAndByMetricType(String.valueOf(linkUUID), type));
        }
        return resultList;
    }

    public MetricRequest createNewResourceUtilizationRequest(ResourceUtilizationMetricRequest resourceUtilizationMetricRequest) {
        WebClient webClientToPRM = WebClient.create(this.prmEndpoint);

        // first obtain the hostname
        UUID srcNodeUUID = resourceUtilizationMetricRequest.getNodeUUID();
        NodeDTO srcNode = webClientToPRM.get()
                .uri("/api/v1/nodes/" + srcNodeUUID)
                .retrieve()
                .bodyToMono(NodeDTO.class)
                .onErrorResume(error -> {
                    throw new RuntimeException(new MetricsServiceException("Can not create link: Source node with id %s does not exist!".formatted(srcNodeUUID)));
                })
                .block();

        // TODO: Build request
        CreateNewResourceUtilizationDTO createNewResourceUtilizationDTO = CreateNewResourceUtilizationDTO.builder()
                .type(resourceUtilizationMetricRequest.getType())
                .recurrence(Integer.parseInt(resourceUtilizationMetricRequest.getRecurrence()))
                .enabled(resourceUtilizationMetricRequest.isEnabled())
                .build();

        WebClient webclientToPNA = WebClient.create("http://" + srcNode.getHostname() + ":7676");
        ShortNodeMetricResponseDTO shortNodeMetricResponseDTO = webclientToPNA.post()
                .uri("/api/v1/nodes/localNode/metric-requests")
                .header("Authorization", "Basic " + getPnaTokenByNodeUUID(srcNodeUUID))
                .bodyValue(createNewResourceUtilizationDTO)
                .retrieve()
                .bodyToMono(ShortNodeMetricResponseDTO.class)
                .onErrorResume(error -> {
                    throw new RuntimeException(new MetricsServiceException("Can not create metric request!"));
                })
                .block();

        MetricRequest metricRequest = MetricRequest.fromShortNodeMetricResponseDTO(shortNodeMetricResponseDTO);
        // TODO: set link UUID to achieve an appropriate mapping in cloud
        metricRequest.setLinkUUID(srcNodeUUID); // global uuid
//        // TODO: do conversion to DTO and persist then in database
        MetricRequest savedMetricRequest = this.metricRequestRepository.save(metricRequest);
        // then send the request to the correct pna
        this.influxDBService.notifyAboutNewMetricRequest(savedMetricRequest);
        return savedMetricRequest;
    }

    // TODO: on startup inform InfluxDBService about all existing metric requests that are in DB

    // TODO: on shutdown inform InfluxDBService to stop all running metric requests

}
