package com.smartscheduler.notification.client.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "ai-priority-client", url = "${ai.priority.engine.url}")
public interface AiPriorityFeignClient {

    @PostMapping("/predict")
    List<Map<String, Object>> getPriorityScores(@RequestBody Map<String, Object> request);
}

