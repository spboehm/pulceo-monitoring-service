package dev.pulceo.pms.controller;

import dev.pulceo.pms.dto.metricrequests.CreateNewAbstractMetricRequestDTO;
import dev.pulceo.pms.dto.metricrequests.CreateNewMetricRequestIcmpRttDTO;
import dev.pulceo.pms.dto.metricrequests.MetricRequestDTOType;
import dev.pulceo.pms.dto.metricrequests.ShortMetricResponseDTO;
import dev.pulceo.pms.model.metricrequests.IcmpRttMetricRequest;
import dev.pulceo.pms.model.metricrequests.MetricRequest;
import dev.pulceo.pms.service.MetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

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

}
