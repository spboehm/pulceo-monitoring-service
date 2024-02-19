package dev.pulceo.pms.dto.metricrequests.pna;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateNewResourceUtilizationDTO {
    private String type;
    private int recurrence;
    private boolean enabled;
}
