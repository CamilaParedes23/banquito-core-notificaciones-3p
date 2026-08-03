USE banquito_notification_db;

INSERT IGNORE INTO NOTIFICATION_CHANNEL_CONFIG (CODIGO,TIPO_CANAL,NOMBRE,CONFIG_JSON) VALUES
('EMAIL_SMTP_REAL','EMAIL','SMTP Transaccional', JSON_OBJECT('provider','SMTP','configuration','environment-variables'));

INSERT IGNORE INTO NOTIFICATION_TEMPLATE (CODIGO,TIPO_EVENTO,TIPO_CANAL,ASUNTO_TEMPLATE,CUERPO_TEMPLATE,IDIOMA) VALUES
('TEMPORARY_CREDENTIALS_EMAIL','TEMPORARY_CREDENTIALS','EMAIL','Credenciales temporales Banco BanQuito','Estimado/a {{nombre}}, su usuario es {{usuario}} y su código temporal es {{codigoTemporal}}. Por seguridad, deberá cambiar su contraseña en el primer ingreso.','es-EC'),
('PASSWORD_RESET_CODE_EMAIL','PASSWORD_RESET_CODE','EMAIL','Código de recuperación de contraseña','Estimado/a {{nombre}}, su código para recuperar contraseña es {{codigoTemporal}}. Si usted no solicitó este cambio, comuníquese con Banco BanQuito.','es-EC'),
('TRANSFER_SENT_EMAIL','TRANSFER_SENT','EMAIL','Transferencia enviada','Estimado/a {{nombre}}, se envió una transferencia por {{monto}} desde la cuenta {{cuentaOrigen}} hacia {{cuentaDestino}}.','es-EC'),
('TRANSFER_RECEIVED_EMAIL','TRANSFER_RECEIVED','EMAIL','Transferencia recibida','Estimado/a {{nombre}}, recibió una transferencia por {{monto}} en la cuenta {{cuentaDestino}} desde {{ordenante}}.','es-EC'),
('ACCOUNT_WITHDRAWAL_EMAIL','ACCOUNT_WITHDRAWAL','EMAIL','Retiro realizado','Estimado/a {{nombre}}, se realizó un retiro por {{monto}} de la cuenta {{cuenta}}.','es-EC'),
('ACCOUNT_BLOCKED_EMAIL','ACCOUNT_BLOCKED','EMAIL','Cuenta bloqueada','Estimado/a {{nombre}}, la cuenta {{cuenta}} fue bloqueada por motivo: {{motivo}}.','es-EC'),
('PAYMENT_BATCH_COMPLETED_EMAIL','PAYMENT_BATCH_COMPLETED','EMAIL','Lote de pagos masivos procesado','El lote {{lote}} de la empresa {{empresa}} finalizó. Exitosos: {{exitosos}}, rechazados: {{rechazados}}.','es-EC'),
('PAYMENT_BATCH_REJECTED_EMAIL','PAYMENT_BATCH_REJECTED','EMAIL','Lote de pagos masivos rechazado','El lote {{lote}} de la empresa {{empresa}} fue rechazado. Motivo: {{motivo}}.','es-EC'),
('EOD_FAILED_EMAIL','EOD_FAILED','EMAIL','Cierre diario con error','El proceso EOD {{eod}} finalizó con error. Motivo: {{motivo}}.','es-EC'),
('LOGIN_SUSPICIOUS_EMAIL','LOGIN_SUSPICIOUS','EMAIL','Alerta de seguridad Banco BanQuito','Detectamos un evento de seguridad en el usuario {{usuario}}. Detalle: {{detalle}}.','es-EC');
