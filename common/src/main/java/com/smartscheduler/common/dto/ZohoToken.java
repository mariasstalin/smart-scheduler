package com.smartscheduler.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

@Data
public class ZohoToken implements Serializable {
    private String accessToken;
    private String refreshToken;
    private Instant expiryTime;

    public ZohoToken() {}

}
