package dev.pulceo.pms.model.metricrequests;

import dev.pulceo.pms.dto.metricrequests.CreateNewMetricRequestTcpBwDTO;
import jakarta.persistence.Entity;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
public class TcpBwMetricRequest extends AbstractMetricRequest {
    private String linkId; // remote link UUID
    private String type;
    private String recurrence;
    private boolean enabled;
    private long port;
    @Builder.Default
    private int bitrate = 0;
    @Builder.Default
    private int time = 10;
    @Builder.Default
    private String strategy = "random"; // ordered, random

    public static TcpBwMetricRequest fromCreateNewMetricRequestTcpBwDTO(CreateNewMetricRequestTcpBwDTO createNewMetricRequestTcpBwDTO) {
        return TcpBwMetricRequest.builder()
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
