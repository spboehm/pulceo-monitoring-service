package dev.pulceo.pms.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.influxdb.client.write.Point;

import java.sql.Array;
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
        Point point = new Point("ICMP_RTT");
        addMetricMetaDataAsTags(jsonNode, point);
        point.addField("packetsTransmitted", jsonNode.get("metric").get("metricResult").get("pingDelayMeasurement").get("packetsTransmitted").asInt());
        point.addField("packetsReceived", jsonNode.get("metric").get("metricResult").get("pingDelayMeasurement").get("packetsReceived").asInt());
        point.addField("packetLoss", jsonNode.get("metric").get("metricResult").get("pingDelayMeasurement").get("packetLoss").asDouble());
        point.addField("time", jsonNode.get("metric").get("metricResult").get("pingDelayMeasurement").get("time").asInt());
        point.addField("rttMin", jsonNode.get("metric").get("metricResult").get("pingDelayMeasurement").get("rttMin").asDouble());
        point.addField("rttAvg", jsonNode.get("metric").get("metricResult").get("pingDelayMeasurement").get("rttAvg").asDouble());
        point.addField("rttMax", jsonNode.get("metric").get("metricResult").get("pingDelayMeasurement").get("rttMax").asDouble());
        point.addField("rttMdev", jsonNode.get("metric").get("metricResult").get("pingDelayMeasurement").get("rttMdev").asDouble());
        return new ArrayList<>(List.of(point));
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
        // For receiver
        Point iperfBandwidthMeasurementReceiverPoint = new Point("UDP_BW");
        addMetricMetaDataAsTags(jsonNode, iperfBandwidthMeasurementReceiverPoint);
        addBandwithMeasurement(jsonNode, iperfBandwidthMeasurementReceiverPoint, "iperfBandwidthMeasurementReceiver");
        // For sender
        Point iperfBandwidthMeasurementSenderPoint = new Point("UDP_BW");
        addMetricMetaDataAsTags(jsonNode, iperfBandwidthMeasurementSenderPoint);
        addBandwithMeasurement(jsonNode, iperfBandwidthMeasurementSenderPoint, "iperfBandwidthMeasurementSender");
        return new ArrayList<>(List.of(iperfBandwidthMeasurementReceiverPoint, iperfBandwidthMeasurementSenderPoint));
    }

    private static void addBandwithMeasurement(JsonNode jsonNode, Point iperfBandwidthMeasurementReceiverPoint, String iperfBandwidthMeasurementRole) {
        iperfBandwidthMeasurementReceiverPoint.addTag("iperf3Protocol", jsonNode.get("metric").get("metricResult").get(iperfBandwidthMeasurementRole).get("iperf3Protocol").asText());
        iperfBandwidthMeasurementReceiverPoint.addField("bitrate", jsonNode.get("metric").get("metricResult").get(iperfBandwidthMeasurementRole).get("bitrate").asDouble());
        iperfBandwidthMeasurementReceiverPoint.addTag("bandwidthUnit", jsonNode.get("metric").get("metricResult").get(iperfBandwidthMeasurementRole).get("bandwidthUnit").asText());
        iperfBandwidthMeasurementReceiverPoint.addTag("iperfRole", jsonNode.get("metric").get("metricResult").get(iperfBandwidthMeasurementRole).get("iperfRole").asText());
        iperfBandwidthMeasurementReceiverPoint.addField("jitter", jsonNode.get("metric").get("metricResult").get(iperfBandwidthMeasurementRole).get("jitter").asDouble());
        iperfBandwidthMeasurementReceiverPoint.addField("lostDatagrams", jsonNode.get("metric").get("metricResult").get(iperfBandwidthMeasurementRole).get("lostDatagrams").asInt());
        iperfBandwidthMeasurementReceiverPoint.addField("totalDatagrams", jsonNode.get("metric").get("metricResult").get(iperfBandwidthMeasurementRole).get("totalDatagrams").asInt());
    }

    private static List<Point> convertTCPBWMetric(JsonNode jsonNode) {
        return new ArrayList<>();
    }

    private static List<Point> convertUDPRTTMetric(JsonNode jsonNode) {
        return new ArrayList<>();
    }

    private static List<Point> convertTCPRTTMetric(JsonNode jsonNode) {
        return new ArrayList<>();
    }

}
