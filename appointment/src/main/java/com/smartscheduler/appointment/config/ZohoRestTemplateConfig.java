package com.smartscheduler.appointment.config;

import com.smartscheduler.appointment.service.ZohoTokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class ZohoRestTemplateConfig {

    private final ZohoTokenService tokenService;

    public ZohoRestTemplateConfig(ZohoTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Bean("zohoRestTemplate")
    public RestTemplate zohoRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
        interceptors.add((request, body, execution) -> {
            String accessToken = tokenService.getAccessToken();
            request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
            return execution.execute(request, body);
        });
        restTemplate.setInterceptors(interceptors);
        return restTemplate;
    }
}

