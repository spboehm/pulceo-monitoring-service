package dev.pulceo.pms.service;

import dev.pulceo.pms.api.PsmApi;
import dev.pulceo.pms.api.dto.orchestration.OrchestrationContextFromPsmDTO;
import dev.pulceo.pms.exception.OrchestrationContextException;
import dev.pulceo.pms.model.orchestration.ImmutableOrchestrationContext;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class OrchestrationContextService {

    private final Logger logger = LoggerFactory.getLogger(OrchestrationContextService.class);
    private final AtomicReference<ImmutableOrchestrationContext> orchestrationContext = new AtomicReference<>();
    private final PsmApi psmApi;
    @Value("${pulceo.data.dir}")
    private String pulceoDataDir;

    @Autowired
    public OrchestrationContextService(PsmApi psmApi) {
        this.psmApi = psmApi;
    }

    public ImmutableOrchestrationContext getOrchestrationContext() {
        return this.orchestrationContext.get();
    }

    public ImmutableOrchestrationContext updateOrchestrationContext(ImmutableOrchestrationContext orchestrationContext) {
        this.logger.info("Updating OrchestrationContext to uuid={}, name={}", orchestrationContext.getUuid(), orchestrationContext.getName());
        this.orchestrationContext.set(orchestrationContext);
        return this.orchestrationContext.get();
    }

    @PostConstruct
    public void init() throws OrchestrationContextException {
        // Initialize the orchestration context here if needed
        this.logger.info("Initializing Orchestration Context Service...");
        OrchestrationContextFromPsmDTO orchestrationContextFromPsmDTO = this.psmApi.getOrchestrationContext();
        this.orchestrationContext.set(ImmutableOrchestrationContext.fromOrchestrationContextFromPsmDTO(orchestrationContextFromPsmDTO));

        // TODO: ensure that pulceoDataDir exists
        this.createPulceoDataDirIfNotExists();
    }

    private void createPulceoDataDirIfNotExists() throws OrchestrationContextException {
        try {
            Files.createDirectories(Path.of(this.pulceoDataDir, "raw"));
            Files.createDirectories(Path.of(this.pulceoDataDir, "plots"));
            Files.createDirectories(Path.of(this.pulceoDataDir, "latex"));
            Files.createDirectories(Path.of(this.pulceoDataDir, "reports"));
            this.logger.info("PSM data directory {} created", this.pulceoDataDir);
        } catch (IOException e) {
            logger.error("Could not create Pulceo data directory", e);
            throw new OrchestrationContextException("Could not create Pulceo data directory", e);
        }
    }

}
