package com.parttimejob.service;

import com.parttimejob.dto.common.NotificationDto;
import com.parttimejob.entity.Notification;
import com.parttimejob.entity.User;
import com.parttimejob.enums.NotificationType;
import com.parttimejob.exception.ResourceNotFoundException;
import com.parttimejob.mapper.DtoMapper;
import com.parttimejob.repository.NotificationRepository;
import com.parttimejob.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SecurityUtils securityUtils;
    private final DtoMapper dtoMapper;

    @Transactional
    public void sendNotification(User recipient, String title, String message, NotificationType type, Long relatedEntityId) {
        if (recipient == null) return;
        Notification notification = Notification.builder()
                .recipient(recipient)
                .title(title)
                .message(message)
                .type(type)
                .relatedEntityId(relatedEntityId)
                .read(false)
                .build();
        notificationRepository.save(notification);
        log.debug("Sent notification [{}] to user [{}]", title, recipient.getEmail());
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> getCurrentUserNotifications() {
        User user = securityUtils.getCurrentUser();
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(dtoMapper::toNotificationDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getUnreadCount() {
        User user = securityUtils.getCurrentUser();
        return notificationRepository.countByRecipientIdAndReadFalse(user.getId());
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        User user = securityUtils.getCurrentUser();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + notificationId));

        if (!notification.getRecipient().getId().equals(user.getId())) {
            return; // Ignore unauthorized attempt silently or throw
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead() {
        User user = securityUtils.getCurrentUser();
        List<Notification> unread = notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(user.getId());
        for (Notification n : unread) {
            n.setRead(true);
        }
        notificationRepository.saveAll(unread);
    }
}
