package com.smartcarepc.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Bật @Scheduled: gửi email nhắc bảo dưỡng mỗi giờ (EmailReminderService.sendDue) */
@Configuration
@EnableScheduling
public class SchedulingConfig {}
