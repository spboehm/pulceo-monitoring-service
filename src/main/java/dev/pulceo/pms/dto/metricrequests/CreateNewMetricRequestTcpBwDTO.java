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
    private String linkId;
    private String recurrence;
    @Builder.Default
    private boolean enabled = true;
    // ignored by svc
    @Builder.Default
    private long port = 5000;
    @Builder.Default
    private int bitrate = 50;
    @Builder.Default
    private int time = 5;
    @Builder.Default
    private String strategy = "random"; // ordered, random

    public static CreateNewMetricRequestTcpBwDTO fromAbstractMetricRequestDTO(CreateNewAbstractMetricRequestDTO createNewAbstractMetricRequestDTO) {
        CreateNewMetricRequestTcpBwDTO createNewMetricRequestTcpBwDTO = (CreateNewMetricRequestTcpBwDTO) createNewAbstractMetricRequestDTO;
        return CreateNewMetricRequestTcpBwDTO.builder()
                .linkId(createNewMetricRequestTcpBwDTO.getLinkId())
                .type(createNewMetricRequestTcpBwDTO.getType())
                .recurrence(createNewMetricRequestTcpBwDTO.getRecurrence())
                .enabled(createNewMetricRequestTcpBwDTO.isEnabled())
                .port(createNewMetricRequestTcpBwDTO.getPort())
                .bitrate(createNewMetricRequestTcpBwDTO.getBitrate())
                .time(createNewMetricRequestTcpBwDTO.getTime())
                .strategy(createNewMetricRequestTcpBwDTO.getStrategy())
                .build();
    }

}
