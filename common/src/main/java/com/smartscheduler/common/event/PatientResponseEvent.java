package com.smartscheduler.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientResponseEvent implements Serializable {
    private Long notificationId;
    private Long patientId;
    private String response;
}

