package dev.pulceo.pms.dto.metricrequests.pna;

import dev.pulceo.pms.dto.metricrequests.CreateNewAbstractMetricRequestDTO;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CreateNewMetricRequestTcpUdpRttOnPNADTO extends CreateNewAbstractMetricRequestDTO {
    private UUID linkUUID;
    private String type;
    private String recurrence;
    private boolean enabled;
    // TODO: ipVersion
    @Builder.Default
    private int rounds = 10;

    public static CreateNewMetricRequestTcpUdpRttOnPNADTO fromAbstractMetricRequestDTO(CreateNewAbstractMetricRequestDTO createNewAbstractMetricRequestDTO) {
        CreateNewMetricRequestTcpUdpRttOnPNADTO createNewMetricRequestTcpBwDTO = (CreateNewMetricRequestTcpUdpRttOnPNADTO) createNewAbstractMetricRequestDTO;
        return CreateNewMetricRequestTcpUdpRttOnPNADTO.builder()
                .linkUUID(createNewMetricRequestTcpBwDTO.getLinkUUID())
                .type(createNewMetricRequestTcpBwDTO.getType())
                .recurrence(createNewMetricRequestTcpBwDTO.getRecurrence())
                .enabled(createNewMetricRequestTcpBwDTO.isEnabled())
                .rounds(createNewMetricRequestTcpBwDTO.getRounds())
                .build();
    }

}
