package dev.pulceo.pms.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import dev.pulceo.pms.dto.metrics.NodeMetricDTO;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@SpringBootTest
public class SimpMessageHandlerTests {

    @Value("${orchestration.id}")
    private String orchestrationId;

    @Value("${orchestration.name}")
    private String orchestrationName;

    @Autowired
    private SimpMessageHandler simpMessageHandler;

    @Test
    @Disabled
    public void testSend() throws JsonProcessingException, InterruptedException {
        // given
        NodeMetricDTO nodeMetricDTO = NodeMetricDTO.builder()
                .orchestrationId(this.orchestrationId)
                .orchestrationName(this.orchestrationName)
                .metricUUID(UUID.randomUUID())
                .metricType("CPU_UTIL")
                .metricRequestUUID(UUID.randomUUID().toString())
                .nodeUUID(UUID.randomUUID().toString())
                .nodeName("fog1")
                .time("2024-05-24T11:39:53Z")
                .val(0.5)
                .unit("%")
                .build();

        // when
        while (true) {
            // infinite loop
            this.simpMessageHandler.send(nodeMetricDTO);
            Thread.sleep(5000);
        }

        // then

    }

}
