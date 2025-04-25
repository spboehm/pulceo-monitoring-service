package dev.pulceo.pms.api;

import dev.pulceo.pms.api.dto.orchestration.OrchestrationContextDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class PsmApi {

    private final Logger logger = LoggerFactory.getLogger(PsmApi.class);
    @Value("${psm.endpoint}")
    private String psmEndpoint;
    private final WebClient webClient;
    private final String PSM_ORCHESTRATION_CONTEXT_API_BASE_PATH = "/api/v1/orchestration-context";

    @Autowired
    public PsmApi(WebClient webClient) {
        this.webClient = webClient;
    }

    public OrchestrationContextDTO getOrchestrationContext() {
        this.logger.info("Retrieving orchestration context from PSM");
        return webClient
                .get()
                .uri(this.psmEndpoint + this.PSM_ORCHESTRATION_CONTEXT_API_BASE_PATH)
                .retrieve()
                .bodyToMono(OrchestrationContextDTO.class)
                .doOnSuccess(orchestrationContextDTO -> {
                    this.logger.info("Successfully retrieved orchestration context from PSM: uuid={}, name={}", orchestrationContextDTO.getUuid(), orchestrationContextDTO.getName());
                })
                .onErrorResume(e -> {
                    this.logger.warn("Could not retrieve orchestration context from PSM...use default orchestration context", e);
                    return Mono.just(OrchestrationContextDTO.builder()
                                                            .uuid("00000000-0000-0000-0000-000000000000")
                                                            .name("default")
                                                            .build());
                })
                .block();
    }

}
