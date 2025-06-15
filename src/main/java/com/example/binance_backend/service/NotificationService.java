package com.example.binance_backend.service;

import com.example.binance_backend.model.User;
import com.example.binance_backend.repository.UserRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private UserRepository userRepository;

    // Envia uma notificação push para o utilizador
    public void sendPushNotification(User user, String title, String body) {
        if (user.getFcmToken() == null || user.getFcmToken().isEmpty()) {
            logger.warn("User {} não tem token FCM válido", user.getId());
            return;
        }

        try {
            Message message = Message.builder()
                    .setToken(user.getFcmToken())
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            logger.info("Notificação enviada para {}: {}", user.getUsername(), response);

        } catch (FirebaseMessagingException e) {
            if ("UNREGISTERED".equals(e.getErrorCode())) {
                logger.warn("Token FCM inválido para user {}. Removendo token.", user.getId());
                user.setFcmToken(null);
                userRepository.save(user);
            } else {
                logger.error("Erro ao enviar notificação: {}", e.getMessage());
            }
        }
    }
}