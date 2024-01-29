package dev.pulceo.pms.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.influxdb.client.write.Point;

public class JsonToInfluxDataConverter {

    // safe
    private static final ObjectMapper mapper = new ObjectMapper();

    public static Point convertMetric(String json) throws JsonProcessingException {
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

    private static Point convertICMPRTTMetric(JsonNode jsonNode) {
        Point point = new Point("ICMP_RTT");
        point.addTag("deviceId", jsonNode.get("deviceId").asText());
        point.addTag("metricUUID", jsonNode.get("metric").get("metricUUID").asText());
        point.addTag("metricType", jsonNode.get("metric").get("metricType").asText());
        point.addTag("sourceHost", jsonNode.get("metric").get("metricResult").get("sourceHost").asText());
        point.addTag("destinationHost", jsonNode.get("metric").get("metricResult").get("destinationHost").asText());
        point.addTag("startTime", jsonNode.get("metric").get("metricResult").get("startTime").asText());
        point.addTag("endTime", jsonNode.get("metric").get("metricResult").get("endTime").asText());
        point.addField("packetsTransmitted", jsonNode.get("metric").get("metricResult").get("pingDelayMeasurement").get("packetsTransmitted").asInt());
        point.addField("packetsReceived", jsonNode.get("metric").get("metricResult").get("pingDelayMeasurement").get("packetsReceived").asInt());
        point.addField("packetLoss", jsonNode.get("metric").get("metricResult").get("pingDelayMeasurement").get("packetLoss").asDouble());
        point.addField("time", jsonNode.get("metric").get("metricResult").get("pingDelayMeasurement").get("time").asInt());
        point.addField("rttMin", jsonNode.get("metric").get("metricResult").get("pingDelayMeasurement").get("rttMin").asDouble());
        point.addField("rttAvg", jsonNode.get("metric").get("metricResult").get("pingDelayMeasurement").get("rttAvg").asDouble());
        point.addField("rttMax", jsonNode.get("metric").get("metricResult").get("pingDelayMeasurement").get("rttMax").asDouble());
        point.addField("rttMdev", jsonNode.get("metric").get("metricResult").get("pingDelayMeasurement").get("rttMdev").asDouble());
        return point;
    }

    private static Point convertUDPBWMetric(JsonNode jsonNode) {
        return null;
    }

    private static Point convertTCPBWMetric(JsonNode jsonNode) {
        return null;
    }

    private static Point convertUDPRTTMetric(JsonNode jsonNode) {
        return null;
    }

    private static Point convertTCPRTTMetric(JsonNode jsonNode) {
        return null;
    }

}
