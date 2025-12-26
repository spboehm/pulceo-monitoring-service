package dev.pulceo.pms.service;

import dev.pulceo.pms.dto.application.ApplicationDTO;
import dev.pulceo.pms.dto.link.NodeLinkDTO;
import dev.pulceo.pms.dto.metricrequests.pna.*;
import dev.pulceo.pms.dto.node.NodeDTO;
import dev.pulceo.pms.exception.MetricsServiceException;
import dev.pulceo.pms.exception.ResourceNotFoundException;
import dev.pulceo.pms.model.event.EventType;
import dev.pulceo.pms.model.event.PulceoEvent;
import dev.pulceo.pms.model.metric.NodeLinkMetric;
import dev.pulceo.pms.model.metricrequests.*;
import dev.pulceo.pms.repository.MetricRequestRepository;
import dev.pulceo.pms.repository.NodeLinkMetricRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final Logger logger = LoggerFactory.getLogger(MetricsService.class);
    private final MetricRequestRepository metricRequestRepository;
    private final NodeLinkMetricRepository nodeLinkMetricRepository;
    // ws
    private final SimpMessagingTemplate simpMessagingTemplate;

    private final InfluxDBService influxDBService;

    @Value("${prm.endpoint}")
    private String prmEndpoint;

    @Value("${psm.endpoint}")
    private String psmEndpoint;

    @Value("${webclient.scheme}")
    private String webClientScheme;

    private final EventHandler eventHandler;

    @Autowired
    public MetricsService(MetricRequestRepository metricRequestRepository, SimpMessagingTemplate simpMessagingTemplate, InfluxDBService influxDBService, NodeLinkMetricRepository nodeLinkMetricRepository, EventHandler eventHandler) {
        this.metricRequestRepository = metricRequestRepository;
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.influxDBService = influxDBService;
        this.nodeLinkMetricRepository = nodeLinkMetricRepository;
        this.eventHandler = eventHandler;
    }

    // ws
    private void process() {
        simpMessagingTemplate.convertAndSend("/metrics/", "content");
    }

    public MetricRequest createNewIcmpRttMetricRequest(IcmpRttMetricRequest icmpRttMetricRequest) throws InterruptedException {

        if (icmpRttMetricRequest.getLinkId() == null) {
            throw new RuntimeException(new MetricsServiceException("Can not create metric request: Link id is missing!"));
        }

        WebClient webClientToPRM = WebClient.create(this.prmEndpoint);
        List<NodeLinkDTO> allLinks;

        if ("*".equals(icmpRttMetricRequest.getLinkId())) {
            allLinks = webClientToPRM
                    .get()
                    .uri("/api/v1/links")
                    .retrieve()
                    .bodyToFlux(NodeLinkDTO.class)
                    .collectList()
                    .block();
        } else {
            allLinks = List.of(webClientToPRM.get()
                    .uri("/api/v1/links/" + icmpRttMetricRequest.getLinkId())
                    .retrieve()
                    .bodyToMono(NodeLinkDTO.class)
                    .onErrorResume(error -> {
                        throw new RuntimeException(new MetricsServiceException("Can not create metric request: Link with id %s does not exist!".formatted(icmpRttMetricRequest.getLinkId())));
                    })
                    .block());
        }

        MetricRequest lastMetricRequest = null;
        for (NodeLinkDTO nodeLink : allLinks) {
            // linkUUID - on device
            // TODO: check if link does already exist
            NodeLinkDTO nodeLinkDTO = webClientToPRM.get()
                    .uri("/api/v1/links/" + nodeLink.getLinkUUID()) // on cloud
                    .retrieve()
                    .bodyToMono(NodeLinkDTO.class)
                    .onErrorResume(error -> {
                        throw new RuntimeException(new MetricsServiceException("Can not create metric request: Link with id %s does not exist!".formatted(nodeLink.getLinkUUID())));
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

            // then send the request to the correct pna
            CreateNewMetricRequestIcmpRttOnPNADTO createNewMetricRequestIcmpRttDTO = CreateNewMetricRequestIcmpRttOnPNADTO.builder()
                    .linkUUID(nodeLinkDTO.getRemoteNodeLinkUUID()) // replace by the remote link UUID, otherwise the id cannot be found
                    .type(icmpRttMetricRequest.getType())
                    .recurrence(icmpRttMetricRequest.getRecurrence())
                    .enabled(icmpRttMetricRequest.isEnabled())
                    .build();

            WebClient webClientToPNA = WebClient.create(this.webClientScheme + "://" + srcNode.getHostname() + ":7676");
            // TODO: proper DTO conversion
            MetricRequest metricRequest = webClientToPNA.post()
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
            metricRequest.setLinkUUID(UUID.fromString(nodeLinkDTO.getLinkUUID())); // global UUID
            MetricRequest savedMetricRequest = this.metricRequestRepository.save(metricRequest);
            // TODO: do conversion to DTO and persist then in database
            this.influxDBService.notifyAboutNewMetricRequest(savedMetricRequest);

            PulceoEvent pulceoEvent = PulceoEvent.builder()
                    .eventType(EventType.LINK_METRIC_REQUEST_CREATED)
                    .payload(savedMetricRequest.toString())
                    .build();
            this.eventHandler.handleEvent(pulceoEvent);

            lastMetricRequest = savedMetricRequest;
        }
        return lastMetricRequest;
    }

    public MetricRequest createNewTcpUdpRttMetricRequest(TcpUdpRttMetricRequest tcpUdpRttMetricRequest) throws InterruptedException {

        if (tcpUdpRttMetricRequest.getLinkId() == null) {
            throw new RuntimeException(new MetricsServiceException("Can not create metric request: Link id is missing!"));
        }

        WebClient webClientToPRM = WebClient.create(this.prmEndpoint);
        List<NodeLinkDTO> allLinks;
        if ("*".equals(tcpUdpRttMetricRequest.getLinkId())) {
            allLinks = WebClient.create(this.prmEndpoint)
                    .get()
                    .uri("/api/v1/links")
                    .retrieve()
                    .bodyToFlux(NodeLinkDTO.class)
                    .collectList()
                    .block();
        } else {
            allLinks = List.of(webClientToPRM.get()
                    .uri("/api/v1/links/" + tcpUdpRttMetricRequest.getLinkId())
                    .retrieve()
                    .bodyToMono(NodeLinkDTO.class)
                    .onErrorResume(error -> {
                        throw new RuntimeException(new MetricsServiceException("Can not create metric request: Link with id %s does not exist!".formatted(tcpUdpRttMetricRequest.getLinkId())));
                    })
                    .block());
        }

        MetricRequest lastMetricRequest = null;

        for (NodeLinkDTO nodeLink : allLinks) {
            // linkUUID - on device
            // TODO: check if link does already exist
            NodeLinkDTO nodeLinkDTO = webClientToPRM.get()
                    .uri("/api/v1/links/" + nodeLink.getLinkUUID()) // on cloud
                    .retrieve()
                    .bodyToMono(NodeLinkDTO.class)
                    .onErrorResume(error -> {
                        throw new RuntimeException(new MetricsServiceException("Can not create metric request: Link with id %s does not exist!".formatted(tcpUdpRttMetricRequest.getLinkId())));
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

            // then send the request to the correct pna
            CreateNewMetricRequestTcpUdpRttOnPNADTO createNewMetricRequestTcpUdpRttOnPNADTO = CreateNewMetricRequestTcpUdpRttOnPNADTO.builder()
                    .linkUUID(nodeLinkDTO.getRemoteNodeLinkUUID()) // replace by the remote link UUID, otherwise the id cannot be found
                    .type(tcpUdpRttMetricRequest.getType())
                    .recurrence(tcpUdpRttMetricRequest.getRecurrence())
                    .enabled(tcpUdpRttMetricRequest.isEnabled())
                    .rounds(tcpUdpRttMetricRequest.getRounds())
                    .build();

            WebClient webClientToPNA = WebClient.create(this.webClientScheme + "://" + srcNode.getHostname() + ":7676");
            // TODO: proper DTO conversion
            MetricRequest metricRequest;
            if (tcpUdpRttMetricRequest.getType().equals("tcp-rtt")) {
                metricRequest = webClientToPNA.post()
                        .uri("/api/v1/links/" + nodeLinkDTO.getRemoteNodeLinkUUID() + "/metric-requests/tcp-rtt-requests")
                        .header("Authorization", "Basic " + getPnaTokenByNodeUUID(srcNodeUUID))
                        .bodyValue(createNewMetricRequestTcpUdpRttOnPNADTO)
                        .retrieve()
                        .bodyToMono(MetricRequest.class)
                        .onErrorResume(error -> {
                            throw new RuntimeException(new MetricsServiceException("Can not create metric request!"));
                        })
                        .block();
            } else if (tcpUdpRttMetricRequest.getType().equals("udp-rtt")) {
                metricRequest = webClientToPNA.post()
                        .uri("/api/v1/links/" + nodeLinkDTO.getRemoteNodeLinkUUID() + "/metric-requests/udp-rtt-requests")
                        .header("Authorization", "Basic " + getPnaTokenByNodeUUID(srcNodeUUID))
                        .bodyValue(createNewMetricRequestTcpUdpRttOnPNADTO)
                        .retrieve()
                        .bodyToMono(MetricRequest.class)
                        .onErrorResume(error -> {
                            throw new RuntimeException(new MetricsServiceException("Can not create metric request!"));
                        })
                        .block();
            } else {
                throw new RuntimeException(new MetricsServiceException("Can not create metric request!"));
            }

            // TODO: set link UUID to achieve an appropriate mapping
            metricRequest.setLinkUUID(UUID.fromString(nodeLinkDTO.getLinkUUID())); // global UUID
            MetricRequest savedMetricRequest = this.metricRequestRepository.save(metricRequest);
            // TODO: do conversion to DTO and persist then in database
            this.influxDBService.notifyAboutNewMetricRequest(savedMetricRequest);

            PulceoEvent pulceoEvent = PulceoEvent.builder()
                    .eventType(EventType.LINK_METRIC_REQUEST_CREATED)
                    .payload(savedMetricRequest.toString())
                    .build();
            this.eventHandler.handleEvent(pulceoEvent);

            lastMetricRequest = savedMetricRequest;
        }
        return lastMetricRequest;
    }

    public MetricRequest createNewTcpBwMetricRequest(TcpBwMetricRequest tcpBwMetricRequest) throws InterruptedException {
        if (tcpBwMetricRequest.getLinkId() == null) {
            throw new RuntimeException(new MetricsServiceException("Can not create metric request: Link id is missing!"));
        }

        WebClient webClientToPRM = WebClient.create(this.prmEndpoint);
        List<NodeLinkDTO> allLinks;
        if ("*".equals(tcpBwMetricRequest.getLinkId())) {
            allLinks = WebClient.create(this.prmEndpoint)
                    .get()
                    .uri("/api/v1/links")
                    .retrieve()
                    .bodyToFlux(NodeLinkDTO.class)
                    .collectList()
                    .block();
        } else {
            allLinks = List.of(webClientToPRM.get()
                    .uri("/api/v1/links/" + tcpBwMetricRequest.getLinkId())
                    .retrieve()
                    .bodyToMono(NodeLinkDTO.class)
                    .onErrorResume(error -> {
                        throw new RuntimeException(new MetricsServiceException("Can not create metric request: Link with id %s does not exist!".formatted(tcpBwMetricRequest.getLinkId())));
                    })
                    .block());
        }

        MetricRequest lastMetricRequest = null;
        int seq = 0;
        for (NodeLinkDTO nodeLink : allLinks) {

            // linkUUID - on device
            // TODO: check if link does already exist
            NodeLinkDTO nodeLinkDTO = webClientToPRM.get()
                    .uri("/api/v1/links/" + nodeLink.getLinkUUID()) // on cloud
                    .retrieve()
                    .bodyToMono(NodeLinkDTO.class)
                    .onErrorResume(error -> {
                        throw new RuntimeException(new MetricsServiceException("Can not create metric request: Link with id %s does not exist!".formatted(tcpBwMetricRequest.getLinkId())));
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
            WebClient webClientToDestNode = WebClient.create(this.webClientScheme + "://" + destNode.getHostname() + ":7676");
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
            CreateNewMetricRequestTcpBwOnPNADTO createNewMetricRequestTcpBwDTO = CreateNewMetricRequestTcpBwOnPNADTO.builder()
                    .linkUUID(nodeLinkDTO.getRemoteNodeLinkUUID()) // replace by the remote link UUID, otherwise the id cannot be found
                    .port(Long.valueOf(portOfRemoteIperfServer))
                    .type(tcpBwMetricRequest.getType())
                    .recurrence(tcpBwMetricRequest.getRecurrence())
                    .enabled(tcpBwMetricRequest.isEnabled())
                    .bitrate(tcpBwMetricRequest.getBitrate())
                    .time(tcpBwMetricRequest.getTime())
                    .initialDelay(tcpBwMetricRequest.getInitialDelay() * seq++)
                    .build();

            WebClient webclientToPNA = WebClient.create(this.webClientScheme + "://" + srcNode.getHostname() + ":7676");
            // case udp or tcp because of different API links for the request on pna
            MetricRequest metricRequest;
            if (tcpBwMetricRequest.getType().equals("tcp-bw")) {
                metricRequest = webclientToPNA.post()
                        .uri("/api/v1/links/" + nodeLinkDTO.getRemoteNodeLinkUUID() + "/metric-requests/tcp-bw-requests")
                        .header("Authorization", "Basic " + getPnaTokenByNodeUUID(srcNodeUUID))
                        .bodyValue(createNewMetricRequestTcpBwDTO)
                        .retrieve()
                        .bodyToMono(MetricRequest.class)
                        .onErrorResume(error -> {
                            throw new RuntimeException(new MetricsServiceException("Can not create metric request!"));
                        })
                        .block();
            } else if (tcpBwMetricRequest.getType().equals("udp-bw")) {
                metricRequest = webclientToPNA.post()
                        .uri("/api/v1/links/" + nodeLinkDTO.getRemoteNodeLinkUUID() + "/metric-requests/udp-bw-requests")
                        .header("Authorization", "Basic " + getPnaTokenByNodeUUID(srcNodeUUID))
                        .bodyValue(createNewMetricRequestTcpBwDTO)
                        .retrieve()
                        .bodyToMono(MetricRequest.class)
                        .onErrorResume(error -> {
                            throw new RuntimeException(new MetricsServiceException("Can not create metric request!"));
                        })
                        .block();
            } else {
                throw new RuntimeException(new MetricsServiceException("Can not create metric request!"));
            }

            // TODO: set link UUID to achieve an appropriate mapping in cloud
            metricRequest.setLinkUUID(UUID.fromString(nodeLinkDTO.getLinkUUID())); // global uuid
            // TODO: do conversion to DTO and persist then in database
            MetricRequest savedMetricRequest = this.metricRequestRepository.save(metricRequest);
            // then send the request to the correct pna
            this.influxDBService.notifyAboutNewMetricRequest(savedMetricRequest);

            PulceoEvent pulceoEvent = PulceoEvent.builder()
                    .eventType(EventType.LINK_METRIC_REQUEST_CREATED)
                    .payload(savedMetricRequest.toString())
                    .build();
            this.eventHandler.handleEvent(pulceoEvent);

            lastMetricRequest = savedMetricRequest;
        }
        return lastMetricRequest;
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

    public MetricRequest createNewResourceUtilizationRequest(ResourceUtilizationMetricRequest resourceUtilizationMetricRequest) throws MetricsServiceException, InterruptedException {
        // decide if for node or app
        if ("node".equals(resourceUtilizationMetricRequest.getResourceType())) {
            return createNewResourceUtilizationRequestForNode(resourceUtilizationMetricRequest);
        } else if ("application".equals(resourceUtilizationMetricRequest.getResourceType())) {
            return createNewResourceUtilizationRequestForApplication(resourceUtilizationMetricRequest);
        } else {
            throw new MetricsServiceException("Can not create metric request: Node id or application id is missing!");
        }
    }

    private MetricRequest createNewResourceUtilizationRequestForApplication(ResourceUtilizationMetricRequest resourceUtilizationMetricRequest) throws InterruptedException {
        if (resourceUtilizationMetricRequest.getNodeId() == null) {
            throw new RuntimeException(new MetricsServiceException("Can not create metric request: Node id is missing!"));
        }

        WebClient webClientToPSM = WebClient.create(this.psmEndpoint);

        List<ApplicationDTO> allApplications;
        if ("*".equals(resourceUtilizationMetricRequest.getNodeId())) {
            allApplications = webClientToPSM
                    .get()
                    .uri("/api/v1/applications")
                    .retrieve()
                    .bodyToFlux(ApplicationDTO.class)
                    .collectList()
                    .block();
        } else {
            allApplications = List.of(webClientToPSM.get()
                    .uri("/api/v1/applications/" + resourceUtilizationMetricRequest.getNodeId())
                    .retrieve()
                    .bodyToMono(ApplicationDTO.class)
                    .onErrorResume(error -> {
                        throw new RuntimeException(new MetricsServiceException("Can not create metric request: Application with id %s does not exist!".formatted(resourceUtilizationMetricRequest.getNodeId())));
                    })
                    .block());
        }

        MetricRequest lastMetricRequest = null;
        for (ApplicationDTO application : allApplications) {
            // first obtain the remote host information
            WebClient webClientToPRM = WebClient.create(this.prmEndpoint);
            NodeDTO remoteNode = webClientToPRM.get()
                    .uri("/api/v1/nodes/" + application.getNodeId())
                    .retrieve()
                    .bodyToMono(NodeDTO.class)
                    .onErrorResume(error -> {
                        throw new RuntimeException(new MetricsServiceException("Can not create link: Source node with id %s does not exist!".formatted(application.getNodeId())));
                    })
                    .block();

            // then send the request to the correct pna
            CreateNewResourceUtilizationDTO createNewResourceUtilizationDTO = CreateNewResourceUtilizationDTO.builder()
                    .type(resourceUtilizationMetricRequest.getType())
                    .recurrence(Integer.parseInt(resourceUtilizationMetricRequest.getRecurrence()))
                    .enabled(resourceUtilizationMetricRequest.isEnabled())
                    .build();

            WebClient webclientToPNA = WebClient.create(this.webClientScheme + "://" + remoteNode.getHostname() + ":7676");
            ShortNodeMetricResponseDTO shortNodeMetricResponseDTO = webclientToPNA.post()
                    .uri("/api/v1/applications/" + application.getRemoteApplicationUUID() + "/metric-requests")
                    .header("Authorization", "Basic " + getPnaTokenByNodeUUID(remoteNode.getUuid()))
                    .bodyValue(createNewResourceUtilizationDTO)
                    .retrieve()
                    .bodyToMono(ShortNodeMetricResponseDTO.class)
                    .onErrorResume(error -> {
                        throw new RuntimeException(new MetricsServiceException("Can not create metric request!"));
                    })
                    .block();

            MetricRequest metricRequest = MetricRequest.fromShortNodeMetricResponseDTO(shortNodeMetricResponseDTO);
            metricRequest.setLinkUUID(UUID.fromString(application.getApplicationUUID())); // global uuid
            MetricRequest savedMetricRequest = this.metricRequestRepository.save(metricRequest);
            this.influxDBService.notifyAboutNewMetricRequest(metricRequest);

            PulceoEvent pulceoEvent = PulceoEvent.builder()
                    .eventType(EventType.APPLICATION_METRIC_REQUEST_CREATED)
                    .payload(savedMetricRequest.toString())
                    .build();
            this.eventHandler.handleEvent(pulceoEvent);

            lastMetricRequest = metricRequest;
        }
        return lastMetricRequest;
    }


    public MetricRequest createNewResourceUtilizationRequestForNode(ResourceUtilizationMetricRequest resourceUtilizationMetricRequest) throws InterruptedException, MetricsServiceException {
        // TODO: evalutate nodeId
        if (resourceUtilizationMetricRequest.getNodeId() == null) {
            throw new RuntimeException(new MetricsServiceException("Can not create metric request: Node id is missing!"));
        }

        WebClient webClientToPRM = WebClient.create(this.prmEndpoint);
        List<NodeDTO> allNodes;
        if ("*".equals(resourceUtilizationMetricRequest.getNodeId())) {
             allNodes = WebClient.create(this.prmEndpoint)
                    .get()
                    .uri("/api/v1/nodes")
                    .retrieve()
                    .bodyToFlux(NodeDTO.class)
                    .collectList()
                    .block();
        } else {
            allNodes = List.of(webClientToPRM.get()
                    .uri("/api/v1/nodes/" + resourceUtilizationMetricRequest.getNodeId())
                    .retrieve()
                    .bodyToMono(NodeDTO.class)
                    .onErrorResume(error -> {
                        throw new RuntimeException(new MetricsServiceException("Can not create metric request: Node with id %s does not exist!".formatted(resourceUtilizationMetricRequest.getNodeId())));
                    })
                    .block());
        }
        MetricRequest lastMetricRequest = null;
        for (NodeDTO node : allNodes) {
            // check if there is already a metric request for that node, same type
            if (this.checkIfMetricRequestsExists(node.getUuid(), resourceUtilizationMetricRequest.getType())) {
                this.logger.warn("Metric request for nodeUuid={} and type={} does already exist...skipping creation", node.getUuid(), resourceUtilizationMetricRequest.getType());
                lastMetricRequest = this.readMetricRequestByLinkUuidAndType(node.getUuid(), resourceUtilizationMetricRequest.getType());
                continue; // skip the current interation
            }

            // first obtain the hostname
            String srcNodeID = node.getUuid().toString();
            NodeDTO srcNode = webClientToPRM.get()
                    .uri("/api/v1/nodes/" + srcNodeID)
                    .retrieve()
                    .bodyToMono(NodeDTO.class)
                    .onErrorResume(error -> {
                        throw new RuntimeException(new MetricsServiceException("Can not create link: Source node with id %s does not exist!".formatted(srcNodeID)));
                    })
                    .block();

            // TODO: Build request
            CreateNewResourceUtilizationDTO createNewResourceUtilizationDTO = CreateNewResourceUtilizationDTO.builder()
                    .type(resourceUtilizationMetricRequest.getType())
                    .recurrence(Integer.parseInt(resourceUtilizationMetricRequest.getRecurrence()))
                    .enabled(resourceUtilizationMetricRequest.isEnabled())
                    .build();

            WebClient webclientToPNA = WebClient.create(this.webClientScheme + "://" + srcNode.getHostname() + ":7676");
            ShortNodeMetricResponseDTO shortNodeMetricResponseDTO = webclientToPNA.post()
                    .uri("/api/v1/nodes/localNode/metric-requests")
                    .header("Authorization", "Basic " + getPnaTokenByNodeUUID(srcNode.getUuid()))
                    .bodyValue(createNewResourceUtilizationDTO)
                    .retrieve()
                    .bodyToMono(ShortNodeMetricResponseDTO.class)
                    .onErrorResume(error -> {
                        throw new RuntimeException(new MetricsServiceException("Can not create metric request!"));
                    })
                    .block();

            MetricRequest metricRequest = MetricRequest.fromShortNodeMetricResponseDTO(shortNodeMetricResponseDTO);
            // TODO: set link UUID to achieve an appropriate mapping in cloud
            metricRequest.setLinkUUID(srcNode.getUuid()); // global uuid
//        // TODO: do conversion to DTO and persist then in database
            MetricRequest savedMetricRequest = this.metricRequestRepository.save(metricRequest);
            // then send the request to the correct pna
            this.influxDBService.notifyAboutNewMetricRequest(savedMetricRequest);

            PulceoEvent pulceoEvent = PulceoEvent.builder()
                    .eventType(EventType.NODE_METRIC_REQUEST_CREATED)
                    .payload(savedMetricRequest.toString())
                    .build();
            this.eventHandler.handleEvent(pulceoEvent);

            lastMetricRequest = savedMetricRequest;
        }

        return lastMetricRequest;
    }

    public MetricRequest readMetricRequestByLinkUuidAndType(UUID linkUUID, String type) {
        return this.metricRequestRepository.findByLinkUUIDAndType(linkUUID, type).orElseThrow(() -> new ResourceNotFoundException("Metric request with linkUUID={} and type={}".formatted(linkUUID, type)));
    }

    private boolean checkIfMetricRequestsExists(UUID linkUUID, String type) {
        return this.metricRequestRepository.existsMetricRequestByLinkUUIDAndType(linkUUID, type);
    }

    public void deleteMetricRequest(UUID metricRequestUUID) throws InterruptedException {
        // TODO: delete metric request on PNA
        MetricRequest metricRequest = this.metricRequestRepository.findByUuid(metricRequestUUID);

        if (metricRequest.getType().equals("icmp-rtt") || metricRequest.getType().equals("tcp-rtt") ||
                metricRequest.getType().equals("udp-rtt") || metricRequest.getType().equals("tcp-bw") ||
                metricRequest.getType().equals("udp-bw")) {
            WebClient webClient = WebClient.create(this.prmEndpoint);
            NodeLinkDTO nodeLinkDTO = webClient.get()
                    .uri("/api/v1/links/" + metricRequest.getLinkUUID()) // on cloud
                    .retrieve()
                    .bodyToMono(NodeLinkDTO.class)
                    .onErrorResume(error -> {
                        throw new RuntimeException(new MetricsServiceException("Can not create metric request: Link with id %s does not exist!".formatted(metricRequest.getLinkUUID())));
                    })
                    .block();

            UUID srcNodeUUID = nodeLinkDTO.getSrcNodeUUID();
            NodeDTO srcNode = webClient.get()
                    .uri("/api/v1/nodes/" + srcNodeUUID)
                    .retrieve()
                    .bodyToMono(NodeDTO.class)
                    .onErrorResume(error -> {
                        throw new RuntimeException(new MetricsServiceException("Can not create link: Source node with id %s does not exist!".formatted(srcNodeUUID)));
                    })
                    .block();

            WebClient webClientToDestNode = WebClient.create(this.webClientScheme + "://" + srcNode.getHostname() + ":7676");
            webClientToDestNode.delete()
                    .uri("/api/v1/links/" + metricRequest.getRemoteLinkUUID() + "/metric-requests/" + metricRequest.getRemoteMetricRequestUUID())
                    .header("Authorization", "Basic " + getPnaTokenByNodeUUID(srcNode.getUuid()))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .onErrorResume(error -> {
                        throw new RuntimeException(new MetricsServiceException("Can not delete metric request!"));
                    })
                    .block();
        } else {
            WebClient webClient = WebClient.create(this.prmEndpoint);
            NodeDTO srcNode = webClient.get()
                    .uri("/api/v1/nodes/" + metricRequest.getLinkUUID())
                    .retrieve()
                    .bodyToMono(NodeDTO.class)
                    .onErrorResume(error -> {
                        throw new RuntimeException(new MetricsServiceException("Can not create link: Source node with id %s does not exist!".formatted(metricRequest.getLinkUUID())));
                    })
                    .block();

            WebClient webClientToPna = WebClient.create(this.webClientScheme + "://" + srcNode.getHostname() + ":7676");
            webClientToPna.delete()
                    .uri("/api/v1/nodes/localNode/metric-requests/" + metricRequest.getRemoteMetricRequestUUID())
                    .header("Authorization", "Basic " + getPnaTokenByNodeUUID(metricRequest.getLinkUUID())) // should be the same as the srcNodeUUID
                    .retrieve()
                    .bodyToMono(Void.class)
                    .onErrorResume(error -> {
                        throw new RuntimeException(new MetricsServiceException("Can not delete metric request!"));
                    })
                    .block();
        }

        PulceoEvent pulceoEvent = PulceoEvent.builder()
                .eventType(EventType.METRIC_REQUEST_DELETED)
                .payload(metricRequest.toString())
                .build();
        this.eventHandler.handleEvent(pulceoEvent);

        this.metricRequestRepository.delete(metricRequest);
    }

    public List<MetricRequest> readAllMetricRequests() {
        List<MetricRequest> metricRequests = new ArrayList<>();
        Iterable<MetricRequest> metricRequestIterable = this.metricRequestRepository.findAll();
        for (MetricRequest metricRequest : metricRequestIterable) {
            metricRequests.add(metricRequest);
        }
        return metricRequests;
    }

    public List<MetricRequest> readMetricRequestsByLinkUUID(UUID linkUUID) {
        List<MetricRequest> metricRequests = new ArrayList<>();
        Iterable<MetricRequest> metricRequestIterable = this.metricRequestRepository.findByLinkUUID(linkUUID);
        for (MetricRequest metricRequest : metricRequestIterable) {
            metricRequests.add(metricRequest);
        }
        return metricRequests;
    }

    public void reset() {
        this.metricRequestRepository.deleteAll();
    }

    // TODO: on startup inform InfluxDBService about all existing metric requests that are in DB

    // TODO: on shutdown inform InfluxDBService to stop all running metric requests

}
