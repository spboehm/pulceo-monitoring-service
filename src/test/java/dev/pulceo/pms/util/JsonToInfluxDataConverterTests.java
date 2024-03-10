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
    public void testConvertFromCPUUtilMetric() throws IOException {
        // given
        File jsonFile = new File("src/test/java/dev/pulceo/pms/resources/metrics/cpu-util.json");
        String jsonAsString = mapper.readTree(jsonFile).toString();

        // when
        List<Point> listOfPoints = JsonToInfluxDataConverter.convertMetric(jsonAsString);

        // then
        assertEquals("CPU_UTIL,deviceId=0247fea1-3ca3-401b-8fa2-b6f83a469680,jobUUID=729c57cc-5796-4b07-b89d-3f5f80f6a053,k8sResourceType=NODE,metricType=CPU_UTIL,metricUUID=315ff74c-5fae-45fa-9fcc-ec592c39e76c,resourceName=k3d-pna-test-server-0,sourceHost=127.0.0.1,timestamp=2024-02-18T11:56:10Z usageCPUPercentage=2.44,usageCoreNanoSeconds=247131680000i,usageNanoCores=39113293i", listOfPoints.get(0).toLineProtocol());
    }

    @Test
    public void testConvertMEMUtilMetric() throws Exception {
        // given
        File jsonFile = new File("src/test/java/dev/pulceo/pms/resources/metrics/mem-util.json");
        String jsonAsString = mapper.readTree(jsonFile).toString();

        // when
        List<Point> listOfPoints = JsonToInfluxDataConverter.convertMetric(jsonAsString);

        // then
        assertEquals("MEM_UTIL,deviceId=0247fea1-3ca3-401b-8fa2-b6f83a469680,jobUUID=18d3b2b1-f659-485c-8db0-0c47303fd68c,k8sResourceType=NODE,metricType=MEM_UTIL,metricUUID=2f0157c8-a00a-4d91-a285-8c0a22a3fd44,resourceName=k3d-pna-test-server-0,sourceHost=127.0.0.1,timestamp=2024-02-18T11:59:50Z availableBytes=66505011200i,usageBytes=1054355456i,usageMemoryPercentage=1.5699999", listOfPoints.get(0).toLineProtocol());
    }

    @Test
    public void testConvertNetUtilMetrics() throws Exception {
        // given
        File jsonFile = new File("src/test/java/dev/pulceo/pms/resources/metrics/net-util.json");
        String jsonAsString = mapper.readTree(jsonFile).toString();

        // when
        List<Point> listOfPoints = JsonToInfluxDataConverter.convertMetric(jsonAsString);

        // then
        assertEquals("NET_UTIL,deviceId=0247fea1-3ca3-401b-8fa2-b6f83a469680,iface=eth0,jobUUID=2b933e2f-d2be-44c7-afdc-bf35b257e5b4,k8sResourceType=NODE,metricType=NET_UTIL,metricUUID=4ed06f92-e9c7-493f-94b5-8d0ad38b92b2,resourceName=k3d-pna-test-server-0,sourceHost=127.0.0.1,timestamp=2024-02-18T11:56:20Z rxBytes=2520547i,txBytes=7199902i", listOfPoints.get(0).toLineProtocol());
    }

    @Test
    public void testConvertStorageUtilMetrics() throws Exception {
        // given
        File jsonFile = new File("src/test/java/dev/pulceo/pms/resources/metrics/storage-util.json");
        String jsonAsString = mapper.readTree(jsonFile).toString();

        // when
        List<Point> listOfPoints = JsonToInfluxDataConverter.convertMetric(jsonAsString);

        // then
        assertEquals("STORAGE_UTIL,deviceId=0247fea1-3ca3-401b-8fa2-b6f83a469680,jobUUID=e8ae8d97-0859-4e86-9163-928870c716dc,k8sResourceType=NODE,metricType=STORAGE_UTIL,metricUUID=ff161744-37df-4927-9f47-50db763b02be,resourceName=k3d-pna-test-server-0,sourceHost=127.0.0.1,timestamp=2024-02-18T11:56:20Z availableBytes=497419288576i,usageBytes=363328516096i,usageStoragePercentage=73.04", listOfPoints.get(0).toLineProtocol());
    }

    @Test
    public void testConvertFromICMPRTTMetric() throws IOException {
        // given
        File jsonFile = new File("src/test/java/dev/pulceo/pms/resources/metrics/icmp-rtt.json");
        String jsonAsString = mapper.readTree(jsonFile).toString();

        // when
        List<Point> listOfPoints = JsonToInfluxDataConverter.convertMetric(jsonAsString);

        // then
        assertEquals("ICMP_RTT,destinationHost=127.0.0.1,deviceId=0247fea1-3ca3-401b-8fa2-b6f83a469680,endTime=2024-01-29T09:06:59.160564103Z,jobUUID=f806ca34-2cea-41f4-a17e-ae3c0efcc971,metricType=ICMP_RTT,metricUUID=99976261-b25d-4466-b522-8682496dccb2,sourceHost=127.0.0.1,startTime=2024-01-29T09:06:59.157516308Z packetLoss=0.0,packetsReceived=1i,packetsTransmitted=1i,rttAvg=0.045,rttMax=0.045,rttMdev=0.0,rttMin=0.045,time=0i", listOfPoints.get(0).toLineProtocol());
    }

    @Test
    public void testConvertFromUDPBWMetric() throws IOException {
        // given
        File jsonFile = new File("src/test/java/dev/pulceo/pms/resources/metrics/udp-bw.json");
        String jsonAsString = mapper.readTree(jsonFile).toString();

        // when
        List<Point> listOfPoints = JsonToInfluxDataConverter.convertMetric(jsonAsString);

        // then
        assertEquals("UDP_BW,bandwidthUnit=Mbits/s,destinationHost=127.0.0.1,deviceId=0247fea1-3ca3-401b-8fa2-b6f83a469680,endTime=2024-01-29T09:06:58.011300946Z,iperf3Protocol=UDP,iperfRole=RECEIVER,jobUUID=a4524c72-f53d-449e-9b3b-37fe7904eae4,metricType=UDP_BW,metricUUID=a4524c72-f53d-449e-9b3b-37fe7904eae4,sourceHost=127.0.0.1,startTime=2024-01-29T09:06:57.008627239Z bitrate=32.1,jitter=0.017,lostDatagrams=0i,totalDatagrams=241i", listOfPoints.get(0).toLineProtocol());
        assertEquals("UDP_BW,bandwidthUnit=Mbits/s,destinationHost=127.0.0.1,deviceId=0247fea1-3ca3-401b-8fa2-b6f83a469680,endTime=2024-01-29T09:06:58.011300946Z,iperf3Protocol=UDP,iperfRole=SENDER,jobUUID=a4524c72-f53d-449e-9b3b-37fe7904eae4,metricType=UDP_BW,metricUUID=a4524c72-f53d-449e-9b3b-37fe7904eae4,sourceHost=127.0.0.1,startTime=2024-01-29T09:06:57.008627239Z bitrate=32.1,jitter=0.0,lostDatagrams=0i,totalDatagrams=241i", listOfPoints.get(1).toLineProtocol());
    }

    @Test
    public void testConvertFromTCPBWMetric() throws IOException {
        // given
        File jsonFile = new File("src/test/java/dev/pulceo/pms/resources/metrics/tcp-bw.json");
        String jsonAsString = mapper.readTree(jsonFile).toString();

        // when
        List<Point> listOfPoints = JsonToInfluxDataConverter.convertMetric(jsonAsString);

        // then
        assertEquals("TCP_BW,bandwidthUnit=Mbits/s,destinationHost=127.0.0.1,deviceId=0247fea1-3ca3-401b-8fa2-b6f83a469680,endTime=2024-01-29T10:32:58.479535020Z,iperf3Protocol=TCP,iperfRole=RECEIVER,jobUUID=fee50cfc-1360-4713-a70e-a0827d2d6976,metricType=TCP_BW,metricUUID=fee50cfc-1360-4713-a70e-a0827d2d6976,sourceHost=127.0.0.1,startTime=2024-01-29T10:32:57.465165Z bitrate=15735.0", listOfPoints.get(0).toLineProtocol());
        assertEquals("TCP_BW,bandwidthUnit=Mbits/s,destinationHost=127.0.0.1,deviceId=0247fea1-3ca3-401b-8fa2-b6f83a469680,endTime=2024-01-29T10:32:58.479535020Z,iperf3Protocol=TCP,iperfRole=SENDER,jobUUID=fee50cfc-1360-4713-a70e-a0827d2d6976,metricType=TCP_BW,metricUUID=fee50cfc-1360-4713-a70e-a0827d2d6976,sourceHost=127.0.0.1,startTime=2024-01-29T10:32:57.465165Z bitrate=15738.0", listOfPoints.get(1).toLineProtocol());
    }

    @Test
    public void testConvertFromUDPRTTMetric() throws Exception {
        // given
        File jsonFile = new File("src/test/java/dev/pulceo/pms/resources/metrics/udp-rtt.json");
        String jsonAsString = mapper.readTree(jsonFile).toString();

        // when
        List<Point> listOfPoints = JsonToInfluxDataConverter.convertMetric(jsonAsString);

        // then
        assertEquals("UDP_RTT,destinationHost=127.0.0.1,deviceId=0247fea1-3ca3-401b-8fa2-b6f83a469680,endTime=2024-01-29T09:06:56.686175106Z,jobUUID=da409089-5605-43ff-8eba-6e7ce7038d52,metricType=UDP_RTT,metricUUID=6cc6c101-b632-4a0c-b4d4-a5ad2a208c31,sourceHost=127.0.0.1,startTime=2024-01-29T09:06:56.679421866Z avgRTT=0.897,dataLength=4i,maxRTT=0.897,minRTT=0.897,udpLostPacketsAbsolute=0i,udpLostPacketsRelative=0.0,udpPacketsSent=1i,udpReceivedPackets=1i", listOfPoints.get(0).toLineProtocol());
    }

    @Test
    public void testConvertFromTCPRTTMetric() throws Exception {
        // given
        File jsonFile = new File("src/test/java/dev/pulceo/pms/resources/metrics/tcp-rtt.json");
        String jsonAsString = mapper.readTree(jsonFile).toString();

        // when
        List<Point> listofPoints = JsonToInfluxDataConverter.convertMetric(jsonAsString);

        // then
        assertEquals("TCP_RTT,destinationHost=127.0.0.1,deviceId=0247fea1-3ca3-401b-8fa2-b6f83a469680,endTime=2024-01-29T09:07:11.725847321Z,jobUUID=ac122df4-32b0-4c2d-9048-6cfd7971b477,metricType=TCP_RTT,metricUUID=cbd29b61-5955-4743-9c62-6710bf7bcab5,sourceHost=127.0.0.1,startTime=2024-01-29T09:07:11.716082302Z avgRTT=0.007,maxRTT=0.007,minRTT=0.007,tcpConnectionAttempts=1i,tcpFailedConnectionsAbsolute=0i,tcpFailedConnectionsRelative=0.0,tcpSuccessfulConnections=1i", listofPoints.get(0).toLineProtocol());
    }

    @Test
    public void testConvertEvent() throws Exception {
        // givne
        File jsonFile = new File("src/test/java/dev/pulceo/pms/resources/events/event.json");
        String jsonAsString = mapper.readTree(jsonFile).toString();

        // when
        List<Point> listofPoints = JsonToInfluxDataConverter.convertEvent(jsonAsString);

        assertEquals("EVENT,eventType=NODE_CREATED,eventUUID=0ca1537d-bbae-4ed0-aad5-aa92645aae09,timestamp=2024-03-10T17:49:51.442707432 payload=\"OnPremNode{onPremProvider=OnPremProvider{providerMetaData=ProviderMetaData{providerName='default', providerType=ON_PREM}}, nodeMetaData=NodeMetaData{remoteNodeUUID=3d068e35-471a-41dc-9771-e964fac72443, pnaUUID=0247fea1-3ca3-401b-8fa2-b6f83a469680, hostname='127.0.0.1'}, node=Node{name='edge-0', type=CLOUD, layer=1, role=WORKLOAD, nodeGroup='', country='Germany', state='Bavaria', city='Bamberg', longitude=0.0, latitude=0.0, cpuResource=CPUResource{cpuCapacity=CPU{modelName='AMD Ryzen 7 3700X 8-Core Processor', cores=8, threads=16, bogoMIPS=7202.77, MIPS=7202.77, GFlop=0.0, minimalFrequency=2200.0, averageFrequency=3313.0854, maximalFrequency=4426.171, shares=16000, slots=0.0}, cpuAllocatable=CPU{modelName='AMD Ryzen 7 3700X 8-Core Processor', cores=8, threads=16, bogoMIPS=7202.77, MIPS=7202.77, GFlop=0.0, minimalFrequency=2200.0, averageFrequency=3313.0854, maximalFrequency=4426.171, shares=16000, slots=0.0}}, memoryResource=MemoryResource{memoryCapacity=Memory{size=62.714092, slots=0}, memoryAllocatable=Memory{size=62.714092, slots=0}}, storageResource=StorageResource{storageCapacity=Storage{size=464.0, slots=0}, storageAllocatable=Storage{size=0.0, slots=0}}}}\"", listofPoints.get(0).toLineProtocol());
    }
 }
