package com.banquito.platform.notification.application.service;

import com.banquito.platform.notification.api.dto.api.*;
import com.banquito.platform.notification.api.dto.internal.InternalNotificationRequest;
import com.banquito.platform.notification.domain.enums.*;
import com.banquito.platform.notification.domain.model.*;
import com.banquito.platform.notification.domain.repository.*;
import com.banquito.platform.notification.shared.exception.BusinessException;
import com.banquito.platform.notification.infrastructure.email.NotificationSendResult;
import com.banquito.platform.notification.infrastructure.email.SmtpNotificationSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceTest {

    @Mock
    private NotificationTemplateRepository templateRepository;

    @Mock
    private NotificationRequestRepository requestRepository;

    @Mock
    private NotificationDeliveryAttemptRepository attemptRepository;

    @Mock
    private NotificationChannelConfigRepository channelRepository;

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @Mock
    private AuditoriaNotificationEventoRepository auditRepository;

    @Mock
    private SmtpNotificationSender smtpNotificationSender;

    @InjectMocks
    private NotificationService notificationService;

    private NotificationTemplate template;
    private NotificationRequest request;
    private NotificationChannelConfig channelConfig;
    private NotificationPreference preference;

    @BeforeEach
    void setUp() {
        template = NotificationTemplate.crear("TEST_TEMPLATE", "TEST_EVENT", TipoCanalEnum.EMAIL, "Test Subject", "Test Body", "ES");
        request = NotificationRequest.crear("corr-123", "TEST_EVENT", "TEST_SERVICE", PrioridadNotificacionEnum.NORMAL, TipoCanalEnum.EMAIL,
                "test@example.com", "Test User", "TEST_TEMPLATE", "Test Subject", "Test Body", "{}");
        channelConfig = new NotificationChannelConfig();
        channelConfig.setTipoCanal(TipoCanalEnum.EMAIL);
        channelConfig.setEstado(EstadoBasicoEnum.ACTIVO);
        preference = new NotificationPreference();
        preference.setUuidActor("actor-123");
        preference.setTipoEvento("TEST_EVENT");
        preference.setTipoCanal(TipoCanalEnum.EMAIL);
        preference.setDestino("test@example.com");
        preference.setHabilitado(true);
    }

    // Tests para obtenerNotificacion
    @Test
    void testObtenerNotificacion_UuidNoExiste_LanzaExcepcion() {
        when(requestRepository.findByUuidNotificacion("no-existe")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> notificationService.obtenerNotificacion("no-existe"));

        assertEquals("NOTIFICATION_REQUEST_NOT_FOUND", exception.getCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    // Tests para listarIntentos
    @Test
    void testListarIntentos_UuidNoExiste_LanzaExcepcion() {
        when(requestRepository.findByUuidNotificacion("no-existe")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> notificationService.listarIntentos("no-existe"));

        assertEquals("NOTIFICATION_REQUEST_NOT_FOUND", exception.getCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    // Tests para listarTemplates
    @Test
    void testListarTemplates_SinFiltros_RetornaTodos() {
        when(templateRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(template));

        List<TemplateResponse> result = notificationService.listarTemplates(null);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // Tests para listarTemplates con filtro
    @Test
    void testListarTemplates_ConFiltro_RetornaFiltrados() {
        when(templateRepository.findByTipoEventoAndEstadoOrderByCodigoAsc("TEST_EVENT", EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(template));

        List<TemplateResponse> result = notificationService.listarTemplates("TEST_EVENT");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // Tests para crearTemplate
    @Test
    void testCrearTemplate_DatosValidos_RetornaCreado() {
        TemplateRequest templateRequest = new TemplateRequest("NEW_TEMPLATE", "TEST_EVENT", "EMAIL", "New Subject", "New Body", "ES");
        when(templateRepository.existsByCodigo("NEW_TEMPLATE")).thenReturn(false);
        when(templateRepository.save(any(NotificationTemplate.class))).thenReturn(template);

        TemplateResponse result = notificationService.crearTemplate(templateRequest);

        assertNotNull(result);
        verify(auditRepository).save(any(AuditoriaNotificationEvento.class));
    }

    // Tests para crearTemplate con código duplicado
    @Test
    void testCrearTemplate_CodigoDuplicado_LanzaExcepcion() {
        TemplateRequest templateRequest = new TemplateRequest("EXISTING_TEMPLATE", "TEST_EVENT", "EMAIL", "Subject", "Body", "ES");
        when(templateRepository.existsByCodigo("EXISTING_TEMPLATE")).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> notificationService.crearTemplate(templateRequest));

        assertEquals("NOTIFICATION_TEMPLATE_DUPLICATED", exception.getCode());
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    // Tests para actualizarTemplate
    @Test
    void testActualizarTemplate_DatosValidos_RetornaActualizado() {
        TemplateUpdateRequest updateRequest = new TemplateUpdateRequest("Updated Subject", "Updated Body", "ACTIVO");
        when(templateRepository.findByCodigo("TEST_TEMPLATE")).thenReturn(Optional.of(template));
        when(templateRepository.save(any(NotificationTemplate.class))).thenReturn(template);

        TemplateResponse result = notificationService.actualizarTemplate("TEST_TEMPLATE", updateRequest);

        assertNotNull(result);
        verify(auditRepository).save(any(AuditoriaNotificationEvento.class));
    }

    // Tests para actualizarTemplate con código no existente
    @Test
    void testActualizarTemplate_CodigoNoExiste_LanzaExcepcion() {
        TemplateUpdateRequest updateRequest = new TemplateUpdateRequest("Subject", "Body", "ACTIVO");
        when(templateRepository.findByCodigo("NOEXISTE")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> notificationService.actualizarTemplate("NOEXISTE", updateRequest));

        assertEquals("NOTIFICATION_TEMPLATE_NOT_FOUND", exception.getCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    // Tests para solicitarNotificacion con canal no disponible
    @Test
    void testSolicitarNotificacion_CanalNoDisponible_LanzaExcepcion() {
        NotificationRequestDto dto = new NotificationRequestDto(
                "corr-123", "TEST_EVENT", "TEST_SERVICE", "NORMAL", "EMAIL",
                "test@example.com", "Test User", "TEST_TEMPLATE",
                "Test Subject", "Test Body", Map.of(), null
        );
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> notificationService.solicitarNotificacion(dto));

        assertEquals("NOTIFICATION_CHANNEL_NOT_AVAILABLE", exception.getCode());
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
    }

    // Tests para obtenerNotificacion con uuid existente
    @Test
    void testObtenerNotificacion_UuidExistente_RetornaNotificacion() {
        when(requestRepository.findByUuidNotificacion("uuid-123")).thenReturn(Optional.of(request));

        NotificationResponse result = notificationService.obtenerNotificacion("uuid-123");

        assertNotNull(result);
    }

    // Tests para listarIntentos con uuid existente
    @Test
    void testListarIntentos_UuidExistente_RetornaIntentos() {
        when(requestRepository.findByUuidNotificacion("uuid-123")).thenReturn(Optional.of(request));
        when(attemptRepository.findByNotificationRequestIdOrderByIntentoNumeroAsc(any())).thenReturn(List.of());

        List<DeliveryAttemptResponse> result = notificationService.listarIntentos("uuid-123");

        assertNotNull(result);
    }

    // Tests para crearTemplate con tipo de canal inválido
    @Test
    void testCrearTemplate_TipoCanalInvalido_LanzaExcepcion() {
        TemplateRequest templateRequest = new TemplateRequest("NEW_TEMPLATE", "TEST_EVENT", "INVALID", "Subject", "Body", "ES");
        when(templateRepository.existsByCodigo("NEW_TEMPLATE")).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> notificationService.crearTemplate(templateRequest));

        assertEquals("NOTIFICATION_CHANNEL_INVALID", exception.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    // Tests para actualizarTemplate con estado inválido
    @Test
    void testActualizarTemplate_EstadoInvalido_LanzaExcepcion() {
        TemplateUpdateRequest updateRequest = new TemplateUpdateRequest("Subject", "Body", "INVALID");
        when(templateRepository.findByCodigo("TEST_TEMPLATE")).thenReturn(Optional.of(template));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> notificationService.actualizarTemplate("TEST_TEMPLATE", updateRequest));

        assertEquals("NOTIFICATION_TEMPLATE_STATUS_INVALID", exception.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    // Tests para solicitarNotificacion con prioridad inválida
    @Test
    void testSolicitarNotificacion_PrioridadInvalida_LanzaExcepcion() {
        NotificationRequestDto dto = new NotificationRequestDto(
                "corr-123", "TEST_EVENT", "TEST_SERVICE", "INVALID", "EMAIL",
                "test@example.com", "Test User", "TEST_TEMPLATE",
                "Test Subject", "Test Body", Map.of(), null
        );
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(channelConfig));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> notificationService.solicitarNotificacion(dto));

        assertEquals("NOTIFICATION_PRIORITY_INVALID", exception.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    // Tests para solicitarNotificacion con cuerpo vacío
    @Test
    void testSolicitarNotificacion_CuerpoVacio_LanzaExcepcion() {
        NotificationRequestDto dto = new NotificationRequestDto(
                "corr-123", "TEST_EVENT", "TEST_SERVICE", "NORMAL", "EMAIL",
                "test@example.com", "Test User", null,
                "Test Subject", null, Map.of(), null
        );
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(channelConfig));
        when(templateRepository.findByCodigo(null)).thenReturn(Optional.empty());
        when(templateRepository.findFirstByTipoEventoAndTipoCanalAndEstadoOrderByIdAsc("TEST_EVENT", TipoCanalEnum.EMAIL, EstadoBasicoEnum.ACTIVO))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> notificationService.solicitarNotificacion(dto));

        assertEquals("NOTIFICATION_BODY_REQUIRED", exception.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    // Tests para solicitarNotificacion con template no encontrado
    @Test
    void testSolicitarNotificacion_TemplateNoEncontrado_LanzaExcepcion() {
        NotificationRequestDto dto = new NotificationRequestDto(
                "corr-123", "TEST_EVENT", "TEST_SERVICE", "NORMAL", "EMAIL",
                "test@example.com", "Test User", "NOEXISTE",
                "Test Subject", "Test Body", Map.of(), null
        );
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(channelConfig));
        when(templateRepository.findByCodigo("NOEXISTE")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> notificationService.solicitarNotificacion(dto));

        assertEquals("NOTIFICATION_TEMPLATE_NOT_FOUND", exception.getCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    // Tests para solicitarNotificacion con tipo de canal inválido
    @Test
    void testSolicitarNotificacion_TipoCanalInvalido_LanzaExcepcion() {
        NotificationRequestDto dto = new NotificationRequestDto(
                "corr-123", "TEST_EVENT", "TEST_SERVICE", "NORMAL", "INVALID",
                "test@example.com", "Test User", "TEST_TEMPLATE",
                "Test Subject", "Test Body", Map.of(), null
        );
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(channelConfig));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> notificationService.solicitarNotificacion(dto));

        assertEquals("NOTIFICATION_CHANNEL_INVALID", exception.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    // Tests para listarTemplates con filtro vacío
    @Test
    void testListarTemplates_FiltroVacio_RetornaTodos() {
        when(templateRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(template));

        List<TemplateResponse> result = notificationService.listarTemplates("");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // Tests para listarTemplates con filtro de espacio
    @Test
    void testListarTemplates_FiltroEspacio_RetornaTodos() {
        when(templateRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(template));

        List<TemplateResponse> result = notificationService.listarTemplates("   ");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // Tests para crearTemplate con tipo de canal válido
    @Test
    void testCrearTemplate_TipoCanalValido_RetornaCreado() {
        TemplateRequest templateRequest = new TemplateRequest("NEW_TEMPLATE", "TEST_EVENT", "EMAIL", "New Subject", "New Body", "ES");
        when(templateRepository.existsByCodigo("NEW_TEMPLATE")).thenReturn(false);
        when(templateRepository.save(any(NotificationTemplate.class))).thenReturn(template);

        TemplateResponse result = notificationService.crearTemplate(templateRequest);

        assertNotNull(result);
        verify(auditRepository).save(any(AuditoriaNotificationEvento.class));
    }

    // Tests para actualizarTemplate sin estado
    @Test
    void testActualizarTemplate_SinEstado_RetornaActualizado() {
        TemplateUpdateRequest updateRequest = new TemplateUpdateRequest("Updated Subject", "Updated Body", null);
        when(templateRepository.findByCodigo("TEST_TEMPLATE")).thenReturn(Optional.of(template));
        when(templateRepository.save(any(NotificationTemplate.class))).thenReturn(template);

        TemplateResponse result = notificationService.actualizarTemplate("TEST_TEMPLATE", updateRequest);

        assertNotNull(result);
        verify(auditRepository).save(any(AuditoriaNotificationEvento.class));
    }

    // Tests para solicitarNotificacion con prioridad vacía
    @Test
    void testSolicitarNotificacion_PrioridadVacia_UsaNormal() {
        NotificationRequestDto dto = new NotificationRequestDto(
                "corr-123", "TEST_EVENT", "TEST_SERVICE", "", "EMAIL",
                "test@example.com", "Test User", "TEST_TEMPLATE",
                "Test Subject", "Test Body", Map.of(), null
        );
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(channelConfig));
        when(templateRepository.findByCodigo("TEST_TEMPLATE")).thenReturn(Optional.of(template));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);
        when(attemptRepository.countByNotificationRequestId(any())).thenReturn(0L);
        when(attemptRepository.save(any(NotificationDeliveryAttempt.class))).thenReturn(new NotificationDeliveryAttempt());
        when(smtpNotificationSender.send(any(NotificationRequest.class)))
                .thenReturn(new NotificationSendResult(true, "200", "OK", "SMTP"));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);

        NotificationResponse result = notificationService.solicitarNotificacion(dto);

        assertNotNull(result);
        verify(auditRepository).save(any(AuditoriaNotificationEvento.class));
    }

    // Tests para solicitarNotificacion con prioridad null
    @Test
    void testSolicitarNotificacion_PrioridadNull_UsaNormal() {
        NotificationRequestDto dto = new NotificationRequestDto(
                "corr-123", "TEST_EVENT", "TEST_SERVICE", null, "EMAIL",
                "test@example.com", "Test User", "TEST_TEMPLATE",
                "Test Subject", "Test Body", Map.of(), null
        );
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(channelConfig));
        when(templateRepository.findByCodigo("TEST_TEMPLATE")).thenReturn(Optional.of(template));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);
        when(attemptRepository.countByNotificationRequestId(any())).thenReturn(0L);
        when(attemptRepository.save(any(NotificationDeliveryAttempt.class))).thenReturn(new NotificationDeliveryAttempt());
        when(smtpNotificationSender.send(any(NotificationRequest.class)))
                .thenReturn(new NotificationSendResult(true, "200", "OK", "SMTP"));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);

        NotificationResponse result = notificationService.solicitarNotificacion(dto);

        assertNotNull(result);
        verify(auditRepository).save(any(AuditoriaNotificationEvento.class));
    }

    // Tests para solicitarNotificacion con subject null
    @Test
    void testSolicitarNotificacion_SubjectNull_UsaTemplate() {
        NotificationRequestDto dto = new NotificationRequestDto(
                "corr-123", "TEST_EVENT", "TEST_SERVICE", "NORMAL", "EMAIL",
                "test@example.com", "Test User", "TEST_TEMPLATE",
                null, "Test Body", Map.of(), null
        );
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(channelConfig));
        when(templateRepository.findByCodigo("TEST_TEMPLATE")).thenReturn(Optional.of(template));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);
        when(attemptRepository.countByNotificationRequestId(any())).thenReturn(0L);
        when(attemptRepository.save(any(NotificationDeliveryAttempt.class))).thenReturn(new NotificationDeliveryAttempt());
        when(smtpNotificationSender.send(any(NotificationRequest.class)))
                .thenReturn(new NotificationSendResult(true, "200", "OK", "SMTP"));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);

        NotificationResponse result = notificationService.solicitarNotificacion(dto);

        assertNotNull(result);
        verify(auditRepository).save(any(AuditoriaNotificationEvento.class));
    }

    // Tests para solicitarNotificacion con body null
    @Test
    void testSolicitarNotificacion_BodyNull_UsaTemplate() {
        NotificationRequestDto dto = new NotificationRequestDto(
                "corr-123", "TEST_EVENT", "TEST_SERVICE", "NORMAL", "EMAIL",
                "test@example.com", "Test User", "TEST_TEMPLATE",
                "Test Subject", null, Map.of(), null
        );
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(channelConfig));
        when(templateRepository.findByCodigo("TEST_TEMPLATE")).thenReturn(Optional.of(template));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);
        when(attemptRepository.countByNotificationRequestId(any())).thenReturn(0L);
        when(attemptRepository.save(any(NotificationDeliveryAttempt.class))).thenReturn(new NotificationDeliveryAttempt());
        when(smtpNotificationSender.send(any(NotificationRequest.class)))
                .thenReturn(new NotificationSendResult(true, "200", "OK", "SMTP"));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);

        NotificationResponse result = notificationService.solicitarNotificacion(dto);

        assertNotNull(result);
        verify(auditRepository).save(any(AuditoriaNotificationEvento.class));
    }

    // Tests para solicitarNotificacion con template null y body null
    @Test
    void testSolicitarNotificacion_TemplateNullYBodyNull_LanzaExcepcion() {
        NotificationRequestDto dto = new NotificationRequestDto(
                "corr-123", "TEST_EVENT", "TEST_SERVICE", "NORMAL", "EMAIL",
                "test@example.com", "Test User", null,
                "Test Subject", null, Map.of(), null
        );
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(channelConfig));
        when(templateRepository.findByCodigo(null)).thenReturn(Optional.empty());
        when(templateRepository.findFirstByTipoEventoAndTipoCanalAndEstadoOrderByIdAsc("TEST_EVENT", TipoCanalEnum.EMAIL, EstadoBasicoEnum.ACTIVO))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> notificationService.solicitarNotificacion(dto));

        assertEquals("NOTIFICATION_BODY_REQUIRED", exception.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    // Tests para solicitarNotificacion con template null y body desde template
    @Test
    void testSolicitarNotificacion_TemplateNull_BodyDesdeTemplate() {
        NotificationRequestDto dto = new NotificationRequestDto(
                "corr-123", "TEST_EVENT", "TEST_SERVICE", "NORMAL", "EMAIL",
                "test@example.com", "Test User", null,
                "Test Subject", null, Map.of(), null
        );
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(channelConfig));
        when(templateRepository.findByCodigo(null)).thenReturn(Optional.empty());
        when(templateRepository.findFirstByTipoEventoAndTipoCanalAndEstadoOrderByIdAsc("TEST_EVENT", TipoCanalEnum.EMAIL, EstadoBasicoEnum.ACTIVO))
                .thenReturn(Optional.of(template));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);
        when(attemptRepository.countByNotificationRequestId(any())).thenReturn(0L);
        when(attemptRepository.save(any(NotificationDeliveryAttempt.class))).thenReturn(new NotificationDeliveryAttempt());
        when(smtpNotificationSender.send(any(NotificationRequest.class)))
                .thenReturn(new NotificationSendResult(true, "200", "OK", "SMTP"));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);

        NotificationResponse result = notificationService.solicitarNotificacion(dto);

        assertNotNull(result);
        verify(auditRepository).save(any(AuditoriaNotificationEvento.class));
    }

    // Tests para solicitarNotificacion con payload null
    @Test
    void testSolicitarNotificacion_PayloadNull_RetornaNotificacion() {
        NotificationRequestDto dto = new NotificationRequestDto(
                "corr-123", "TEST_EVENT", "TEST_SERVICE", "NORMAL", "EMAIL",
                "test@example.com", "Test User", "TEST_TEMPLATE",
                "Test Subject", "Test Body", null, null
        );
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(channelConfig));
        when(templateRepository.findByCodigo("TEST_TEMPLATE")).thenReturn(Optional.of(template));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);
        when(attemptRepository.countByNotificationRequestId(any())).thenReturn(0L);
        when(attemptRepository.save(any(NotificationDeliveryAttempt.class))).thenReturn(new NotificationDeliveryAttempt());
        when(smtpNotificationSender.send(any(NotificationRequest.class)))
                .thenReturn(new NotificationSendResult(true, "200", "OK", "SMTP"));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);

        NotificationResponse result = notificationService.solicitarNotificacion(dto);

        assertNotNull(result);
        verify(auditRepository).save(any(AuditoriaNotificationEvento.class));
    }

    // Tests para solicitarNotificacion con payload con valores null
    @Test
    void testSolicitarNotificacion_PayloadConValoresNull_RetornaNotificacion() {
        NotificationRequestDto dto = new NotificationRequestDto(
                "corr-123", "TEST_EVENT", "TEST_SERVICE", "NORMAL", "EMAIL",
                "test@example.com", "Test User", "TEST_TEMPLATE",
                "Test Subject", "Test Body", Map.of("key", "value"), null
        );
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(channelConfig));
        when(templateRepository.findByCodigo("TEST_TEMPLATE")).thenReturn(Optional.of(template));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);
        when(attemptRepository.countByNotificationRequestId(any())).thenReturn(0L);
        when(attemptRepository.save(any(NotificationDeliveryAttempt.class))).thenReturn(new NotificationDeliveryAttempt());
        when(smtpNotificationSender.send(any(NotificationRequest.class)))
                .thenReturn(new NotificationSendResult(true, "200", "OK", "SMTP"));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);

        NotificationResponse result = notificationService.solicitarNotificacion(dto);

        assertNotNull(result);
        verify(auditRepository).save(any(AuditoriaNotificationEvento.class));
    }

    // Tests para solicitarNotificacion con envío fallido
    @Test
    void testSolicitarNotificacion_EnvioFallido_RetornaNotificacion() {
        NotificationRequestDto dto = new NotificationRequestDto(
                "corr-123", "TEST_EVENT", "TEST_SERVICE", "NORMAL", "EMAIL",
                "test@example.com", "Test User", "TEST_TEMPLATE",
                "Test Subject", "Test Body", Map.of(), null
        );
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(channelConfig));
        when(templateRepository.findByCodigo("TEST_TEMPLATE")).thenReturn(Optional.of(template));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);
        when(attemptRepository.countByNotificationRequestId(any())).thenReturn(0L);
        when(attemptRepository.save(any(NotificationDeliveryAttempt.class))).thenReturn(new NotificationDeliveryAttempt());
        when(smtpNotificationSender.send(any(NotificationRequest.class)))
                .thenReturn(new NotificationSendResult(false, "500", "Error", "SMTP"));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);

        NotificationResponse result = notificationService.solicitarNotificacion(dto);

        assertNotNull(result);
        verify(auditRepository, atLeast(1)).save(any(AuditoriaNotificationEvento.class));
    }

    // Tests para solicitarNotificacion con intentos previos
    @Test
    void testSolicitarNotificacion_ConIntentosPrevios_RetornaNotificacion() {
        NotificationRequestDto dto = new NotificationRequestDto(
                "corr-123", "TEST_EVENT", "TEST_SERVICE", "NORMAL", "EMAIL",
                "test@example.com", "Test User", "TEST_TEMPLATE",
                "Test Subject", "Test Body", Map.of(), null
        );
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(channelConfig));
        when(templateRepository.findByCodigo("TEST_TEMPLATE")).thenReturn(Optional.of(template));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);
        when(attemptRepository.countByNotificationRequestId(any())).thenReturn(5L);
        when(attemptRepository.save(any(NotificationDeliveryAttempt.class))).thenReturn(new NotificationDeliveryAttempt());
        when(smtpNotificationSender.send(any(NotificationRequest.class)))
                .thenReturn(new NotificationSendResult(true, "200", "OK", "SMTP"));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);

        NotificationResponse result = notificationService.solicitarNotificacion(dto);

        assertNotNull(result);
        verify(auditRepository).save(any(AuditoriaNotificationEvento.class));
    }

    // Tests para solicitarNotificacion con template null y evento null
    @Test
    void testSolicitarNotificacion_TemplateNullYEventoNull_BodyDesdeTemplate() {
        NotificationRequestDto dto = new NotificationRequestDto(
                "corr-123", null, "TEST_SERVICE", "NORMAL", "EMAIL",
                "test@example.com", "Test User", null,
                "Test Subject", null, Map.of(), null
        );
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(channelConfig));
        when(templateRepository.findByCodigo(null)).thenReturn(Optional.empty());
        when(templateRepository.findFirstByTipoEventoAndTipoCanalAndEstadoOrderByIdAsc(null, TipoCanalEnum.EMAIL, EstadoBasicoEnum.ACTIVO))
                .thenReturn(Optional.of(template));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);
        when(attemptRepository.countByNotificationRequestId(any())).thenReturn(0L);
        when(attemptRepository.save(any(NotificationDeliveryAttempt.class))).thenReturn(new NotificationDeliveryAttempt());
        when(smtpNotificationSender.send(any(NotificationRequest.class)))
                .thenReturn(new NotificationSendResult(true, "200", "OK", "SMTP"));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);

        NotificationResponse result = notificationService.solicitarNotificacion(dto);

        assertNotNull(result);
        verify(auditRepository).save(any(AuditoriaNotificationEvento.class));
    }

    // Tests para solicitarNotificacion con template null y evento null sin template encontrado
    @Test
    void testSolicitarNotificacion_TemplateNullYEventoNull_SinTemplate_LanzaExcepcion() {
        NotificationRequestDto dto = new NotificationRequestDto(
                "corr-123", null, "TEST_SERVICE", "NORMAL", "EMAIL",
                "test@example.com", "Test User", null,
                "Test Subject", null, Map.of(), null
        );
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(channelConfig));
        when(templateRepository.findByCodigo(null)).thenReturn(Optional.empty());
        when(templateRepository.findFirstByTipoEventoAndTipoCanalAndEstadoOrderByIdAsc(null, TipoCanalEnum.EMAIL, EstadoBasicoEnum.ACTIVO))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> notificationService.solicitarNotificacion(dto));

        assertEquals("NOTIFICATION_BODY_REQUIRED", exception.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    // Tests para solicitarNotificacion con template null y evento específico
    @Test
    void testSolicitarNotificacion_TemplateNull_EventoEspecifico_BodyDesdeTemplate() {
        NotificationRequestDto dto = new NotificationRequestDto(
                "corr-123", "SPECIFIC_EVENT", "TEST_SERVICE", "NORMAL", "EMAIL",
                "test@example.com", "Test User", null,
                "Test Subject", null, Map.of(), null
        );
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(channelConfig));
        when(templateRepository.findByCodigo(null)).thenReturn(Optional.empty());
        when(templateRepository.findFirstByTipoEventoAndTipoCanalAndEstadoOrderByIdAsc("SPECIFIC_EVENT", TipoCanalEnum.EMAIL, EstadoBasicoEnum.ACTIVO))
                .thenReturn(Optional.of(template));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);
        when(attemptRepository.countByNotificationRequestId(any())).thenReturn(0L);
        when(attemptRepository.save(any(NotificationDeliveryAttempt.class))).thenReturn(new NotificationDeliveryAttempt());
        when(smtpNotificationSender.send(any(NotificationRequest.class)))
                .thenReturn(new NotificationSendResult(true, "200", "OK", "SMTP"));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);

        NotificationResponse result = notificationService.solicitarNotificacion(dto);

        assertNotNull(result);
        verify(auditRepository).save(any(AuditoriaNotificationEvento.class));
    }

    // Tests para solicitarNotificacion con template y evento específico
    @Test
    void testSolicitarNotificacion_TemplateYEventoEspecifico_RetornaNotificacion() {
        NotificationRequestDto dto = new NotificationRequestDto(
                "corr-123", "SPECIFIC_EVENT", "TEST_SERVICE", "NORMAL", "EMAIL",
                "test@example.com", "Test User", "TEST_TEMPLATE",
                "Test Subject", "Test Body", Map.of(), null
        );
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(channelConfig));
        when(templateRepository.findByCodigo("TEST_TEMPLATE")).thenReturn(Optional.of(template));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);
        when(attemptRepository.countByNotificationRequestId(any())).thenReturn(0L);
        when(attemptRepository.save(any(NotificationDeliveryAttempt.class))).thenReturn(new NotificationDeliveryAttempt());
        when(smtpNotificationSender.send(any(NotificationRequest.class)))
                .thenReturn(new NotificationSendResult(true, "200", "OK", "SMTP"));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);

        NotificationResponse result = notificationService.solicitarNotificacion(dto);

        assertNotNull(result);
        verify(auditRepository).save(any(AuditoriaNotificationEvento.class));
    }

    // Tests para solicitarNotificacion con canal diferente (SMS)
    @Test
    void testSolicitarNotificacion_CanalSMS_RetornaNotificacion() {
        NotificationRequestDto dto = new NotificationRequestDto(
                "corr-123", "TEST_EVENT", "TEST_SERVICE", "NORMAL", "SMS",
                "0987654321", "Test User", "TEST_TEMPLATE",
                "Test Subject", "Test Body", Map.of(), null
        );
        channelConfig.setTipoCanal(TipoCanalEnum.SMS);
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(channelConfig));
        when(templateRepository.findByCodigo("TEST_TEMPLATE")).thenReturn(Optional.of(template));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);
        when(attemptRepository.countByNotificationRequestId(any())).thenReturn(0L);
        when(attemptRepository.save(any(NotificationDeliveryAttempt.class))).thenReturn(new NotificationDeliveryAttempt());
        when(smtpNotificationSender.send(any(NotificationRequest.class)))
                .thenReturn(new NotificationSendResult(true, "200", "OK", "SMS"));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);

        NotificationResponse result = notificationService.solicitarNotificacion(dto);

        assertNotNull(result);
        verify(auditRepository).save(any(AuditoriaNotificationEvento.class));
    }

    // Tests para solicitarNotificacion con canal diferente (PUSH)
    @Test
    void testSolicitarNotificacion_CanalPUSH_RetornaNotificacion() {
        NotificationRequestDto dto = new NotificationRequestDto(
                "corr-123", "TEST_EVENT", "TEST_SERVICE", "NORMAL", "PUSH",
                "device-token", "Test User", "TEST_TEMPLATE",
                "Test Subject", "Test Body", Map.of(), null
        );
        channelConfig.setTipoCanal(TipoCanalEnum.PUSH);
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(channelConfig));
        when(templateRepository.findByCodigo("TEST_TEMPLATE")).thenReturn(Optional.of(template));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);
        when(attemptRepository.countByNotificationRequestId(any())).thenReturn(0L);
        when(attemptRepository.save(any(NotificationDeliveryAttempt.class))).thenReturn(new NotificationDeliveryAttempt());
        when(smtpNotificationSender.send(any(NotificationRequest.class)))
                .thenReturn(new NotificationSendResult(true, "200", "OK", "PUSH"));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);

        NotificationResponse result = notificationService.solicitarNotificacion(dto);

        assertNotNull(result);
        verify(auditRepository).save(any(AuditoriaNotificationEvento.class));
    }

    // Tests para solicitarNotificacion con prioridad ALTA
    @Test
    void testSolicitarNotificacion_PrioridadAlta_RetornaNotificacion() {
        NotificationRequestDto dto = new NotificationRequestDto(
                "corr-123", "TEST_EVENT", "TEST_SERVICE", "ALTA", "EMAIL",
                "test@example.com", "Test User", "TEST_TEMPLATE",
                "Test Subject", "Test Body", Map.of(), null
        );
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(channelConfig));
        when(templateRepository.findByCodigo("TEST_TEMPLATE")).thenReturn(Optional.of(template));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);
        when(attemptRepository.countByNotificationRequestId(any())).thenReturn(0L);
        when(attemptRepository.save(any(NotificationDeliveryAttempt.class))).thenReturn(new NotificationDeliveryAttempt());
        when(smtpNotificationSender.send(any(NotificationRequest.class)))
                .thenReturn(new NotificationSendResult(true, "200", "OK", "SMTP"));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);

        NotificationResponse result = notificationService.solicitarNotificacion(dto);

        assertNotNull(result);
        verify(auditRepository).save(any(AuditoriaNotificationEvento.class));
    }

    // Tests para solicitarNotificacion con prioridad BAJA
    @Test
    void testSolicitarNotificacion_PrioridadBaja_RetornaNotificacion() {
        NotificationRequestDto dto = new NotificationRequestDto(
                "corr-123", "TEST_EVENT", "TEST_SERVICE", "BAJA", "EMAIL",
                "test@example.com", "Test User", "TEST_TEMPLATE",
                "Test Subject", "Test Body", Map.of(), null
        );
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(channelConfig));
        when(templateRepository.findByCodigo("TEST_TEMPLATE")).thenReturn(Optional.of(template));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);
        when(attemptRepository.countByNotificationRequestId(any())).thenReturn(0L);
        when(attemptRepository.save(any(NotificationDeliveryAttempt.class))).thenReturn(new NotificationDeliveryAttempt());
        when(smtpNotificationSender.send(any(NotificationRequest.class)))
                .thenReturn(new NotificationSendResult(true, "200", "OK", "SMTP"));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);

        NotificationResponse result = notificationService.solicitarNotificacion(dto);

        assertNotNull(result);
        verify(auditRepository).save(any(AuditoriaNotificationEvento.class));
    }

    // Tests para solicitarNotificacion con cuerpo con espacios
    @Test
    void testSolicitarNotificacion_CuerpoConEspacios_LanzaExcepcion() {
        NotificationRequestDto dto = new NotificationRequestDto(
                "corr-123", "TEST_EVENT", "TEST_SERVICE", "NORMAL", "EMAIL",
                "test@example.com", "Test User", "TEST_TEMPLATE",
                "Test Subject", "   ", Map.of(), null
        );
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(channelConfig));
        when(templateRepository.findByCodigo("TEST_TEMPLATE")).thenReturn(Optional.of(template));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> notificationService.solicitarNotificacion(dto));

        assertEquals("NOTIFICATION_BODY_REQUIRED", exception.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    // Tests para solicitarNotificacion con evidencia document
    @Test
    void testSolicitarNotificacion_ConEvidenciaDocument_RetornaNotificacion() {
        NotificationRequestDto dto = new NotificationRequestDto(
                "corr-123", "TEST_EVENT", "TEST_SERVICE", "NORMAL", "EMAIL",
                "test@example.com", "Test User", "TEST_TEMPLATE",
                "Test Subject", "Test Body", Map.of(), "evidence-uuid"
        );
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(channelConfig));
        when(templateRepository.findByCodigo("TEST_TEMPLATE")).thenReturn(Optional.of(template));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);
        when(attemptRepository.countByNotificationRequestId(any())).thenReturn(0L);
        when(attemptRepository.save(any(NotificationDeliveryAttempt.class))).thenReturn(new NotificationDeliveryAttempt());
        when(smtpNotificationSender.send(any(NotificationRequest.class)))
                .thenReturn(new NotificationSendResult(true, "200", "OK", "SMTP"));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);

        NotificationResponse result = notificationService.solicitarNotificacion(dto);

        assertNotNull(result);
        verify(auditRepository).save(any(AuditoriaNotificationEvento.class));
    }

    // Tests para solicitarNotificacion con cuerpo vacío y template null
    @Test
    void testSolicitarNotificacion_CuerpoVacioYTemplateNull_LanzaExcepcion() {
        NotificationRequestDto dto = new NotificationRequestDto(
                "corr-123", "TEST_EVENT", "TEST_SERVICE", "NORMAL", "EMAIL",
                "test@example.com", "Test User", null,
                "Test Subject", "", Map.of(), null
        );
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(channelConfig));
        when(templateRepository.findByCodigo(null)).thenReturn(Optional.empty());
        when(templateRepository.findFirstByTipoEventoAndTipoCanalAndEstadoOrderByIdAsc("TEST_EVENT", TipoCanalEnum.EMAIL, EstadoBasicoEnum.ACTIVO))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> notificationService.solicitarNotificacion(dto));

        assertEquals("NOTIFICATION_BODY_REQUIRED", exception.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    // Tests para solicitarNotificacion con cuerpo vacío y template encontrado
    @Test
    void testSolicitarNotificacion_CuerpoVacioYTemplateEncontrado_LanzaExcepcion() {
        NotificationRequestDto dto = new NotificationRequestDto(
                "corr-123", "TEST_EVENT", "TEST_SERVICE", "NORMAL", "EMAIL",
                "test@example.com", "Test User", null,
                "Test Subject", "", Map.of(), null
        );
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(channelConfig));
        when(templateRepository.findByCodigo(null)).thenReturn(Optional.empty());
        when(templateRepository.findFirstByTipoEventoAndTipoCanalAndEstadoOrderByIdAsc("TEST_EVENT", TipoCanalEnum.EMAIL, EstadoBasicoEnum.ACTIVO))
                .thenReturn(Optional.of(template));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> notificationService.solicitarNotificacion(dto));

        assertEquals("NOTIFICATION_BODY_REQUIRED", exception.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    // Tests para crearTemplate con diferentes idiomas
    @Test
    void testCrearTemplate_IdiomaIngles_RetornaCreado() {
        TemplateRequest templateRequest = new TemplateRequest("NEW_TEMPLATE", "TEST_EVENT", "EMAIL", "New Subject", "New Body", "EN");
        when(templateRepository.existsByCodigo("NEW_TEMPLATE")).thenReturn(false);
        when(templateRepository.save(any(NotificationTemplate.class))).thenReturn(template);

        TemplateResponse result = notificationService.crearTemplate(templateRequest);

        assertNotNull(result);
        verify(auditRepository).save(any(AuditoriaNotificationEvento.class));
    }

    // Tests para crearTemplate con idioma español
    @Test
    void testCrearTemplate_IdiomaEspanol_RetornaCreado() {
        TemplateRequest templateRequest = new TemplateRequest("NEW_TEMPLATE", "TEST_EVENT", "EMAIL", "Nuevo Asunto", "Nuevo Cuerpo", "ES");
        when(templateRepository.existsByCodigo("NEW_TEMPLATE")).thenReturn(false);
        when(templateRepository.save(any(NotificationTemplate.class))).thenReturn(template);

        TemplateResponse result = notificationService.crearTemplate(templateRequest);

        assertNotNull(result);
        verify(auditRepository).save(any(AuditoriaNotificationEvento.class));
    }

    // Tests para actualizarTemplate con solo subject
    @Test
    void testActualizarTemplate_SoloSubject_RetornaActualizado() {
        TemplateUpdateRequest updateRequest = new TemplateUpdateRequest("Updated Subject", null, null);
        when(templateRepository.findByCodigo("TEST_TEMPLATE")).thenReturn(Optional.of(template));
        when(templateRepository.save(any(NotificationTemplate.class))).thenReturn(template);

        TemplateResponse result = notificationService.actualizarTemplate("TEST_TEMPLATE", updateRequest);

        assertNotNull(result);
        verify(auditRepository).save(any(AuditoriaNotificationEvento.class));
    }

    // Tests para actualizarTemplate con solo body
    @Test
    void testActualizarTemplate_SoloBody_RetornaActualizado() {
        TemplateUpdateRequest updateRequest = new TemplateUpdateRequest(null, "Updated Body", null);
        when(templateRepository.findByCodigo("TEST_TEMPLATE")).thenReturn(Optional.of(template));
        when(templateRepository.save(any(NotificationTemplate.class))).thenReturn(template);

        TemplateResponse result = notificationService.actualizarTemplate("TEST_TEMPLATE", updateRequest);

        assertNotNull(result);
        verify(auditRepository).save(any(AuditoriaNotificationEvento.class));
    }

    // Tests para actualizarTemplate con solo estado
    @Test
    void testActualizarTemplate_SoloEstado_RetornaActualizado() {
        TemplateUpdateRequest updateRequest = new TemplateUpdateRequest(null, null, "INACTIVO");
        when(templateRepository.findByCodigo("TEST_TEMPLATE")).thenReturn(Optional.of(template));
        when(templateRepository.save(any(NotificationTemplate.class))).thenReturn(template);

        TemplateResponse result = notificationService.actualizarTemplate("TEST_TEMPLATE", updateRequest);

        assertNotNull(result);
        verify(auditRepository).save(any(AuditoriaNotificationEvento.class));
    }

    // Tests para listarTemplates con múltiples templates
    @Test
    void testListarTemplates_MultiplesTemplates_RetornaLista() {
        NotificationTemplate template2 = NotificationTemplate.crear("TEST_TEMPLATE2", "TEST_EVENT2", TipoCanalEnum.EMAIL, "Test Subject 2", "Test Body 2", "ES");
        when(templateRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(template, template2));

        List<TemplateResponse> result = notificationService.listarTemplates(null);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    // Tests para listarTemplates con filtro y múltiples templates
    @Test
    void testListarTemplates_FiltroYMultiplesTemplates_RetornaFiltrados() {
        NotificationTemplate template2 = NotificationTemplate.crear("TEST_TEMPLATE2", "TEST_EVENT", TipoCanalEnum.EMAIL, "Test Subject 2", "Test Body 2", "ES");
        when(templateRepository.findByTipoEventoAndEstadoOrderByCodigoAsc("TEST_EVENT", EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(template, template2));

        List<TemplateResponse> result = notificationService.listarTemplates("TEST_EVENT");

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    // Tests para listarIntentos con múltiples intentos
    @Test
    void testListarIntentos_MultiplesIntentos_RetornaLista() {
        NotificationDeliveryAttempt attempt1 = NotificationDeliveryAttempt.crear(request, 1, EstadoIntentoEntregaEnum.ENVIADA, "200", "OK");
        NotificationDeliveryAttempt attempt2 = NotificationDeliveryAttempt.crear(request, 2, EstadoIntentoEntregaEnum.ENVIADA, "200", "OK");
        when(requestRepository.findByUuidNotificacion("uuid-123")).thenReturn(Optional.of(request));
        when(attemptRepository.findByNotificationRequestIdOrderByIntentoNumeroAsc(any()))
                .thenReturn(List.of(attempt1, attempt2));

        List<DeliveryAttemptResponse> result = notificationService.listarIntentos("uuid-123");

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    // Tests para solicitarNotificacion con múltiples intentos previos
    @Test
    void testSolicitarNotificacion_MultiplesIntentosPrevios_RetornaNotificacion() {
        NotificationRequestDto dto = new NotificationRequestDto(
                "corr-123", "TEST_EVENT", "TEST_SERVICE", "NORMAL", "EMAIL",
                "test@example.com", "Test User", "TEST_TEMPLATE",
                "Test Subject", "Test Body", Map.of(), null
        );
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(channelConfig));
        when(templateRepository.findByCodigo("TEST_TEMPLATE")).thenReturn(Optional.of(template));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);
        when(attemptRepository.countByNotificationRequestId(any())).thenReturn(10L);
        when(attemptRepository.save(any(NotificationDeliveryAttempt.class))).thenReturn(new NotificationDeliveryAttempt());
        when(smtpNotificationSender.send(any(NotificationRequest.class)))
                .thenReturn(new NotificationSendResult(true, "200", "OK", "SMTP"));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);

        NotificationResponse result = notificationService.solicitarNotificacion(dto);

        assertNotNull(result);
        verify(auditRepository).save(any(AuditoriaNotificationEvento.class));
    }

    // Tests para solicitarNotificacion con cuerpo desde template con payload
    @Test
    void testSolicitarNotificacion_CuerpoDesdeTemplateConPayload_RetornaNotificacion() {
        NotificationRequestDto dto = new NotificationRequestDto(
                "corr-123", "TEST_EVENT", "TEST_SERVICE", "NORMAL", "EMAIL",
                "test@example.com", "Test User", "TEST_TEMPLATE",
                null, null, Map.of("nombre", "Juan", "monto", "100.00"), null
        );
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(channelConfig));
        when(templateRepository.findByCodigo("TEST_TEMPLATE")).thenReturn(Optional.of(template));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);
        when(attemptRepository.countByNotificationRequestId(any())).thenReturn(0L);
        when(attemptRepository.save(any(NotificationDeliveryAttempt.class))).thenReturn(new NotificationDeliveryAttempt());
        when(smtpNotificationSender.send(any(NotificationRequest.class)))
                .thenReturn(new NotificationSendResult(true, "200", "OK", "SMTP"));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);

        NotificationResponse result = notificationService.solicitarNotificacion(dto);

        assertNotNull(result);
        verify(auditRepository).save(any(AuditoriaNotificationEvento.class));
    }

    // Tests para solicitarNotificacion con subject desde template con payload
    @Test
    void testSolicitarNotificacion_SubjectDesdeTemplateConPayload_RetornaNotificacion() {
        NotificationRequestDto dto = new NotificationRequestDto(
                "corr-123", "TEST_EVENT", "TEST_SERVICE", "NORMAL", "EMAIL",
                "test@example.com", "Test User", "TEST_TEMPLATE",
                null, "Test Body", Map.of("nombre", "Juan", "monto", "100.00"), null
        );
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(channelConfig));
        when(templateRepository.findByCodigo("TEST_TEMPLATE")).thenReturn(Optional.of(template));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);
        when(attemptRepository.countByNotificationRequestId(any())).thenReturn(0L);
        when(attemptRepository.save(any(NotificationDeliveryAttempt.class))).thenReturn(new NotificationDeliveryAttempt());
        when(smtpNotificationSender.send(any(NotificationRequest.class)))
                .thenReturn(new NotificationSendResult(true, "200", "OK", "SMTP"));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);

        NotificationResponse result = notificationService.solicitarNotificacion(dto);

        assertNotNull(result);
        verify(auditRepository).save(any(AuditoriaNotificationEvento.class));
    }

    // Tests para solicitarNotificacion con subject y body desde template con payload
    @Test
    void testSolicitarNotificacion_SubjectYBodyDesdeTemplateConPayload_RetornaNotificacion() {
        NotificationRequestDto dto = new NotificationRequestDto(
                "corr-123", "TEST_EVENT", "TEST_SERVICE", "NORMAL", "EMAIL",
                "test@example.com", "Test User", "TEST_TEMPLATE",
                null, null, Map.of("nombre", "Juan", "monto", "100.00"), null
        );
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(channelConfig));
        when(templateRepository.findByCodigo("TEST_TEMPLATE")).thenReturn(Optional.of(template));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);
        when(attemptRepository.countByNotificationRequestId(any())).thenReturn(0L);
        when(attemptRepository.save(any(NotificationDeliveryAttempt.class))).thenReturn(new NotificationDeliveryAttempt());
        when(smtpNotificationSender.send(any(NotificationRequest.class)))
                .thenReturn(new NotificationSendResult(true, "200", "OK", "SMTP"));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);

        NotificationResponse result = notificationService.solicitarNotificacion(dto);

        assertNotNull(result);
        verify(auditRepository).save(any(AuditoriaNotificationEvento.class));
    }

    // Tests para solicitarNotificacion con template null y body desde template con payload
    @Test
    void testSolicitarNotificacion_TemplateNull_BodyDesdeTemplateConPayload_RetornaNotificacion() {
        NotificationRequestDto dto = new NotificationRequestDto(
                "corr-123", "TEST_EVENT", "TEST_SERVICE", "NORMAL", "EMAIL",
                "test@example.com", "Test User", null,
                null, null, Map.of("nombre", "Juan", "monto", "100.00"), null
        );
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(channelConfig));
        when(templateRepository.findByCodigo(null)).thenReturn(Optional.empty());
        when(templateRepository.findFirstByTipoEventoAndTipoCanalAndEstadoOrderByIdAsc("TEST_EVENT", TipoCanalEnum.EMAIL, EstadoBasicoEnum.ACTIVO))
                .thenReturn(Optional.of(template));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);
        when(attemptRepository.countByNotificationRequestId(any())).thenReturn(0L);
        when(attemptRepository.save(any(NotificationDeliveryAttempt.class))).thenReturn(new NotificationDeliveryAttempt());
        when(smtpNotificationSender.send(any(NotificationRequest.class)))
                .thenReturn(new NotificationSendResult(true, "200", "OK", "SMTP"));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);

        NotificationResponse result = notificationService.solicitarNotificacion(dto);

        assertNotNull(result);
        verify(auditRepository).save(any(AuditoriaNotificationEvento.class));
    }

    // Tests para solicitarNotificacion con prioridad vacía y canal diferente
    @Test
    void testSolicitarNotificacion_PrioridadVacia_CanalSMS_RetornaNotificacion() {
        NotificationRequestDto dto = new NotificationRequestDto(
                "corr-123", "TEST_EVENT", "TEST_SERVICE", "", "SMS",
                "0987654321", "Test User", "TEST_TEMPLATE",
                "Test Subject", "Test Body", Map.of(), null
        );
        channelConfig.setTipoCanal(TipoCanalEnum.SMS);
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(channelConfig));
        when(templateRepository.findByCodigo("TEST_TEMPLATE")).thenReturn(Optional.of(template));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);
        when(attemptRepository.countByNotificationRequestId(any())).thenReturn(0L);
        when(attemptRepository.save(any(NotificationDeliveryAttempt.class))).thenReturn(new NotificationDeliveryAttempt());
        when(smtpNotificationSender.send(any(NotificationRequest.class)))
                .thenReturn(new NotificationSendResult(true, "200", "OK", "SMS"));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);

        NotificationResponse result = notificationService.solicitarNotificacion(dto);

        assertNotNull(result);
        verify(auditRepository).save(any(AuditoriaNotificationEvento.class));
    }

    // Tests para solicitarNotificacion con prioridad null y canal diferente
    @Test
    void testSolicitarNotificacion_PrioridadNull_CanalPUSH_RetornaNotificacion() {
        NotificationRequestDto dto = new NotificationRequestDto(
                "corr-123", "TEST_EVENT", "TEST_SERVICE", null, "PUSH",
                "device-token", "Test User", "TEST_TEMPLATE",
                "Test Subject", "Test Body", Map.of(), null
        );
        channelConfig.setTipoCanal(TipoCanalEnum.PUSH);
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(channelConfig));
        when(templateRepository.findByCodigo("TEST_TEMPLATE")).thenReturn(Optional.of(template));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);
        when(attemptRepository.countByNotificationRequestId(any())).thenReturn(0L);
        when(attemptRepository.save(any(NotificationDeliveryAttempt.class))).thenReturn(new NotificationDeliveryAttempt());
        when(smtpNotificationSender.send(any(NotificationRequest.class)))
                .thenReturn(new NotificationSendResult(true, "200", "OK", "PUSH"));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);

        NotificationResponse result = notificationService.solicitarNotificacion(dto);

        assertNotNull(result);
        verify(auditRepository).save(any(AuditoriaNotificationEvento.class));
    }

    // Tests para solicitarNotificacion con envío fallido y auditoría
    @Test
    void testSolicitarNotificacion_EnvioFallido_ConAuditoria_RetornaNotificacion() {
        NotificationRequestDto dto = new NotificationRequestDto(
                "corr-123", "TEST_EVENT", "TEST_SERVICE", "NORMAL", "EMAIL",
                "test@example.com", "Test User", "TEST_TEMPLATE",
                "Test Subject", "Test Body", Map.of(), null
        );
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(channelConfig));
        when(templateRepository.findByCodigo("TEST_TEMPLATE")).thenReturn(Optional.of(template));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);
        when(attemptRepository.countByNotificationRequestId(any())).thenReturn(0L);
        when(attemptRepository.save(any(NotificationDeliveryAttempt.class))).thenReturn(new NotificationDeliveryAttempt());
        when(smtpNotificationSender.send(any(NotificationRequest.class)))
                .thenReturn(new NotificationSendResult(false, "500", "Internal Server Error", "SMTP"));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);

        NotificationResponse result = notificationService.solicitarNotificacion(dto);

        assertNotNull(result);
        verify(auditRepository, atLeast(1)).save(any(AuditoriaNotificationEvento.class));
    }

    // Tests para solicitarNotificacionInterna
    @Test
    void testSolicitarNotificacionInterna_DatosValidos_RetornaNotificacion() {
        InternalNotificationRequest dto = new InternalNotificationRequest(
                "source-event-uuid-123", "corr-123", "TEST_EVENT", "TEST_SERVICE", "NORMAL", "EMAIL",
                "actor-uuid-123", "CUSTOMER", "test@example.com", "Test User", "TEST_TEMPLATE",
                "Test Subject", "Test Body", Map.of(), null
        );
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(channelConfig));
        when(requestRepository.findFirstByUuidEventoOrigenAndTipoEventoAndDestinatario(
                "source-event-uuid-123", "TEST_EVENT", "test@example.com")).thenReturn(Optional.empty());
        when(preferenceRepository.findByUuidActorAndTipoEventoAndTipoCanal("actor-uuid-123", "TEST_EVENT", TipoCanalEnum.EMAIL))
                .thenReturn(Optional.empty());
        when(preferenceRepository.findByUuidActorAndTipoEventoAndTipoCanal("actor-uuid-123", "*", TipoCanalEnum.EMAIL))
                .thenReturn(Optional.of(preference));
        when(templateRepository.findByCodigo("TEST_TEMPLATE")).thenReturn(Optional.of(template));
        when(requestRepository.saveAndFlush(any(NotificationRequest.class))).thenReturn(request);
        when(attemptRepository.countByNotificationRequestId(any())).thenReturn(0L);
        when(attemptRepository.save(any(NotificationDeliveryAttempt.class))).thenReturn(new NotificationDeliveryAttempt());
        when(smtpNotificationSender.send(any(NotificationRequest.class)))
                .thenReturn(new NotificationSendResult(true, "200", "OK", "SMTP"));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);

        NotificationResponse result = notificationService.solicitarNotificacionInterna(dto);

        assertNotNull(result);
        verify(auditRepository).save(any(AuditoriaNotificationEvento.class));
    }

    @Test
    void testSolicitarNotificacionInterna_DestinatarioExplicito_RetornaNotificacion() {
        InternalNotificationRequest dto = new InternalNotificationRequest(
                "source-event-uuid-123", "corr-123", "TEST_EVENT", "TEST_SERVICE", "NORMAL", "EMAIL",
                "actor-uuid-123", "CUSTOMER", "explicit@example.com", "Test User", "TEST_TEMPLATE",
                "Test Subject", "Test Body", Map.of(), null
        );
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(channelConfig));
        when(requestRepository.findFirstByUuidEventoOrigenAndTipoEventoAndDestinatario(
                "source-event-uuid-123", "TEST_EVENT", "explicit@example.com")).thenReturn(Optional.empty());
        when(templateRepository.findByCodigo("TEST_TEMPLATE")).thenReturn(Optional.of(template));
        when(requestRepository.saveAndFlush(any(NotificationRequest.class))).thenReturn(request);
        when(attemptRepository.countByNotificationRequestId(any())).thenReturn(0L);
        when(attemptRepository.save(any(NotificationDeliveryAttempt.class))).thenReturn(new NotificationDeliveryAttempt());
        when(smtpNotificationSender.send(any(NotificationRequest.class)))
                .thenReturn(new NotificationSendResult(true, "200", "OK", "SMTP"));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);

        NotificationResponse result = notificationService.solicitarNotificacionInterna(dto);

        assertNotNull(result);
        verify(auditRepository).save(any(AuditoriaNotificationEvento.class));
    }

    @Test
    void testSolicitarNotificacionInterna_SinActorNiDestinatario_LanzaExcepcion() {
        InternalNotificationRequest dto = new InternalNotificationRequest(
                "source-event-uuid-123", "corr-123", "TEST_EVENT", "TEST_SERVICE", "NORMAL", "EMAIL",
                null, "CUSTOMER", null, "Test User", "TEST_TEMPLATE",
                "Test Subject", "Test Body", Map.of(), null
        );
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(channelConfig));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> notificationService.solicitarNotificacionInterna(dto));

        assertEquals("NOTIFICATION_RECIPIENT_REQUIRED", exception.getCode());
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
    }

    @Test
    void testSolicitarNotificacionInterna_PreferenciaNoEncontrada_LanzaExcepcion() {
        InternalNotificationRequest dto = new InternalNotificationRequest(
                "source-event-uuid-123", "corr-123", "TEST_EVENT", "TEST_SERVICE", "NORMAL", "EMAIL",
                "actor-uuid-123", "CUSTOMER", null, "Test User", "TEST_TEMPLATE",
                "Test Subject", "Test Body", Map.of(), null
        );
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(channelConfig));
        when(preferenceRepository.findByUuidActorAndTipoEventoAndTipoCanal("actor-uuid-123", "TEST_EVENT", TipoCanalEnum.EMAIL))
                .thenReturn(Optional.empty());
        when(preferenceRepository.findByUuidActorAndTipoEventoAndTipoCanal("actor-uuid-123", "*", TipoCanalEnum.EMAIL))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> notificationService.solicitarNotificacionInterna(dto));

        assertEquals("NOTIFICATION_PREFERENCE_NOT_FOUND", exception.getCode());
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
    }

    @Test
    void testSolicitarNotificacionInterna_CuerpoVacio_LanzaExcepcion() {
        InternalNotificationRequest dto = new InternalNotificationRequest(
                "source-event-uuid-123", "corr-123", "TEST_EVENT", "TEST_SERVICE", "NORMAL", "EMAIL",
                "actor-uuid-123", "CUSTOMER", "test@example.com", "Test User", null,
                "Test Subject", null, Map.of(), null
        );
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(channelConfig));
        when(requestRepository.findFirstByUuidEventoOrigenAndTipoEventoAndDestinatario(
                "source-event-uuid-123", "TEST_EVENT", "test@example.com")).thenReturn(Optional.empty());
        when(preferenceRepository.findByUuidActorAndTipoEventoAndTipoCanal("actor-uuid-123", "TEST_EVENT", TipoCanalEnum.EMAIL))
                .thenReturn(Optional.of(preference));
        when(templateRepository.findByCodigo(null)).thenReturn(Optional.empty());
        when(templateRepository.findFirstByTipoEventoAndTipoCanalAndEstadoOrderByIdAsc("TEST_EVENT", TipoCanalEnum.EMAIL, EstadoBasicoEnum.ACTIVO))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> notificationService.solicitarNotificacionInterna(dto));

        assertEquals("NOTIFICATION_BODY_REQUIRED", exception.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void testSolicitarNotificacionInterna_NotificacionExistente_EnviaNuevamente() {
        InternalNotificationRequest dto = new InternalNotificationRequest(
                "source-event-uuid-123", "corr-123", "TEST_EVENT", "TEST_SERVICE", "NORMAL", "EMAIL",
                "actor-uuid-123", "CUSTOMER", "test@example.com", "Test User", "TEST_TEMPLATE",
                "Test Subject", "Test Body", Map.of(), null
        );
        request.setEstado(EstadoNotificacionEnum.PENDIENTE);
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(channelConfig));
        when(requestRepository.findFirstByUuidEventoOrigenAndTipoEventoAndDestinatario(
                "source-event-uuid-123", "TEST_EVENT", "test@example.com")).thenReturn(Optional.of(request));
        when(attemptRepository.countByNotificationRequestId(any())).thenReturn(0L);
        when(attemptRepository.save(any(NotificationDeliveryAttempt.class))).thenReturn(new NotificationDeliveryAttempt());
        when(smtpNotificationSender.send(any(NotificationRequest.class)))
                .thenReturn(new NotificationSendResult(true, "200", "OK", "SMTP"));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);

        NotificationResponse result = notificationService.solicitarNotificacionInterna(dto);

        assertNotNull(result);
        verify(attemptRepository).save(any(NotificationDeliveryAttempt.class));
    }

    @Test
    void testSolicitarNotificacionInterna_NotificacionYaEnviada_RetornaExistente() {
        InternalNotificationRequest dto = new InternalNotificationRequest(
                "source-event-uuid-123", "corr-123", "TEST_EVENT", "TEST_SERVICE", "NORMAL", "EMAIL",
                "actor-uuid-123", "CUSTOMER", "test@example.com", "Test User", "TEST_TEMPLATE",
                "Test Subject", "Test Body", Map.of(), null
        );
        request.setEstado(EstadoNotificacionEnum.ENVIADA);
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(channelConfig));
        when(requestRepository.findFirstByUuidEventoOrigenAndTipoEventoAndDestinatario(
                "source-event-uuid-123", "TEST_EVENT", "test@example.com")).thenReturn(Optional.of(request));

        NotificationResponse result = notificationService.solicitarNotificacionInterna(dto);

        assertNotNull(result);
        verify(smtpNotificationSender, never()).send(any(NotificationRequest.class));
    }

    @Test
    void testSolicitarNotificacionInterna_CanalNoDisponible_LanzaExcepcion() {
        InternalNotificationRequest dto = new InternalNotificationRequest(
                "source-event-uuid-123", "corr-123", "TEST_EVENT", "TEST_SERVICE", "NORMAL", "EMAIL",
                "actor-uuid-123", "CUSTOMER", "test@example.com", "Test User", "TEST_TEMPLATE",
                "Test Subject", "Test Body", Map.of(), null
        );
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> notificationService.solicitarNotificacionInterna(dto));

        assertEquals("NOTIFICATION_CHANNEL_NOT_AVAILABLE", exception.getCode());
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
    }

    @Test
    void testSolicitarNotificacionInterna_PrioridadInvalida_LanzaExcepcion() {
        InternalNotificationRequest dto = new InternalNotificationRequest(
                "source-event-uuid-123", "corr-123", "TEST_EVENT", "TEST_SERVICE", "INVALID", "EMAIL",
                "actor-uuid-123", "CUSTOMER", "test@example.com", "Test User", "TEST_TEMPLATE",
                "Test Subject", "Test Body", Map.of(), null
        );
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(channelConfig));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> notificationService.solicitarNotificacionInterna(dto));

        assertEquals("NOTIFICATION_PRIORITY_INVALID", exception.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    // Tests para probarNotificacion
    @Test
    void testProbarNotificacion_DatosValidos_RetornaNotificacion() {
        TestNotificationRequest testRequest = new TestNotificationRequest(
                "TEST_EVENT", "test@example.com", "Test User", "TEST_TEMPLATE", Map.of("nombre", "Juan")
        );
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(channelConfig));
        when(templateRepository.findByCodigo("TEST_TEMPLATE")).thenReturn(Optional.of(template));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);
        when(attemptRepository.countByNotificationRequestId(any())).thenReturn(0L);
        when(attemptRepository.save(any(NotificationDeliveryAttempt.class))).thenReturn(new NotificationDeliveryAttempt());
        when(smtpNotificationSender.send(any(NotificationRequest.class)))
                .thenReturn(new NotificationSendResult(true, "200", "OK", "SMTP"));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);

        NotificationResponse result = notificationService.probarNotificacion(testRequest);

        assertNotNull(result);
        verify(auditRepository).save(any(AuditoriaNotificationEvento.class));
    }

    @Test
    void testProbarNotificacion_SinEventType_UsaDefault() {
        TestNotificationRequest testRequest = new TestNotificationRequest(
                null, "test@example.com", "Test User", "TEST_TEMPLATE", Map.of("nombre", "Juan")
        );
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(channelConfig));
        when(templateRepository.findByCodigo("TEST_TEMPLATE")).thenReturn(Optional.of(template));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);
        when(attemptRepository.countByNotificationRequestId(any())).thenReturn(0L);
        when(attemptRepository.save(any(NotificationDeliveryAttempt.class))).thenReturn(new NotificationDeliveryAttempt());
        when(smtpNotificationSender.send(any(NotificationRequest.class)))
                .thenReturn(new NotificationSendResult(true, "200", "OK", "SMTP"));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);

        NotificationResponse result = notificationService.probarNotificacion(testRequest);

        assertNotNull(result);
        verify(auditRepository).save(any(AuditoriaNotificationEvento.class));
    }

    @Test
    void testProbarNotificacion_EventTypeVacio_UsaDefault() {
        TestNotificationRequest testRequest = new TestNotificationRequest(
                "", "test@example.com", "Test User", "TEST_TEMPLATE", Map.of("nombre", "Juan")
        );
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(channelConfig));
        when(templateRepository.findByCodigo("TEST_TEMPLATE")).thenReturn(Optional.of(template));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);
        when(attemptRepository.countByNotificationRequestId(any())).thenReturn(0L);
        when(attemptRepository.save(any(NotificationDeliveryAttempt.class))).thenReturn(new NotificationDeliveryAttempt());
        when(smtpNotificationSender.send(any(NotificationRequest.class)))
                .thenReturn(new NotificationSendResult(true, "200", "OK", "SMTP"));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);

        NotificationResponse result = notificationService.probarNotificacion(testRequest);

        assertNotNull(result);
        verify(auditRepository).save(any(AuditoriaNotificationEvento.class));
    }

    @Test
    void testProbarNotificacion_SinPayload_UsaDefault() {
        TestNotificationRequest testRequest = new TestNotificationRequest(
                "TEST_EVENT", "test@example.com", "Test User", "TEST_TEMPLATE", null
        );
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(channelConfig));
        when(templateRepository.findByCodigo("TEST_TEMPLATE")).thenReturn(Optional.of(template));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);
        when(attemptRepository.countByNotificationRequestId(any())).thenReturn(0L);
        when(attemptRepository.save(any(NotificationDeliveryAttempt.class))).thenReturn(new NotificationDeliveryAttempt());
        when(smtpNotificationSender.send(any(NotificationRequest.class)))
                .thenReturn(new NotificationSendResult(true, "200", "OK", "SMTP"));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);

        NotificationResponse result = notificationService.probarNotificacion(testRequest);

        assertNotNull(result);
        verify(auditRepository).save(any(AuditoriaNotificationEvento.class));
    }

    @Test
    void testProbarNotificacion_SinRecipientName_UsaDefault() {
        TestNotificationRequest testRequest = new TestNotificationRequest(
                "TEST_EVENT", "test@example.com", null, "TEST_TEMPLATE", Map.of("nombre", "Juan")
        );
        when(channelRepository.findByEstadoOrderByCodigoAsc(EstadoBasicoEnum.ACTIVO))
                .thenReturn(List.of(channelConfig));
        when(templateRepository.findByCodigo("TEST_TEMPLATE")).thenReturn(Optional.of(template));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);
        when(attemptRepository.countByNotificationRequestId(any())).thenReturn(0L);
        when(attemptRepository.save(any(NotificationDeliveryAttempt.class))).thenReturn(new NotificationDeliveryAttempt());
        when(smtpNotificationSender.send(any(NotificationRequest.class)))
                .thenReturn(new NotificationSendResult(true, "200", "OK", "SMTP"));
        when(requestRepository.save(any(NotificationRequest.class))).thenReturn(request);

        NotificationResponse result = notificationService.probarNotificacion(testRequest);

        assertNotNull(result);
        verify(auditRepository).save(any(AuditoriaNotificationEvento.class));
    }
}
