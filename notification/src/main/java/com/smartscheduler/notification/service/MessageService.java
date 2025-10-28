package com.smartscheduler.notification.service;

import java.util.List;
import java.util.Map;

public interface MessageService {

    void sendWhatsAppMessage(List<Map<String, Object>> messages);

}
