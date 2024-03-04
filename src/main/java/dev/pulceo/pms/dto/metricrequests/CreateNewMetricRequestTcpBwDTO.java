package dev.pulceo.pms.dto.metricrequests;

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
public class CreateNewMetricRequestTcpBwDTO extends CreateNewAbstractMetricRequestDTO {
    private UUID linkUUID;
    private String recurrence;
    private boolean enabled;
    // ignored by svc
    @Builder.Default
    private long port = 5000;
    @Builder.Default
    private int bitrate = 0;
    @Builder.Default
    private int time = 10;

    public static CreateNewMetricRequestTcpBwDTO fromAbstractMetricRequestDTO(CreateNewAbstractMetricRequestDTO createNewAbstractMetricRequestDTO) {
        CreateNewMetricRequestTcpBwDTO createNewMetricRequestTcpBwDTO = (CreateNewMetricRequestTcpBwDTO) createNewAbstractMetricRequestDTO;
        return CreateNewMetricRequestTcpBwDTO.builder()
                .linkUUID(createNewMetricRequestTcpBwDTO.getLinkUUID())
                .type(createNewMetricRequestTcpBwDTO.getType())
                .recurrence(createNewMetricRequestTcpBwDTO.getRecurrence())
                .enabled(createNewMetricRequestTcpBwDTO.isEnabled())
                .port(createNewMetricRequestTcpBwDTO.getPort())
                .bitrate(createNewMetricRequestTcpBwDTO.getBitrate())
                .time(createNewMetricRequestTcpBwDTO.getTime())
                .build();
    }

}
