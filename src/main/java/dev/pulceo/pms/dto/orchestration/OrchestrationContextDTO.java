package dev.pulceo.pms.dto.orchestration;

import dev.pulceo.pms.model.orchestration.ImmutableOrchestrationContext;
import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@Getter
@Setter
@NoArgsConstructor
public class OrchestrationContextDTO {

    @Builder.Default
    private String service = "pms";
    private String uuid;
    private String name;

    public static OrchestrationContextDTO fromOrchestrationContext(ImmutableOrchestrationContext orchestrationContext) {
        return OrchestrationContextDTO.builder()
                .uuid(orchestrationContext.getUuid())
                .name(orchestrationContext.getName())
                .build();
    }
}
