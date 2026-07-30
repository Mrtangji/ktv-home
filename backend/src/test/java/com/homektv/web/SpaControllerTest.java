package com.homektv.web;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class SpaControllerTest {
    @Test
    void rootRedirectsToAdmin() throws Exception {
        MockMvc mvc = standaloneSetup(new SpaController()).build();

        mvc.perform(get("/"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/m/admin"));
    }
}
