package dev.pulceo.pms.controller;

import dev.pulceo.pms.dto.orchestration.OrchestrationContextDTO;
import dev.pulceo.pms.dto.orchestration.UpdateOrchestrationContextDTO;
import dev.pulceo.pms.model.orchestration.ImmutableOrchestrationContext;
import dev.pulceo.pms.service.InfluxDBService;
import dev.pulceo.pms.service.MetricsQueryService;
import dev.pulceo.pms.service.MetricsService;
import dev.pulceo.pms.service.OrchestrationContextService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orchestration-context")
public class OrchestrationContextController {

    private final OrchestrationContextService orchestrationContextService;
    private final InfluxDBService influxDBService;
    private final MetricsQueryService metricsQueryService;
    private final MetricsService metricsService;

    public OrchestrationContextController(OrchestrationContextService orchestrationContextService, InfluxDBService influxDBService, MetricsQueryService metricsQueryService, MetricsService metricsService) {
        this.orchestrationContextService = orchestrationContextService;
        this.influxDBService = influxDBService;
        this.metricsQueryService = metricsQueryService;
        this.metricsService = metricsService;
    }

    @Autowired


    @GetMapping
    public ResponseEntity<OrchestrationContextDTO> readOrchestrationContext() {
        return ResponseEntity.ok(OrchestrationContextDTO.fromOrchestrationContext(orchestrationContextService.getOrchestrationContext()));
    }

    @PostMapping("/reset")
    public void deleteOrchestrationContext() {
        this.influxDBService.reset();
        this.metricsQueryService.reset();
        this.metricsService.reset();
    }

    @PutMapping
    public ResponseEntity<OrchestrationContextDTO> updateOrchestrationContext(@RequestBody UpdateOrchestrationContextDTO fromUpdateUpdateOrchestrationContextDTO) {
        ImmutableOrchestrationContext updatedOrchestrationContext = this.orchestrationContextService.updateOrchestrationContext(ImmutableOrchestrationContext.fromUpdateUpdateOrchestrationContextDTO(fromUpdateUpdateOrchestrationContextDTO));
        return ResponseEntity.ok(OrchestrationContextDTO.fromOrchestrationContext(updatedOrchestrationContext));
    }

}
