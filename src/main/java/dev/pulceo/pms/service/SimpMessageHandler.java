package dev.pulceo.pms.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.pulceo.pms.dto.metrics.NodeMetricDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class SimpMessageHandler {

    private final Logger logger = LoggerFactory.getLogger(InfluxDBService.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${orchestration.id}")
    private String orchestrationId;

    @Value("${orchestration.name}")
    private String orchestrationName;

    private final SimpMessagingTemplate simpMessagingTemplate;

    @Autowired
    public SimpMessageHandler(SimpMessagingTemplate simpMessagingTemplate) {
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    public void send(NodeMetricDTO nodeMetricDTO) throws JsonProcessingException {
        this.simpMessagingTemplate.convertAndSend("/orchestrations/" + this.orchestrationId + "/metrics", this.objectMapper.writeValueAsString(nodeMetricDTO));
    }
}
