package dev.pulceo.pms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.pulceo.pms.model.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import java.security.Principal;
import java.text.SimpleDateFormat;

@Controller
public class MetricsController {

    @MessageMapping("/hello")
    @SendTo("/topic/greetings")
    public String greeting(Object message) throws Exception {
        Thread.sleep(1000); // simulated delay
        return "Hello!";
    }
}
