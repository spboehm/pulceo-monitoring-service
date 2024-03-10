package dev.pulceo.pms.dto.application;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Data
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ApplicationComponentDTO {

    private String applicationComponentUUID;
    private String name;
    private String endpoint;
    private String image;
    private int port;
    private String protocol;
    private ApplicationComponentType applicationComponentType;
//    @Builder.Default
//    private Map<String, String> environmentVariables = new HashMap<>();
}
