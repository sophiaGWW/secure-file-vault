package com.example.securefilevault;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import com.example.securefilevault.controller.HealthController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class SecureFileVaultApplicationTests {

    @Test
    void healthEndpointReturnsOk() throws Exception {
        // Spring Context 全体ではなく Controller 単体で health endpoint を検証する。
        MockMvc mockMvc = standaloneSetup(new HealthController()).build();

        // /api/health が 200 OK と "OK" を返すことを確認する。
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }
}
