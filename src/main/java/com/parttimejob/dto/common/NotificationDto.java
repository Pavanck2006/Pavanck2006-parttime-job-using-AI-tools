package com.parttimejob.dto.common;

import com.parttimejob.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
    private Long id;
    private Long recipientId;
    private String title;
    private String message;
    private NotificationType type;
    private Long relatedEntityId;
    private boolean read;
    private LocalDateTime createdAt;
}
