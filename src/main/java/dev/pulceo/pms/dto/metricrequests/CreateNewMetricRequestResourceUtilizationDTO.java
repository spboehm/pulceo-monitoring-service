package dev.pulceo.pms.dto.metricrequests;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CreateNewMetricRequestResourceUtilizationDTO extends CreateNewAbstractMetricRequestDTO {
    private String resourceId;
    @Builder.Default
    private String resourceType = "node";
    private String recurrence;
    @Builder.Default
    private boolean enabled = true;

    public static CreateNewMetricRequestResourceUtilizationDTO fromAbstractMetricRequestDTO(CreateNewAbstractMetricRequestDTO createNewAbstractMetricRequestDTO) {
        CreateNewMetricRequestResourceUtilizationDTO createNewMetricRequestResourceUtilizationDTO = (CreateNewMetricRequestResourceUtilizationDTO) createNewAbstractMetricRequestDTO;
        return CreateNewMetricRequestResourceUtilizationDTO.builder()
                .resourceId(createNewMetricRequestResourceUtilizationDTO.getResourceId())
                .resourceType(createNewMetricRequestResourceUtilizationDTO.getResourceType())
                .type(createNewMetricRequestResourceUtilizationDTO.getType())
                .recurrence(createNewMetricRequestResourceUtilizationDTO.getRecurrence())
                .enabled(createNewMetricRequestResourceUtilizationDTO.isEnabled())
                .build();
    }


}
