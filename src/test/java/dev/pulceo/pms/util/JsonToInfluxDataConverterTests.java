package dev.pulceo.pms.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.influxdb.client.JSON;
import com.influxdb.client.write.Point;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

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
        List<Point> listOfPoints = JsonToInfluxDataConverter.convertMetric(jsonAsString);

        // then
        assertEquals("ICMP_RTT,destinationHost=localhost,deviceId=0247fea1-3ca3-401b-8fa2-b6f83a469680,endTime=2024-01-29T09:06:59.160564103Z,metricType=ICMP_RTT,metricUUID=99976261-b25d-4466-b522-8682496dccb2,sourceHost=localhost,startTime=2024-01-29T09:06:59.157516308Z packetLoss=0.0,packetsReceived=1i,packetsTransmitted=1i,rttAvg=0.045,rttMax=0.045,rttMdev=0.0,rttMin=0.045,time=0i", listOfPoints.get(0).toLineProtocol());
    }

    @Test
    public void testConvertFromUDPBWMetric() throws IOException {
        // given
        File jsonFile = new File("src/test/java/dev/pulceo/pms/resources/metrics/udp-bw.json");
        String jsonAsString = mapper.readTree(jsonFile).toString();

        // when
        List<Point> listOfPoints = JsonToInfluxDataConverter.convertMetric(jsonAsString);

        // then
        assertEquals("UDP_BW,bandwidthUnit=Mbits/s,destinationHost=127.0.0.1,deviceId=0247fea1-3ca3-401b-8fa2-b6f83a469680,endTime=2024-01-29T09:06:58.011300946Z,iperf3Protocol=UDP,iperfRole=RECEIVER,metricType=UDP_BW,metricUUID=a4524c72-f53d-449e-9b3b-37fe7904eae4,sourceHost=127.0.0.1,startTime=2024-01-29T09:06:57.008627239Z bitrate=32.1,jitter=0.017,lostDatagrams=0i,totalDatagrams=241i", listOfPoints.get(0).toLineProtocol());
        assertEquals("UDP_BW,bandwidthUnit=Mbits/s,destinationHost=127.0.0.1,deviceId=0247fea1-3ca3-401b-8fa2-b6f83a469680,endTime=2024-01-29T09:06:58.011300946Z,iperf3Protocol=UDP,iperfRole=SENDER,metricType=UDP_BW,metricUUID=a4524c72-f53d-449e-9b3b-37fe7904eae4,sourceHost=127.0.0.1,startTime=2024-01-29T09:06:57.008627239Z bitrate=32.1,jitter=0.0,lostDatagrams=0i,totalDatagrams=241i", listOfPoints.get(1).toLineProtocol());
    }

    @Test
    public void testConvertFromTCPBWMetric() throws IOException {
        // given
        File jsonFile = new File("src/test/java/dev/pulceo/pms/resources/metrics/tcp-bw.json");
        String jsonAsString = mapper.readTree(jsonFile).toString();

        // when
        List<Point> listOfPoints = JsonToInfluxDataConverter.convertMetric(jsonAsString);

        // then
        assertEquals("TCP_BW,bandwidthUnit=Mbits/s,destinationHost=localhost,deviceId=0247fea1-3ca3-401b-8fa2-b6f83a469680,endTime=2024-01-29T10:32:58.479535020Z,iperf3Protocol=TCP,iperfRole=RECEIVER,metricType=TCP_BW,metricUUID=fee50cfc-1360-4713-a70e-a0827d2d6976,sourceHost=localhost,startTime=2024-01-29T10:32:57.465165Z bitrate=15735.0", listOfPoints.get(0).toLineProtocol());
        assertEquals("TCP_BW,bandwidthUnit=Mbits/s,destinationHost=localhost,deviceId=0247fea1-3ca3-401b-8fa2-b6f83a469680,endTime=2024-01-29T10:32:58.479535020Z,iperf3Protocol=TCP,iperfRole=SENDER,metricType=TCP_BW,metricUUID=fee50cfc-1360-4713-a70e-a0827d2d6976,sourceHost=localhost,startTime=2024-01-29T10:32:57.465165Z bitrate=15738.0", listOfPoints.get(1).toLineProtocol());
    }

    @Test
    public void testConvertFromUDPRTTMetric() throws Exception {
        // given
        File jsonFile = new File("src/test/java/dev/pulceo/pms/resources/metrics/udp-rtt.json");
        String jsonAsString = mapper.readTree(jsonFile).toString();

        // when
        List<Point> listOfPoints = JsonToInfluxDataConverter.convertMetric(jsonAsString);

        // then
        assertEquals("UDP_RTT,destinationHost=127.0.0.1,deviceId=0247fea1-3ca3-401b-8fa2-b6f83a469680,endTime=2024-01-29T09:06:56.686175106Z,metricType=UDP_RTT,metricUUID=6cc6c101-b632-4a0c-b4d4-a5ad2a208c31,sourceHost=127.0.0.1,startTime=2024-01-29T09:06:56.679421866Z avgRTT=0.897,dataLength=4i,maxRTT=0.897,minRTT=0.897,udpLostPacketsAbsolute=0i,udpLostPacketsRelative=0.0,udpPacketsSent=1i,udpReceivedPackets=1i", listOfPoints.get(0).toLineProtocol());
    }
}
