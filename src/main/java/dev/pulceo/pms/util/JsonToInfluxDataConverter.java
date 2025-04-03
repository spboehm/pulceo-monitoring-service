package dev.pulceo.pms.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.influxdb.client.write.Point;

import java.util.ArrayList;
import java.util.List;

public class JsonToInfluxDataConverter {

    // safe
    private static final ObjectMapper mapper = new ObjectMapper();

    public static List<Point> convertMetric(String json) throws JsonProcessingException {
        JsonNode jsonNode = mapper.readTree(json);
        String type = jsonNode.get("metric").get("metricType").asText();
        switch(type) {
            case "CPU_UTIL":
                return convertCPUUtilMetric(jsonNode);
            case "MEM_UTIL":
                return convertMemUtilMetric(jsonNode);
            case "NET_UTIL":
                return convertNetUtilMetric(jsonNode);
            case "STORAGE_UTIL":
                return convertStorageUtilMetric(jsonNode);
            case "ICMP_RTT":
                return convertICMPRTTMetric(jsonNode);
            case "TCP_RTT":
                return convertTCPRTTMetric(jsonNode);
            case "UDP_RTT":
                return convertUDPRTTMetric(jsonNode);
            case "TCP_BW":
                return convertTCPBWMetric(jsonNode);
            case "UDP_BW":
                return convertUDPBWMetric(jsonNode);
            case "EVENT":
                return convertEvent(json);
            default:
                throw new IllegalArgumentException("Unknown metric type: " + type);
        }
    }

    private static List<Point> convertCPUUtilMetric(JsonNode jsonNode) {
        Point cpuUtilMeasurement = new Point("CPU_UTIL");
        addNodeMetricDataAsTags(jsonNode, cpuUtilMeasurement);
        cpuUtilMeasurement.addField("usageNanoCores", jsonNode.get("metric").get("metricResult").get("cpuUtilizationMeasurement").get("usageNanoCores").asLong());
        cpuUtilMeasurement.addField("usageCoreNanoSeconds", jsonNode.get("metric").get("metricResult").get("cpuUtilizationMeasurement").get("usageCoreNanoSeconds").asLong());
        cpuUtilMeasurement.addField("usageCPUPercentage", jsonNode.get("metric").get("metricResult").get("cpuUtilizationMeasurement").get("usageCPUPercentage").asDouble());
        return new ArrayList<>(List.of(cpuUtilMeasurement));
    }

    private static List<Point> convertMemUtilMetric(JsonNode jsonNode) {
        Point memUtilMeasurement = new Point("MEM_UTIL");
        addNodeMetricDataAsTags(jsonNode, memUtilMeasurement);
        memUtilMeasurement.addField("usageBytes", jsonNode.get("metric").get("metricResult").get("memoryUtilizationMeasurement").get("usageBytes").asLong());
        memUtilMeasurement.addField("availableBytes", jsonNode.get("metric").get("metricResult").get("memoryUtilizationMeasurement").get("availableBytes").asLong());
        memUtilMeasurement.addField("usageMemoryPercentage", jsonNode.get("metric").get("metricResult").get("memoryUtilizationMeasurement").get("usageMemoryPercentage").asDouble());
        return new ArrayList<>(List.of(memUtilMeasurement));
    }

    private static List<Point> convertNetUtilMetric(JsonNode jsonNode) {
        Point netUtilMeasurement = new Point("NET_UTIL");
        addNodeMetricDataAsTags(jsonNode, netUtilMeasurement);
        netUtilMeasurement.addTag("iface", jsonNode.get("metric").get("metricResult").get("networkUtilizationMeasurement").get("iface").asText());
        netUtilMeasurement.addField("rxBytes", jsonNode.get("metric").get("metricResult").get("networkUtilizationMeasurement").get("rxBytes").asLong());
        netUtilMeasurement.addField("txBytes", jsonNode.get("metric").get("metricResult").get("networkUtilizationMeasurement").get("txBytes").asLong());
        return new ArrayList<>(List.of(netUtilMeasurement));
    }

    private static List<Point> convertStorageUtilMetric(JsonNode jsonNode) {
        Point storageUtilMeasurement = new Point("STORAGE_UTIL");
        addNodeMetricDataAsTags(jsonNode, storageUtilMeasurement);
        storageUtilMeasurement.addField("usageBytes", jsonNode.get("metric").get("metricResult").get("storageUtilizationMeasurement").get("usedBytes").asLong());
        storageUtilMeasurement.addField("availableBytes", jsonNode.get("metric").get("metricResult").get("storageUtilizationMeasurement").get("capacityBytes").asLong());
        storageUtilMeasurement.addField("usageStoragePercentage", jsonNode.get("metric").get("metricResult").get("storageUtilizationMeasurement").get("usageStoragePercentage").asDouble());
        return new ArrayList<>(List.of(storageUtilMeasurement));
    }

    private static List<Point> convertICMPRTTMetric(JsonNode jsonNode) {
        Point pingDelayMeasurement = new Point("ICMP_RTT");
        addNetworkMetricDataAsTags(jsonNode, pingDelayMeasurement);
        pingDelayMeasurement.addField("packetsTransmitted", jsonNode.get("metric").get("metricResult").get("pingDelayMeasurement").get("packetsTransmitted").asInt());
        pingDelayMeasurement.addField("packetsReceived", jsonNode.get("metric").get("metricResult").get("pingDelayMeasurement").get("packetsReceived").asInt());
        pingDelayMeasurement.addField("packetLoss", jsonNode.get("metric").get("metricResult").get("pingDelayMeasurement").get("packetLoss").asDouble());
        pingDelayMeasurement.addField("time", jsonNode.get("metric").get("metricResult").get("pingDelayMeasurement").get("time").asInt());
        pingDelayMeasurement.addField("rttMin", jsonNode.get("metric").get("metricResult").get("pingDelayMeasurement").get("rttMin").asDouble());
        pingDelayMeasurement.addField("rttAvg", jsonNode.get("metric").get("metricResult").get("pingDelayMeasurement").get("rttAvg").asDouble());
        pingDelayMeasurement.addField("rttMax", jsonNode.get("metric").get("metricResult").get("pingDelayMeasurement").get("rttMax").asDouble());
        pingDelayMeasurement.addField("rttMdev", jsonNode.get("metric").get("metricResult").get("pingDelayMeasurement").get("rttMdev").asDouble());
        return new ArrayList<>(List.of(pingDelayMeasurement));
    }

    private static void addNodeMetricDataAsTags(JsonNode jsonNode, Point point) {
        point.addTag("deviceId", jsonNode.get("deviceId").asText());
        point.addTag("jobUUID", jsonNode.get("metric").get("jobUUID").asText());
        point.addTag("metricUUID", jsonNode.get("metric").get("metricUUID").asText());
        point.addTag("metricType", jsonNode.get("metric").get("metricType").asText());
        point.addTag("sourceHost", jsonNode.get("metric").get("metricResult").get("sourceHost").asText());
        point.addTag("k8sResourceType", jsonNode.get("metric").get("metricResult").get("k8sResourceType").asText());
        point.addTag("resourceName", jsonNode.get("metric").get("metricResult").get("resourceName").asText());
        point.addTag("timestamp", jsonNode.get("metric").get("metricResult").get("time").asText());
    }

    private static void addNetworkMetricDataAsTags(JsonNode jsonNode, Point point) {
        point.addTag("deviceId", jsonNode.get("deviceId").asText());
        point.addTag("jobUUID", jsonNode.get("metric").get("jobUUID").asText());
        point.addTag("metricUUID", jsonNode.get("metric").get("metricUUID").asText());
        point.addTag("metricType", jsonNode.get("metric").get("metricType").asText());
        point.addTag("sourceHost", jsonNode.get("metric").get("metricResult").get("sourceHost").asText());
        point.addTag("destinationHost", jsonNode.get("metric").get("metricResult").get("destinationHost").asText());
        point.addTag("startTime", jsonNode.get("metric").get("metricResult").get("startTime").asText());
        point.addTag("endTime", jsonNode.get("metric").get("metricResult").get("endTime").asText());
    }

    private static List<Point> convertUDPBWMetric(JsonNode jsonNode) {
        String measurementName = "UDP_BW";
        // For receiver
        Point iperfBandwidthMeasurementReceiverPoint = new Point(measurementName);
        addNetworkMetricDataAsTags(jsonNode, iperfBandwidthMeasurementReceiverPoint);
        addUDPBandwithMeasurement(jsonNode, iperfBandwidthMeasurementReceiverPoint, "iperfBandwidthMeasurementReceiver");
        // For sender
        Point iperfBandwidthMeasurementSenderPoint = new Point(measurementName);
        addNetworkMetricDataAsTags(jsonNode, iperfBandwidthMeasurementSenderPoint);
        addUDPBandwithMeasurement(jsonNode, iperfBandwidthMeasurementSenderPoint, "iperfBandwidthMeasurementSender");
        return new ArrayList<>(List.of(iperfBandwidthMeasurementReceiverPoint, iperfBandwidthMeasurementSenderPoint));
    }

    private static void addUDPBandwithMeasurement(JsonNode jsonNode, Point iperfBandwidthMeasurementReceiverPoint, String iperfBandwidthMeasurementRole) {
        iperfBandwidthMeasurementReceiverPoint.addTag("iperf3Protocol", jsonNode.get("metric").get("metricResult").get(iperfBandwidthMeasurementRole).get("iperf3Protocol").asText());
        iperfBandwidthMeasurementReceiverPoint.addField("bitrate", jsonNode.get("metric").get("metricResult").get(iperfBandwidthMeasurementRole).get("bitrate").asDouble());
        iperfBandwidthMeasurementReceiverPoint.addTag("bandwidthUnit", jsonNode.get("metric").get("metricResult").get(iperfBandwidthMeasurementRole).get("bandwidthUnit").asText());
        iperfBandwidthMeasurementReceiverPoint.addTag("iperfRole", jsonNode.get("metric").get("metricResult").get(iperfBandwidthMeasurementRole).get("iperfRole").asText());
        iperfBandwidthMeasurementReceiverPoint.addField("jitter", jsonNode.get("metric").get("metricResult").get(iperfBandwidthMeasurementRole).get("jitter").asDouble());
        iperfBandwidthMeasurementReceiverPoint.addField("lostDatagrams", jsonNode.get("metric").get("metricResult").get(iperfBandwidthMeasurementRole).get("lostDatagrams").asInt());
        iperfBandwidthMeasurementReceiverPoint.addField("totalDatagrams", jsonNode.get("metric").get("metricResult").get(iperfBandwidthMeasurementRole).get("totalDatagrams").asInt());
    }

    private static void addTCPBandWidthMeasurement(JsonNode jsonNode, Point iperfBandwidthMeasurementReceiverPoint, String iperfBandwidthMeasurementRole) {
        iperfBandwidthMeasurementReceiverPoint.addTag("iperf3Protocol", jsonNode.get("metric").get("metricResult").get(iperfBandwidthMeasurementRole).get("iperf3Protocol").asText());
        iperfBandwidthMeasurementReceiverPoint.addField("bitrate", jsonNode.get("metric").get("metricResult").get(iperfBandwidthMeasurementRole).get("bitrate").asDouble());
        iperfBandwidthMeasurementReceiverPoint.addTag("bandwidthUnit", jsonNode.get("metric").get("metricResult").get(iperfBandwidthMeasurementRole).get("bandwidthUnit").asText());
        iperfBandwidthMeasurementReceiverPoint.addTag("iperfRole", jsonNode.get("metric").get("metricResult").get(iperfBandwidthMeasurementRole).get("iperfRole").asText());
    }

    private static List<Point> convertTCPBWMetric(JsonNode jsonNode) {
        String measurementName = "TCP_BW";
        // For receiver
        Point iperfBandwidthMeasurementReceiverPoint = new Point(measurementName);
        addNetworkMetricDataAsTags(jsonNode, iperfBandwidthMeasurementReceiverPoint);
        addTCPBandWidthMeasurement(jsonNode, iperfBandwidthMeasurementReceiverPoint, "iperfBandwidthMeasurementReceiver");
        // For sender
        Point iperfBandwidthMeasurementSenderPoint = new Point(measurementName);
        addNetworkMetricDataAsTags(jsonNode, iperfBandwidthMeasurementSenderPoint);
        addTCPBandWidthMeasurement(jsonNode, iperfBandwidthMeasurementSenderPoint, "iperfBandwidthMeasurementSender");
        return new ArrayList<>(List.of(iperfBandwidthMeasurementReceiverPoint, iperfBandwidthMeasurementSenderPoint));
    }

    private static List<Point> convertUDPRTTMetric(JsonNode jsonNode) {
        String measurementName = "UDP_RTT";
        Point npingUDPDelayMeasurment = new Point(measurementName);
        addNetworkMetricDataAsTags(jsonNode, npingUDPDelayMeasurment);
        npingUDPDelayMeasurment.addField("dataLength", jsonNode.get("metric").get("metricResult").get("dataLength").asInt());
        npingUDPDelayMeasurment.addField("maxRTT", jsonNode.get("metric").get("metricResult").get("npingUDPDelayMeasurement").get("maxRTT").asDouble());
        npingUDPDelayMeasurment.addField("minRTT", jsonNode.get("metric").get("metricResult").get("npingUDPDelayMeasurement").get("minRTT").asDouble());
        npingUDPDelayMeasurment.addField("avgRTT", jsonNode.get("metric").get("metricResult").get("npingUDPDelayMeasurement").get("avgRTT").asDouble());
        npingUDPDelayMeasurment.addField("udpPacketsSent", jsonNode.get("metric").get("metricResult").get("npingUDPDelayMeasurement").get("udpPacketsSent").asInt());
        npingUDPDelayMeasurment.addField("udpReceivedPackets", jsonNode.get("metric").get("metricResult").get("npingUDPDelayMeasurement").get("udpReceivedPackets").asInt());
        npingUDPDelayMeasurment.addField("udpLostPacketsAbsolute", jsonNode.get("metric").get("metricResult").get("npingUDPDelayMeasurement").get("udpLostPacketsAbsolute").asInt());
        npingUDPDelayMeasurment.addField("udpLostPacketsRelative", jsonNode.get("metric").get("metricResult").get("npingUDPDelayMeasurement").get("udpLostPacketsRelative").asDouble());
        return new ArrayList<>(List.of(npingUDPDelayMeasurment));
    }

    private static List<Point> convertTCPRTTMetric(JsonNode jsonNode) {
        String measurementName = "TCP_RTT";
        Point npingTCPDelayMeasurement = new Point(measurementName);
        addNetworkMetricDataAsTags(jsonNode, npingTCPDelayMeasurement);
        npingTCPDelayMeasurement.addField("maxRTT", jsonNode.get("metric").get("metricResult").get("npingTCPDelayMeasurement").get("maxRTT").asDouble());
        npingTCPDelayMeasurement.addField("minRTT", jsonNode.get("metric").get("metricResult").get("npingTCPDelayMeasurement").get("minRTT").asDouble());
        npingTCPDelayMeasurement.addField("avgRTT", jsonNode.get("metric").get("metricResult").get("npingTCPDelayMeasurement").get("avgRTT").asDouble());
        npingTCPDelayMeasurement.addField("tcpConnectionAttempts", jsonNode.get("metric").get("metricResult").get("npingTCPDelayMeasurement").get("tcpConnectionAttempts").asInt());
        npingTCPDelayMeasurement.addField("tcpSuccessfulConnections", jsonNode.get("metric").get("metricResult").get("npingTCPDelayMeasurement").get("tcpSuccessfulConnections").asInt());
        npingTCPDelayMeasurement.addField("tcpFailedConnectionsAbsolute", jsonNode.get("metric").get("metricResult").get("npingTCPDelayMeasurement").get("tcpFailedConnectionsAbsolute").asInt());
        npingTCPDelayMeasurement.addField("tcpFailedConnectionsRelative", jsonNode.get("metric").get("metricResult").get("npingTCPDelayMeasurement").get("tcpFailedConnectionsRelative").asDouble());
        return new ArrayList<>(List.of(npingTCPDelayMeasurement));
    }

    public static List<Point> convertEvent(String json) throws JsonProcessingException {
        JsonNode jsonNode = mapper.readTree(json);
        Point event = new Point("EVENT");
        event.addTag("eventUUID", jsonNode.get("eventUUID").asText());
        event.addTag("timestamp", jsonNode.get("timestamp").asText());
        event.addTag("eventType", jsonNode.get("eventType").asText());
        event.addField("payload", jsonNode.get("payload").asText());
        return new ArrayList<>(List.of(event));
    }

    public static List<Point> convertRequest(String json) throws JsonProcessingException {
        JsonNode jsonNode = mapper.readTree(json);
        Point request = new Point("REQUEST");
        request.addTag("requestUUID", jsonNode.get("requestUUID").asText());
        request.addTag("timestamp", jsonNode.get("timestamp").asText());
        request.addTag("requestType", jsonNode.get("requestType").asText());
        request.addTag("sourceHost", jsonNode.get("sourceHost").asText());
        request.addTag("destinationHost", jsonNode.get("destinationHost").asText());
        request.addTag("resource", jsonNode.get("resource").asText());
        request.addField("value", jsonNode.get("value").asDouble());
        request.addTag("unit", jsonNode.get("unit").asText());
        return new ArrayList<>(List.of(request));
    }

    public static List<Point> convertTaskStatusLog(String json) throws JsonProcessingException {
        JsonNode jsonNode = mapper.readTree(json);
        Point request = new Point("TASKSTATUSLOG");
        request.addTag("taskSequenceNumber", jsonNode.get("taskSequenceNumber").asText());
        request.addTag("taskUUID", jsonNode.get("taskUUID").asText());
        request.addTag("timestamp", jsonNode.get("timestamp").asText());
        request.addTag("previousStatus", jsonNode.get("previousStatus").asText());
        request.addTag("newStatus", jsonNode.get("newStatus").asText());
        request.addTag("modifiedOn", jsonNode.get("modifiedOn").asText());
        request.addTag("modifiedBy", jsonNode.get("modifiedBy").asText());
        request.addTag("modifiedById", jsonNode.get("modifiedById").asText());
        request.addTag("previousStateOfTask", jsonNode.get("previousStateOfTask").asText());
        request.addTag("newStateOfTask", jsonNode.get("newStateOfTask").asText());
        request.addTag("taskSchedulingUUID", jsonNode.get("taskSchedulingUUID").asText());
        request.addTag("comment", jsonNode.get("comment").asText());
        request.addField("value", 0);
        jsonNode.get("properties").fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            String value = entry.getValue().asText();
            request.addTag(key, value);
        });
        return new ArrayList<>(List.of(request));
    }
}
