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
public class CreateNewMetricRequestResourceUtilizationDTO extends CreateNewAbstractMetricRequestDTO {
    private UUID nodeUUID;
    private String type;
    private String recurrence;
    private boolean enabled;

    public static CreateNewMetricRequestResourceUtilizationDTO fromAbstractMetricRequestDTO(CreateNewAbstractMetricRequestDTO createNewAbstractMetricRequestDTO) {
        CreateNewMetricRequestResourceUtilizationDTO createNewMetricRequestResourceUtilizationDTO = (CreateNewMetricRequestResourceUtilizationDTO) createNewAbstractMetricRequestDTO;
        return CreateNewMetricRequestResourceUtilizationDTO.builder()
                .nodeUUID(createNewMetricRequestResourceUtilizationDTO.getNodeUUID())
                .type(createNewMetricRequestResourceUtilizationDTO.getType())
                .recurrence(createNewMetricRequestResourceUtilizationDTO.getRecurrence())
                .enabled(createNewMetricRequestResourceUtilizationDTO.isEnabled())
                .build();
    }


}
