package dev.pulceo.pms.dto.orchestration;

import dev.pulceo.pms.model.orchestration.ImmutableOrchestrationContext;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@Getter
@Setter
@NoArgsConstructor
public class UpdateOrchestrationContextDTO {

    private String uuid;
    private String name;

    public static OrchestrationContextDTO fromOrchestrationContext(ImmutableOrchestrationContext orchestrationContext) {
        return OrchestrationContextDTO.builder()
                .uuid(orchestrationContext.getUuid())
                .name(orchestrationContext.getName())
                .build();
    }

}
