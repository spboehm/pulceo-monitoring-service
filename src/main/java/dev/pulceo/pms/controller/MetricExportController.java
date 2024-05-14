package dev.pulceo.pms.controller;

import dev.pulceo.pms.dto.metricexports.MetricExportDTO;
import dev.pulceo.pms.dto.metricexports.MetricExportRequestDTO;
import dev.pulceo.pms.exception.MetricsQueryServiceException;
import dev.pulceo.pms.model.metricexports.MetricExport;
import dev.pulceo.pms.model.metricexports.MetricExportRequest;
import dev.pulceo.pms.service.MetricsQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/metric-exports")
public class MetricExportController {

    private final Logger logger = LoggerFactory.getLogger(MetricExportController.class);

    private final MetricsQueryService metricsQueryService;

    public MetricExportController(MetricsQueryService metricsQueryService) {
        this.metricsQueryService = metricsQueryService;
    }

    @PostMapping("")
    public ResponseEntity<MetricExportDTO> createMetricExport(@RequestBody MetricExportRequestDTO metricExportRequestDTO) throws MetricsQueryServiceException {
        this.logger.info("Received request to create a new metric export");
        MetricExport metricExport = this.metricsQueryService.createMetricExport(MetricExportRequest.fromMetricExportRequestDTO(metricExportRequestDTO));
        return ResponseEntity.status(201).body(MetricExportDTO.fromMetricExportDTO(metricExport));
    }

    @GetMapping("")
    public ResponseEntity<List<MetricExportDTO>> readMetricExports() {
        List<MetricExport> metricExports = this.metricsQueryService.readAllMetricExports();
        List<MetricExportDTO> metricExportDTOs = new ArrayList<>();
        for (MetricExport metricExport : metricExports) {
            metricExportDTOs.add(MetricExportDTO.fromMetricExportDTO(metricExport));
        }
        return ResponseEntity.status(200).body(metricExportDTOs);
    }

    @ExceptionHandler(value = MetricsQueryServiceException.class)
    public ResponseEntity<CustomErrorResponse> handleCloudRegistrationException(MetricsQueryServiceException metricsQueryServiceException) {
        CustomErrorResponse error = new CustomErrorResponse("BAD_REQUEST", metricsQueryServiceException.getMessage());
        error.setStatus(HttpStatus.BAD_REQUEST.value());
        error.setErrorMsg(metricsQueryServiceException.getMessage());
        error.setTimestamp(LocalDateTime.now());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

}
