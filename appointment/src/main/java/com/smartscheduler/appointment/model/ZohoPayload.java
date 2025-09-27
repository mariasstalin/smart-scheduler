package com.smartscheduler.appointment.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ZohoPayload {

    @JsonProperty("start_time")
    private String startTime;

    @JsonProperty("end_time")
    private String endTime;

    private Customer customer;

    @Data
    public static class Customer {
        private String email;
    }
}

