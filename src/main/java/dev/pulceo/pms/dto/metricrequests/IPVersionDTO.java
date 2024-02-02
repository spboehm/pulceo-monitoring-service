package dev.pulceo.pms.dto.metricrequests;

public enum IPVersionDTO {
    IPv4(4), IPv6(6);

    public final int label;

    private IPVersionDTO(int label) {
        this.label = label;
    }
}
