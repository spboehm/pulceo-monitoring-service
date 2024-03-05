package dev.pulceo.pms.model.metricrequests;


import dev.pulceo.pms.dto.metricrequests.CreateNewMetricRequestResourceUtilizationDTO;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@ToString
public class ResourceUtilizationMetricRequest extends AbstractMetricRequest {
    private String nodeId; // remote link UUID
    private String type;
    private String recurrence;
    private boolean enabled;

    public static ResourceUtilizationMetricRequest fromCreateNewMetricRequestResourceUtilizationDTO(CreateNewMetricRequestResourceUtilizationDTO createNewMetricRequestResourceUtilizationDTO) {
        return ResourceUtilizationMetricRequest.builder()
                .nodeId(createNewMetricRequestResourceUtilizationDTO.getNodeId())
                .type(createNewMetricRequestResourceUtilizationDTO.getType())
                .recurrence(createNewMetricRequestResourceUtilizationDTO.getRecurrence())
                .enabled(createNewMetricRequestResourceUtilizationDTO.isEnabled())
                .build();
    }

}
