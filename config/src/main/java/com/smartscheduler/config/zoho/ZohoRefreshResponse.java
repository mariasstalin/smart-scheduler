package com.smartscheduler.config.zoho;

import lombok.Data;

@Data
public class ZohoRefreshResponse {
    private String access_token;
    private long expires_in;

    public ZohoRefreshResponse() {}

}
