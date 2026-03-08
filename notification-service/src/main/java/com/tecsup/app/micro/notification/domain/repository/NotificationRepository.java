package com.tecsup.app.micro.notification.domain.repository;

import com.tecsup.app.micro.notification.domain.model.Notification;

import java.util.List;

public interface NotificationRepository {
    Notification save(Notification notification);
    List<Notification> findAll();
    List<Notification> findByUserId(Long userId);
}
