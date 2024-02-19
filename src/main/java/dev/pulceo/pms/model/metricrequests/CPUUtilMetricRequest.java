package dev.pulceo.pms.model.metricrequests;


import dev.pulceo.pms.dto.metricrequests.CreateNewMetricRequestCPUUtilDTO;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.builder.ToStringExclude;

import java.util.UUID;

@Entity
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@ToString
public class CPUUtilMetricRequest extends AbstractMetricRequest {
    private UUID nodeUUID; // remote link UUID
    private String type;
    private String recurrence;
    private boolean enabled;

    public static CPUUtilMetricRequest fromCreateNewMetricRequestCPUUtilDTO(CreateNewMetricRequestCPUUtilDTO createNewMetricRequestCPUUtilDTO) {
        return CPUUtilMetricRequest.builder()
                .nodeUUID(createNewMetricRequestCPUUtilDTO.getNodeUUID())
                .type(createNewMetricRequestCPUUtilDTO.getType())
                .recurrence(createNewMetricRequestCPUUtilDTO.getRecurrence())
                .enabled(createNewMetricRequestCPUUtilDTO.isEnabled())
                .build();
    }

}
