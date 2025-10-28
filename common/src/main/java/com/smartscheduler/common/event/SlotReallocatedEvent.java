package com.smartscheduler.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SlotReallocatedEvent implements Serializable {
    private Long notificationId;
}

