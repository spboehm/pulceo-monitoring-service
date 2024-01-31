package dev.pulceo.pms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.pulceo.pms.model.Test;
import dev.pulceo.pms.service.InfluxDBService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import java.security.Principal;
import java.text.SimpleDateFormat;

@Controller
public class MetricsController {

    @MessageMapping("/hello/{dest}")
    @SendTo("/topic/greetings/{dest}")
    public String greeting(@DestinationVariable String dest, Object message) throws Exception {
        

        return "Hello!";
    }
}
