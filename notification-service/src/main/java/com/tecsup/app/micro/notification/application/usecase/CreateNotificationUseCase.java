package com.tecsup.app.micro.notification.application.usecase;

import com.tecsup.app.micro.notification.domain.model.Notification;
import com.tecsup.app.micro.notification.domain.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateNotificationUseCase {
    
    private final NotificationRepository notificationRepository;
    
    public Notification execute(Long userId, String message, String type) {
        Notification notification = Notification.builder()
                .userId(userId)
                .message(message)
                .type(type)
                .build();
        
        Notification saved = notificationRepository.save(notification);
        log.info("Notification created: id={}, type={}, message={}", saved.getId(), type, message);
        return saved;
    }
}
