package dev.pulceo.pms.controller;

import dev.pulceo.pms.dto.metrics.ShortNodeLinkMetricDTO;
import dev.pulceo.pms.exception.MetricsQueryServiceException;
import dev.pulceo.pms.service.MetricsQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
public class MetricsQueryController {

    private final MetricsQueryService metricsQueryService;

    @Autowired
    public MetricsQueryController(MetricsQueryService metricsQueryService) {
        this.metricsQueryService = metricsQueryService;
    }

    @GetMapping("/api/v1/node-link-metrics")
    public ResponseEntity<List<ShortNodeLinkMetricDTO>> getNodeLinkMetrics(@RequestParam(defaultValue = "ICMP_RTT") String measurement, @RequestParam(defaultValue = "limit(n:10)") String aggregation) throws MetricsQueryServiceException {
        List<ShortNodeLinkMetricDTO> shortNodeLinkMetricDTO;
        switch (aggregation) {
            case "min", "max":
                shortNodeLinkMetricDTO = metricsQueryService.queryRangeNodeLinkMetrics(measurement, aggregation + "()");
                break;
            case "mean", "median":
                shortNodeLinkMetricDTO = List.of(ShortNodeLinkMetricDTO.builder().build());
                break;
            default:
                if (aggregation.startsWith("limit")) {
                    shortNodeLinkMetricDTO = metricsQueryService.queryRangeNodeLinkMetrics(measurement, aggregation);
                    break;
                } else {
                    throw new MetricsQueryServiceException("Invalid aggregation type");
                }
        }
        return ResponseEntity.status(200).body(shortNodeLinkMetricDTO);
    }

    @GetMapping("/api/v1/metrics")
    public ResponseEntity<List<ShortNodeLinkMetricDTO>> getMetrics(@RequestParam(defaultValue = "link") String type, @RequestParam(defaultValue = "limit(n:1)") String aggregation) throws MetricsQueryServiceException {
        List<ShortNodeLinkMetricDTO> shortNodeLinkMetricDTO = new ArrayList<>();
        switch (aggregation) {
            case "min", "max":
                for (String measurement : List.of("ICMP_RTT", "TCP_BW", "UDP_BW")) {
                   shortNodeLinkMetricDTO.addAll(metricsQueryService.queryRangeNodeLinkMetrics(measurement, aggregation + "()"));
                }
                break;
            case "mean", "median":
                for (String measurement : List.of("ICMP_RTT", "TCP_BW", "UDP_BW")) {
                    shortNodeLinkMetricDTO.addAll(metricsQueryService.queryRangeNodeLinkMetrics(measurement, aggregation));
                }
                break;
            default:
                if (aggregation.startsWith("limit")) {
                    for (String measurement : List.of("ICMP_RTT", "TCP_BW", "UDP_BW")) {
                        shortNodeLinkMetricDTO.addAll(metricsQueryService.queryRangeNodeLinkMetrics(measurement, aggregation));
                    }
                    break;
                } else {
                    throw new MetricsQueryServiceException("Invalid aggregation type");
                }
        }
        return ResponseEntity.status(200).body(shortNodeLinkMetricDTO);
    }

    @ExceptionHandler(value = MetricsQueryServiceException.class)
    public ResponseEntity<CustomErrorResponse> handleCloudRegistrationException(MetricsQueryServiceException nodeServiceException) {
        CustomErrorResponse error = new CustomErrorResponse("BAD_REQUEST", nodeServiceException.getMessage());
        error.setStatus(HttpStatus.BAD_REQUEST.value());
        error.setErrorMsg(nodeServiceException.getMessage());
        error.setTimestamp(LocalDateTime.now());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

}
