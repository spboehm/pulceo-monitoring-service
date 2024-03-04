package dev.pulceo.pms.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping()
public class HealthController {

        @GetMapping(value = {"/health", "/healthz", "/pms/health"})
        public ResponseEntity<String> health() {
            return ResponseEntity.status(200).body("OK");
        }
}
