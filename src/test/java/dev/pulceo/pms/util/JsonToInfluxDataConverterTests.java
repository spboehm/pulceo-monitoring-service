package dev.pulceo.pms.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.influxdb.client.JSON;
import com.influxdb.client.write.Point;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JsonToInfluxDataConverterTests {

    // safe
    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testConvertFromICMPRTTMetric() throws IOException {
        // given
        File jsonFile = new File("src/test/java/dev/pulceo/pms/resources/metrics/icmp-rtt.json");
        String jsonAsString = mapper.readTree(jsonFile).toString();

        // when
        Point p = JsonToInfluxDataConverter.convertMetric(jsonAsString);

        // then
        assertEquals("ICMP_RTT,destinationHost=localhost,deviceId=0247fea1-3ca3-401b-8fa2-b6f83a469680,endTime=2024-01-29T09:06:59.160564103Z,metricType=ICMP_RTT,metricUUID=99976261-b25d-4466-b522-8682496dccb2,sourceHost=localhost,startTime=2024-01-29T09:06:59.157516308Z packetLoss=0.0,packetsReceived=1i,packetsTransmitted=1i,rttAvg=0.045,rttMax=0.045,rttMdev=0.0,rttMin=0.045,time=0i", p.toLineProtocol());
    }
}
