package dev.pulceo.pms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.pulceo.pms.dto.orchestration.UpdateOrchestrationContextDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class OrchestrationContextControllerTests {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testReadOrchestrationContext() throws Exception {
        // given

        // when and then
        this.mockMvc.perform(get("/api/v1/orchestration-context"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").exists())
                .andExpect(jsonPath("$.name").value("default"));
    }

    @Test
    public void testUpdateOrchestrationContext() throws Exception {
        // given
        String expectedUuid = "123e4567-e89b-12d3-a456-426614174000";
        String expectedName = "newOrchestration";
        UpdateOrchestrationContextDTO updateRequest = UpdateOrchestrationContextDTO.builder()
                .uuid(expectedUuid)
                .name(expectedName)
                .build();

        // when and then
        this.mockMvc.perform(put("/api/v1/orchestration-context")
                .contentType("application/json")
                .content(this.objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(expectedUuid))
                .andExpect(jsonPath("$.name").value(expectedName));
    }
}
