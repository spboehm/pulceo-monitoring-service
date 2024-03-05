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
public class CreateNewMetricRequestTcpUdpRttDTO extends CreateNewAbstractMetricRequestDTO {
    private String linkId;
    private String recurrence;
    private boolean enabled;
    @Builder.Default
    private int rounds = 10;

    public static CreateNewMetricRequestTcpUdpRttDTO fromAbstractMetricRequestDTO(CreateNewAbstractMetricRequestDTO createNewAbstractMetricRequestDTO) {
        CreateNewMetricRequestTcpUdpRttDTO createNewMetricRequestTcpUdpRttDTO = (CreateNewMetricRequestTcpUdpRttDTO) createNewAbstractMetricRequestDTO;
        return CreateNewMetricRequestTcpUdpRttDTO.builder()
                .type(createNewMetricRequestTcpUdpRttDTO.getType())
                .linkId(createNewMetricRequestTcpUdpRttDTO.getLinkId())
                .recurrence(createNewMetricRequestTcpUdpRttDTO.getRecurrence())
                .enabled(createNewMetricRequestTcpUdpRttDTO.isEnabled())
                .rounds(createNewMetricRequestTcpUdpRttDTO.getRounds())
                .build();
    }

}
