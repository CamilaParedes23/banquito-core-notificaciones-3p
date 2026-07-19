package com.banquito.platform.notification.api.controller;

import com.banquito.platform.notification.api.dto.api.*;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController controller;

    private TemplateResponse templateResponse;
    private NotificationResponse notificationResponse;
    private DeliveryAttemptResponse deliveryAttemptResponse;

    @BeforeEach
    void setUp() {
        templateResponse = new TemplateResponse("EMAIL_TEMPLATE", "WELCOME", "EMAIL", "Welcome email", "Welcome {{name}}", "es", "ACTIVO");
        notificationResponse = new NotificationResponse("notif-uuid-123", "corr-123", "WELCOME", "NOTIFICATION_SERVICE", "NORMAL", "EMAIL", "user@example.com", "John Doe", "EMAIL_TEMPLATE", "Welcome", "Welcome email", "PENDIENTE", null, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
        deliveryAttemptResponse = new DeliveryAttemptResponse(1, "SMTP", "EXITOSO", "200", "Email sent successfully", LocalDateTime.now());
    }

    // Tests para listTemplates
    @Test
    void testListTemplates_SinFiltro_RetornaLista() {
        when(notificationService.listarTemplates(null)).thenReturn(List.of(templateResponse));

        List<TemplateResponse> result = controller.listTemplates(null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("EMAIL_TEMPLATE", result.get(0).code());
    }

    @Test
    void testListTemplates_ConFiltro_RetornaFiltrada() {
        when(notificationService.listarTemplates("EMAIL")).thenReturn(List.of(templateResponse));

        List<TemplateResponse> result = controller.listTemplates("EMAIL");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // Tests para createTemplate
    @Test
    void testCreateTemplate_DatosValidos_RetornaCreado() {
        TemplateRequest request = new TemplateRequest("EMAIL_TEMPLATE", "WELCOME", "EMAIL", "Welcome email", "Welcome {{name}}", "es");
        when(notificationService.crearTemplate(request)).thenReturn(templateResponse);

        TemplateResponse result = controller.createTemplate(request);

        assertNotNull(result);
        assertEquals("EMAIL_TEMPLATE", result.code());
        verify(notificationService).crearTemplate(request);
    }

    // Tests para updateTemplate
    @Test
    void testUpdateTemplate_DatosValidos_RetornaActualizado() {
        TemplateUpdateRequest request = new TemplateUpdateRequest("Updated subject", "Updated content", "ACTIVO");
        when(notificationService.actualizarTemplate("EMAIL_TEMPLATE", request)).thenReturn(templateResponse);

        TemplateResponse result = controller.updateTemplate("EMAIL_TEMPLATE", request);

        assertNotNull(result);
        verify(notificationService).actualizarTemplate("EMAIL_TEMPLATE", request);
    }

    // Tests para requestNotification
    @Test
    void testRequestNotification_DatosValidos_RetornaCreado() {
        NotificationRequestDto request = new NotificationRequestDto("corr-123", "WELCOME", "NOTIFICATION_SERVICE", "NORMAL", "EMAIL", "user@example.com", "John Doe", "EMAIL_TEMPLATE", "Welcome", "Welcome email", Map.of("name", "John"), null);
        when(notificationService.solicitarNotificacion(request)).thenReturn(notificationResponse);

        NotificationResponse result = controller.requestNotification(request);

        assertNotNull(result);
        assertEquals("notif-uuid-123", result.notificationUuid());
        verify(notificationService).solicitarNotificacion(request);
    }

    // Tests para getRequest
    @Test
    void testGetRequest_UuidValido_RetornaNotificacion() {
        when(notificationService.obtenerNotificacion("notif-uuid-123")).thenReturn(notificationResponse);

        NotificationResponse result = controller.getRequest("notif-uuid-123");

        assertNotNull(result);
        assertEquals("notif-uuid-123", result.notificationUuid());
    }

    // Tests para getNotificationAlias
    @Test
    void testGetNotificationAlias_UuidValido_RetornaNotificacion() {
        when(notificationService.obtenerNotificacion("notif-uuid-123")).thenReturn(notificationResponse);

        NotificationResponse result = controller.getNotificationAlias("notif-uuid-123");

        assertNotNull(result);
        assertEquals("notif-uuid-123", result.notificationUuid());
    }

    // Tests para getAttempts
    @Test
    void testGetAttempts_UuidValido_RetornaLista() {
        when(notificationService.listarIntentos("notif-uuid-123")).thenReturn(List.of(deliveryAttemptResponse));

        List<DeliveryAttemptResponse> result = controller.getAttempts("notif-uuid-123");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // Tests para testNotification
    @Test
    void testTestNotification_DatosValidos_RetornaCreado() {
        TestNotificationRequest request = new TestNotificationRequest("WELCOME", "user@example.com", "John Doe", "EMAIL_TEMPLATE", Map.of("name", "John"));
        when(notificationService.probarNotificacion(request)).thenReturn(notificationResponse);

        NotificationResponse result = controller.testNotification(request);

        assertNotNull(result);
        assertEquals("notif-uuid-123", result.notificationUuid());
        verify(notificationService).probarNotificacion(request);
    }
}
