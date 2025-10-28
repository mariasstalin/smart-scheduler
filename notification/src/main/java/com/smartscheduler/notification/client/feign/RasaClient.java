package com.smartscheduler.notification.client.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "rasaClient", url = "${rasa.url}")
public interface RasaClient {

    @PostMapping(value = "/webhooks/rest/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    List<Map<String, Object>> sendMessage(@RequestBody Map<String, Object> payload);

}

