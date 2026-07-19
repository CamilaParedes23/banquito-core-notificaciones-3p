package com.banquito.platform.notification.api.controller;

import com.banquito.platform.notification.api.dto.api.NotificationResponse;
import com.banquito.platform.notification.api.dto.internal.InternalNotificationRequest;
import com.banquito.platform.notification.application.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InternalNotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private InternalNotificationController controller;

    private NotificationResponse notificationResponse;

    @BeforeEach
    void setUp() {
        notificationResponse = new NotificationResponse("notif-uuid-123", "corr-123", "WELCOME", "NOTIFICATION_SERVICE", "NORMAL", "EMAIL", "user@example.com", "John Doe", "EMAIL_TEMPLATE", "Welcome", "Welcome email", "PENDIENTE", null, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
    }

    // Tests para request
    @Test
    void testRequest_DatosValidos_RetornaCreado() {
        InternalNotificationRequest request = new InternalNotificationRequest(
                "source-event-uuid-123", "corr-123", "WELCOME", "NOTIFICATION_SERVICE", "NORMAL", "EMAIL",
                "actor-uuid-123", "CUSTOMER", "user@example.com", "John Doe", "EMAIL_TEMPLATE",
                "Welcome", "Welcome email", Map.of("name", "John"), null
        );
        when(notificationService.solicitarNotificacionInterna(request)).thenReturn(notificationResponse);

        NotificationResponse result = controller.request(request);

        assertNotNull(result);
        assertEquals("notif-uuid-123", result.notificationUuid());
        verify(notificationService).solicitarNotificacionInterna(request);
    }
}
