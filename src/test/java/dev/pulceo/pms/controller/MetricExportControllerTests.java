package dev.pulceo.pms.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.pulceo.pms.dto.metricexports.MetricExportRequestDTO;
import dev.pulceo.pms.model.metric.MetricType;
import dev.pulceo.pms.repository.MetricRequestRepository;
import dev.pulceo.pms.util.InfluxDBUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class MetricExportControllerTests {

    @Autowired
    private MetricRequestRepository metricRequestRepository;

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void before() {
        this.metricRequestRepository.deleteAll();
    }

    @BeforeAll
    static void setupClass() {
        InfluxDBUtil.initInfluxDBTestEnvironment();
    }

    @Test
    public void testCreateMetricExportRequestForCPUUtil() throws Exception {
        // given
        InfluxDBUtil.provideInfluxCPUUtilMetrics();
        MetricExportRequestDTO metricExportRequestDTO = MetricExportRequestDTO.builder()
                .metricType(MetricType.CPU_UTIL)
                .build();

        // when and then
        MvcResult mvcResult = this.mockMvc.perform(post("/api/v1/metric-exports")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(metricExportRequestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.metricType").value("CPU_UTIL"))
                .andExpect(jsonPath("$.numberOfRecords").value(100))
                .andExpect(jsonPath("$.metricExportState").value("PENDING"))
                .andReturn();

        UUID metricExportUUID = UUID.fromString(objectMapper.readTree(mvcResult.getResponse().getContentAsString()).get("metricExportUUID").asText());

        // then
        boolean metricExportStateIsPending = true;
        while(metricExportStateIsPending) {
            MvcResult subsequentMvcResult = this.mockMvc.perform(get("/api/v1/metric-exports/" + metricExportUUID)
                    .contentType("application/json"))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode jsonNode = objectMapper.readTree(subsequentMvcResult.getResponse().getContentAsString());

            if (!jsonNode.get("metricExportState").asText().equals("COMPLETED")) {
                Thread.sleep(500);
            } else {
                assertEquals("COMPLETED", jsonNode.get("metricExportState").asText());
                metricExportStateIsPending = false;
            }
        }
    }

}
