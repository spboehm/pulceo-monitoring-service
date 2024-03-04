package dev.pulceo.pms.dto.metricrequests;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.geo.Metric;

import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CreateNewMetricRequestResourceUtilizationDTO extends CreateNewAbstractMetricRequestDTO {
    private UUID nodeUUID;
    private String recurrence;
    @Builder.Default
    private boolean enabled = true;

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
