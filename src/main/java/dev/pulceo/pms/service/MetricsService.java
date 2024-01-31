package dev.pulceo.pms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    private SimpMessagingTemplate simpMessagingTemplate;

    @Autowired
    public MetricsService(SimpMessagingTemplate simpMessagingTemplate) {
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    private void process() {
        simpMessagingTemplate.convertAndSend("/metrics/", "content");
    }

}
