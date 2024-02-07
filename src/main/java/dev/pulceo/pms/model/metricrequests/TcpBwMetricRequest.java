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
    private UUID linkUUID; // remote link UUID
    private String type;
    private String recurrence;
    private boolean enabled;
    private long port;
    @Builder.Default
    private int bitrate = 0;
    @Builder.Default
    private int time = 10;

    public static TcpBwMetricRequest fromCreateNewMetricRequestTcpBwDTO(CreateNewMetricRequestTcpBwDTO createNewMetricRequestTcpBwDTO) {
        return TcpBwMetricRequest.builder()
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
