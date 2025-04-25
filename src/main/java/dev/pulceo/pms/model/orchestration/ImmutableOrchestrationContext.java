package dev.pulceo.pms.model.orchestration;


import dev.pulceo.pms.api.dto.orchestration.OrchestrationContextDTO;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class ImmutableOrchestrationContext {

    String uuid;
    String name;

    public static ImmutableOrchestrationContext fromOrchestrationContextDTO(OrchestrationContextDTO orchestrationContextDTO) {
        return ImmutableOrchestrationContext.builder()
                .uuid(orchestrationContextDTO.getUuid())
                .name(orchestrationContextDTO.getName())
                .build();
    }

}
