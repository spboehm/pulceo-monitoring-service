package dev.pulceo.pms.controller;

import dev.pulceo.pms.dto.metricrequests.CreateNewAbstractMetricRequestDTO;
import dev.pulceo.pms.dto.metricrequests.CreateNewMetricRequestIcmpRttDTO;
import dev.pulceo.pms.dto.metricrequests.MetricRequestDTOType;
import dev.pulceo.pms.dto.metricrequests.ShortMetricResponseDTO;
import dev.pulceo.pms.dto.metrics.NodeLinkMetricDTO;
import dev.pulceo.pms.model.metric.NodeLinkMetric;
import dev.pulceo.pms.model.metricrequests.IcmpRttMetricRequest;
import dev.pulceo.pms.model.metricrequests.MetricRequest;
import dev.pulceo.pms.service.MetricsService;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
public class MetricsController {

    private final MetricsService metricsService;

    @Autowired
    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @MessageMapping("/register")
    @SendTo("/metrics")
    public String greeting(Object message) throws Exception {
        return "Hello!";
    }

    @PostMapping("/api/v1/metric-requests")
    public ResponseEntity<ShortMetricResponseDTO> createNewMetricRequest(@RequestBody CreateNewAbstractMetricRequestDTO createNewAbstractMetricRequestDTO) {
        // TODO: check type of metric request
        if (createNewAbstractMetricRequestDTO.getMetricRequestDTOType() == MetricRequestDTOType.ICMP_RTT) {
            CreateNewMetricRequestIcmpRttDTO createNewMetricRequestIcmpRttDTO = CreateNewMetricRequestIcmpRttDTO.fromAbstractMetricRequestDTO(createNewAbstractMetricRequestDTO);
            MetricRequest metricRequest = this.metricsService.createNewIcmpRttMetricRequest(IcmpRttMetricRequest.fromCreateNewMetricRequestIcmpRttDTO(createNewMetricRequestIcmpRttDTO));
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
}
