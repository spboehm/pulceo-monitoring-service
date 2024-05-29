package dev.pulceo.pms.controller;

import dev.pulceo.pms.dto.metricrequests.*;
import dev.pulceo.pms.dto.metrics.NodeLinkMetricDTO;
import dev.pulceo.pms.exception.MetricsServiceException;
import dev.pulceo.pms.model.metric.NodeLinkMetric;
import dev.pulceo.pms.model.metricrequests.*;
import dev.pulceo.pms.service.MetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
public class MetricsController {

    private final MetricsService metricsService;

    @Autowired
    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    // TODO: remove
    @MessageMapping("/register")
    @SendTo("/metrics")
    public String greeting(Object message) throws Exception {
        return "Hello!";
    }

    @PostMapping("/api/v1/metric-requests")
    public ResponseEntity<ShortMetricResponseDTO> createNewMetricRequest(@RequestBody CreateNewAbstractMetricRequestDTO createNewAbstractMetricRequestDTO) throws MetricsServiceException, InterruptedException {
        // TODO: check type of metric request
        if (createNewAbstractMetricRequestDTO.getType().equals("cpu-util") || createNewAbstractMetricRequestDTO.getType().equals("mem-util") ||
                createNewAbstractMetricRequestDTO.getType().equals("storage-util") || createNewAbstractMetricRequestDTO.getType().equals("net-util")) {
            CreateNewMetricRequestResourceUtilizationDTO createNewMetricRequestResourceUtilizationDTO = CreateNewMetricRequestResourceUtilizationDTO.fromAbstractMetricRequestDTO(createNewAbstractMetricRequestDTO);
            MetricRequest metricRequest = this.metricsService.createNewResourceUtilizationRequest(ResourceUtilizationMetricRequest.fromCreateNewMetricRequestResourceUtilizationDTO(createNewMetricRequestResourceUtilizationDTO));
            return ResponseEntity.status(201).body(ShortMetricResponseDTO.fromMetricRequest(metricRequest));
        } else if (createNewAbstractMetricRequestDTO.getType().equals("icmp-rtt")) {
            CreateNewMetricRequestIcmpRttDTO createNewMetricRequestIcmpRttDTO = CreateNewMetricRequestIcmpRttDTO.fromAbstractMetricRequestDTO(createNewAbstractMetricRequestDTO);
            MetricRequest metricRequest = this.metricsService.createNewIcmpRttMetricRequest(IcmpRttMetricRequest.fromCreateNewMetricRequestIcmpRttDTO(createNewMetricRequestIcmpRttDTO));
            return ResponseEntity.status(201).body(ShortMetricResponseDTO.fromMetricRequest(metricRequest));
        } else if (createNewAbstractMetricRequestDTO.getType().equals("udp-rtt") || createNewAbstractMetricRequestDTO.getType().equals("tcp-rtt")) {
            CreateNewMetricRequestTcpUdpRttDTO createNewMetricRequestTcpUdpRttDTO = CreateNewMetricRequestTcpUdpRttDTO.fromAbstractMetricRequestDTO(createNewAbstractMetricRequestDTO);
            MetricRequest metricRequest = this.metricsService.createNewTcpUdpRttMetricRequest(TcpUdpRttMetricRequest.fromCreateNewMetricRequestTcpUdpRttDTO(createNewMetricRequestTcpUdpRttDTO));
            return ResponseEntity.status(201).body(ShortMetricResponseDTO.fromMetricRequest(metricRequest));
        } else if (createNewAbstractMetricRequestDTO.getType().equals("udp-bw") || createNewAbstractMetricRequestDTO.getType().equals("tcp-bw")) {
            CreateNewMetricRequestTcpBwDTO createNewMetricRequestTcpBwDTO = CreateNewMetricRequestTcpBwDTO.fromAbstractMetricRequestDTO(createNewAbstractMetricRequestDTO);
            MetricRequest metricRequest = this.metricsService.createNewTcpBwMetricRequest(TcpBwMetricRequest.fromCreateNewMetricRequestTcpBwDTO(createNewMetricRequestTcpBwDTO));
            return ResponseEntity.status(201).body(ShortMetricResponseDTO.fromMetricRequest(metricRequest));
        }
        return ResponseEntity.status(400).build();
    }

    @GetMapping("/api/v1/node-link-metrics/{linkUUID}")
    public ResponseEntity<List<NodeLinkMetricDTO>> readLinkMetricsByLinkUUID(@PathVariable UUID linkUUID) {
        List<NodeLinkMetric> nodeLinkMetric = this.metricsService.readLastNodeLinkMetricsByLinkUUIDAndMetricType(linkUUID);
        List<NodeLinkMetricDTO> listOfNodeLinkMetricDTO = new ArrayList<>();
        for (NodeLinkMetric metric : nodeLinkMetric) {
            listOfNodeLinkMetricDTO.add(NodeLinkMetricDTO.fromNodeLinkMetric(metric));
        }
        return ResponseEntity.status(200).body(listOfNodeLinkMetricDTO);
    }

    @GetMapping("/api/v1/metric-requests")
    public ResponseEntity<List<ShortMetricResponseDTO>> readAllMetricRequests(@RequestParam(value = "linkUUID", required = false) UUID linkUUID) {
        List<MetricRequest> metricRequests;
        if (linkUUID == null) {
            metricRequests = this.metricsService.readAllMetricRequests();
        } else {
            metricRequests = this.metricsService.readMetricRequestsByLinkUUID(linkUUID);

        }
        List<ShortMetricResponseDTO> listOfShortMetricResponseDTO = new ArrayList<>();
        for (MetricRequest metricRequest : metricRequests) {
            listOfShortMetricResponseDTO.add(ShortMetricResponseDTO.fromMetricRequest(metricRequest));
        }
        return ResponseEntity.status(200).body(listOfShortMetricResponseDTO);
    }

    @DeleteMapping("/api/v1/metric-requests/{metricRequestUUID}")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void deleteMetricRequest(@PathVariable UUID metricRequestUUID) throws InterruptedException {
        this.metricsService.deleteMetricRequest(metricRequestUUID);
    }
}
