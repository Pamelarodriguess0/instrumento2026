package com.curso.instrumento2026.support;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public final class JwtTestSupport {
    private JwtTestSupport() {}

    public static MockHttpServletRequestBuilder loginRequest(String login, String senha) {
        return post("/api/autenticacao/login").contentType(MediaType.APPLICATION_JSON)
                .content(JsonMapper.builder().build().writeValueAsString(Map.of("login", login, "senha", senha)));
    }

    public static String token(MockMvc mvc, String login, String senha) throws Exception {
        var result = mvc.perform(loginRequest(login, senha)).andExpect(status().isOk()).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    public static RequestPostProcessor bearer(MockMvc mvc, String login, String senha) throws Exception {
        return bearer(token(mvc, login, senha));
    }

    public static RequestPostProcessor bearer(String token) {
        return request -> {
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            return request;
        };
    }
}
