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
            default:
                throw new IllegalArgumentException("Unknown metric type: " + type);
        }
    }

    private static List<Point> convertICMPRTTMetric(JsonNode jsonNode) {
        Point pingDelayMeasurement = new Point("ICMP_RTT");
        addMetricMetaDataAsTags(jsonNode, pingDelayMeasurement);
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

    private static void addMetricMetaDataAsTags(JsonNode jsonNode, Point point) {
        point.addTag("deviceId", jsonNode.get("deviceId").asText());
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
        addMetricMetaDataAsTags(jsonNode, iperfBandwidthMeasurementReceiverPoint);
        addUDPBandwithMeasurement(jsonNode, iperfBandwidthMeasurementReceiverPoint, "iperfBandwidthMeasurementReceiver");
        // For sender
        Point iperfBandwidthMeasurementSenderPoint = new Point(measurementName);
        addMetricMetaDataAsTags(jsonNode, iperfBandwidthMeasurementSenderPoint);
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
        addMetricMetaDataAsTags(jsonNode, iperfBandwidthMeasurementReceiverPoint);
        addTCPBandWidthMeasurement(jsonNode, iperfBandwidthMeasurementReceiverPoint, "iperfBandwidthMeasurementReceiver");
        // For sender
        Point iperfBandwidthMeasurementSenderPoint = new Point(measurementName);
        addMetricMetaDataAsTags(jsonNode, iperfBandwidthMeasurementSenderPoint);
        addTCPBandWidthMeasurement(jsonNode, iperfBandwidthMeasurementSenderPoint, "iperfBandwidthMeasurementSender");
        return new ArrayList<>(List.of(iperfBandwidthMeasurementReceiverPoint, iperfBandwidthMeasurementSenderPoint));
    }

    private static List<Point> convertUDPRTTMetric(JsonNode jsonNode) {
        String measurementName = "UDP_RTT";
        Point npingDelayMeasurementPoint = new Point(measurementName);
        addMetricMetaDataAsTags(jsonNode, npingDelayMeasurementPoint);
        npingDelayMeasurementPoint.addField("dataLength", jsonNode.get("metric").get("metricResult").get("dataLength").asInt());
        npingDelayMeasurementPoint.addField("maxRTT", jsonNode.get("metric").get("metricResult").get("npingUDPDelayMeasurement").get("maxRTT").asDouble());
        npingDelayMeasurementPoint.addField("minRTT", jsonNode.get("metric").get("metricResult").get("npingUDPDelayMeasurement").get("minRTT").asDouble());
        npingDelayMeasurementPoint.addField("avgRTT", jsonNode.get("metric").get("metricResult").get("npingUDPDelayMeasurement").get("avgRTT").asDouble());
        npingDelayMeasurementPoint.addField("udpPacketsSent", jsonNode.get("metric").get("metricResult").get("npingUDPDelayMeasurement").get("udpPacketsSent").asInt());
        npingDelayMeasurementPoint.addField("udpReceivedPackets", jsonNode.get("metric").get("metricResult").get("npingUDPDelayMeasurement").get("udpReceivedPackets").asInt());
        npingDelayMeasurementPoint.addField("udpLostPacketsAbsolute", jsonNode.get("metric").get("metricResult").get("npingUDPDelayMeasurement").get("udpLostPacketsAbsolute").asInt());
        npingDelayMeasurementPoint.addField("udpLostPacketsRelative", jsonNode.get("metric").get("metricResult").get("npingUDPDelayMeasurement").get("udpLostPacketsRelative").asDouble());
        return new ArrayList<>(List.of(npingDelayMeasurementPoint));
    }

    private static List<Point> convertTCPRTTMetric(JsonNode jsonNode) {
        return new ArrayList<>();
    }

}
