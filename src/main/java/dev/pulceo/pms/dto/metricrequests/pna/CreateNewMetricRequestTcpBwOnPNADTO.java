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
public class CreateNewMetricRequestTcpBwOnPNADTO extends CreateNewAbstractMetricRequestDTO {
    private UUID linkUUID;
    private String recurrence;
    private boolean enabled;
    // ignored by svc
    private long port;
    @Builder.Default
    private int bitrate = 0;
    @Builder.Default
    private int time = 10;
    @Builder.Default
    private int initialDelay = 0; // ordered, random

    public static CreateNewMetricRequestTcpBwOnPNADTO fromAbstractMetricRequestDTO(CreateNewAbstractMetricRequestDTO createNewAbstractMetricRequestDTO) {
        CreateNewMetricRequestTcpBwOnPNADTO createNewMetricRequestTcpBwDTO = (CreateNewMetricRequestTcpBwOnPNADTO) createNewAbstractMetricRequestDTO;
        return CreateNewMetricRequestTcpBwOnPNADTO.builder()
                .linkUUID(createNewMetricRequestTcpBwDTO.getLinkUUID())
                .type(createNewMetricRequestTcpBwDTO.getType())
                .recurrence(createNewMetricRequestTcpBwDTO.getRecurrence())
                .enabled(createNewMetricRequestTcpBwDTO.isEnabled())
                .port(createNewMetricRequestTcpBwDTO.getPort())
                .bitrate(createNewMetricRequestTcpBwDTO.getBitrate())
                .time(createNewMetricRequestTcpBwDTO.getTime())
                .initialDelay(createNewMetricRequestTcpBwDTO.getInitialDelay())
                .build();
    }

}
