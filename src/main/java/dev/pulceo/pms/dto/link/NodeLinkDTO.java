package dev.pulceo.pms.dto.link;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NodeLinkDTO extends AbstractLinkDTO {
    private String linkUUID;
    private UUID remoteNodeLinkUUID;
    private String name;
    private UUID srcNodeUUID;
    private UUID destNodeUUID;
}
