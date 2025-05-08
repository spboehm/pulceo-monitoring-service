package dev.pulceo.pms.controller;

import dev.pulceo.pms.dto.metricexports.MetricExportDTO;
import dev.pulceo.pms.dto.metricexports.MetricExportRequestDTO;
import dev.pulceo.pms.exception.MetricsQueryServiceException;
import dev.pulceo.pms.model.metricexports.MetricExport;
import dev.pulceo.pms.model.metricexports.MetricExportRequest;
import dev.pulceo.pms.model.metricexports.MetricExportState;
import dev.pulceo.pms.service.MetricsQueryService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/metric-exports")
public class MetricExportController {

    private final Logger logger = LoggerFactory.getLogger(MetricExportController.class);

    private final MetricsQueryService metricsQueryService;

    @Value("${pms.data.dir}")
    private String pmsDataDir;

    public MetricExportController(MetricsQueryService metricsQueryService) {
        this.metricsQueryService = metricsQueryService;
    }

    @PostMapping("")
    public ResponseEntity<MetricExportDTO> createMetricExport(@RequestBody @Valid MetricExportRequestDTO metricExportRequestDTO) throws MetricsQueryServiceException {
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

    @GetMapping("/{metricExportUuid}")
    public ResponseEntity<MetricExportDTO> readMetricExportById(@PathVariable UUID metricExportUuid) throws MetricsQueryServiceException {
        Optional<MetricExport> metricExport = this.metricsQueryService.readMetricExportByUuid(metricExportUuid);
        if (metricExport.isEmpty()) {
            throw new MetricsQueryServiceException("Metric export not found");
        }
        return ResponseEntity.status(200).body(MetricExportDTO.fromMetricExportDTO(metricExport.get()));
    }

    @GetMapping(value = "/{metricExportUuid}/blobs/{filename}")
    public ResponseEntity<Object> downloadExportedMetrics(@PathVariable UUID metricExportUuid, @PathVariable String filename) throws MetricsQueryServiceException {
        this.logger.info("Received request to download exported metric export with UUID: {} and filename: {}", metricExportUuid, filename);
        Optional<MetricExport> metricExport = this.metricsQueryService.readMetricExportByUuid(metricExportUuid);
        if (metricExport.isEmpty()) {
            throw new MetricsQueryServiceException("Metric export not found");
        }

        // TODO: move logic to service
        if (metricExport.get().getMetricExportState() == MetricExportState.COMPLETED) {
            // TODO: check if filename does exist, if not throw exception, this is going to be a list later
            if (!metricExport.get().getFilename().equals(filename)) {
                throw new MetricsQueryServiceException("Requested resource does not exist!");
            }
            // in case of everything is ok, return the file
            FileSystemResource resource = new FileSystemResource(pmsDataDir + "/" + filename);
            MediaType mediaType = MediaType.parseMediaType("application/octet-stream");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(mediaType);
            ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
                    .filename(resource.getFilename())
                    .build();
            headers.setContentDisposition(contentDisposition);
            return new ResponseEntity<>(resource, headers, HttpStatus.OK);
        } else if (metricExport.get().getMetricExportState() == MetricExportState.FAILED) {
            throw new MetricsQueryServiceException("Metric export failed...retry!");
        } else {
            // TODO: 202
            throw new MetricsQueryServiceException("Metric export still in progress...retry!");
        }
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
