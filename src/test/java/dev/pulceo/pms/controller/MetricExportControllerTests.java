package dev.pulceo.pms.controller;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

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

        // when
        MvcResult mvcResult = this.mockMvc.perform(post("/api/v1/metric-exports")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(metricExportRequestDTO)))
                .andReturn();

        // then
        System.out.println(mvcResult.getResponse().getContentAsString());
    }
}
