package com.curso.instrumento2026.support;

import com.jayway.jsonpath.JsonPath;
import com.curso.instrumento2026.config.JwtProperties;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.List;
import java.time.Instant;

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

    // Signed fixture identity: exercises JwtDecoder, role conversion and authorization, not account login.
    // Does not install a thread-local administrator or require promoting a persisted account.
    public static RequestPostProcessor bearerComPerfis(JwtEncoder encoder, JwtProperties properties,
                                                       String login, String... perfis) {
        Instant agora = Instant.now();
        var claims = JwtClaimsSet.builder().issuer(properties.issuer()).subject(login)
                .issuedAt(agora).expiresAt(agora.plus(properties.expiration()))
                .claim("perfis", List.of(perfis)).build();
        return bearer(encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue());
    }
}
