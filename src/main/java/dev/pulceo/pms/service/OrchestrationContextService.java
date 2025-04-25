package dev.pulceo.pms.service;

import dev.pulceo.pms.api.PsmApi;
import dev.pulceo.pms.api.dto.orchestration.OrchestrationContextDTO;
import dev.pulceo.pms.model.orchestration.ImmutableOrchestrationContext;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicReference;

@Service
public class OrchestrationContextService {

    private final Logger logger = LoggerFactory.getLogger(OrchestrationContextService.class);
    private final AtomicReference<ImmutableOrchestrationContext> orchestrationContext = new AtomicReference<>();
    private final PsmApi psmApi;

    @Autowired
    public OrchestrationContextService(PsmApi psmApi) {
        this.psmApi = psmApi;
    }

    public ImmutableOrchestrationContext getOrchestrationContext() {
        return this.orchestrationContext.get();
    }

    public void setOrchestrationContext(ImmutableOrchestrationContext orchestrationContext) {
        this.orchestrationContext.set(orchestrationContext);
    }

    @PostConstruct
    public void init() {
        // Initialize the orchestration context here if needed
        this.logger.info("Initializing Orchestration Context Service");
        OrchestrationContextDTO orchestrationContextDTO = this.psmApi.getOrchestrationContext();
        this.orchestrationContext.set(ImmutableOrchestrationContext.fromOrchestrationContextDTO(orchestrationContextDTO));
    }

}
