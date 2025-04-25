package dev.pulceo.pms.api.dto.orchestration;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Data
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class OrchestrationContextFromPsmDTO {

    private String uuid;
    private String name;

}
