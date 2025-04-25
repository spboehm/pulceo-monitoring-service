package dev.pulceo.pms.controller;

import dev.pulceo.pms.dto.orchestration.OrchestrationContextDTO;
import dev.pulceo.pms.dto.orchestration.UpdateOrchestrationContextDTO;
import dev.pulceo.pms.model.orchestration.ImmutableOrchestrationContext;
import dev.pulceo.pms.service.OrchestrationContextService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orchestration-context")
public class OrchestrationContextController {

    private final OrchestrationContextService orchestrationContextService;

    @Autowired
    public OrchestrationContextController(OrchestrationContextService orchestrationContextService) {
        this.orchestrationContextService = orchestrationContextService;
    }

    @GetMapping
    public ResponseEntity<OrchestrationContextDTO> readOrchestrationContext() {
        return ResponseEntity.ok(OrchestrationContextDTO.fromOrchestrationContext(orchestrationContextService.getOrchestrationContext()));
    }

    @PutMapping
    public ResponseEntity<OrchestrationContextDTO> updateOrchestrationContext(@RequestBody UpdateOrchestrationContextDTO fromUpdateUpdateOrchestrationContextDTO) {
        ImmutableOrchestrationContext updatedOrchestrationContext = this.orchestrationContextService.updateOrchestrationContext(ImmutableOrchestrationContext.fromUpdateUpdateOrchestrationContextDTO(fromUpdateUpdateOrchestrationContextDTO));
        return ResponseEntity.ok(OrchestrationContextDTO.fromOrchestrationContext(updatedOrchestrationContext));
    }

}
