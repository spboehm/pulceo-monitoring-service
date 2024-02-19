package dev.pulceo.pms.dto.metricrequests;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CreateNewMetricRequestCPUUtilDTO extends CreateNewAbstractMetricRequestDTO {
    private UUID nodeUUID;
    private String type;
    private String recurrence;
    private boolean enabled;

    public static CreateNewMetricRequestCPUUtilDTO fromAbstractMetricRequestDTO(CreateNewAbstractMetricRequestDTO createNewAbstractMetricRequestDTO) {
        CreateNewMetricRequestCPUUtilDTO createNewMetricRequestCPUUtilDTO = (CreateNewMetricRequestCPUUtilDTO) createNewAbstractMetricRequestDTO;
        return CreateNewMetricRequestCPUUtilDTO.builder()
                .nodeUUID(createNewMetricRequestCPUUtilDTO.getNodeUUID())
                .type(createNewMetricRequestCPUUtilDTO.getType())
                .recurrence(createNewMetricRequestCPUUtilDTO.getRecurrence())
                .enabled(createNewMetricRequestCPUUtilDTO.isEnabled())
                .build();
    }


}
