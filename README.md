# Guia Despacho Backend

## Descripción

Backend Java Spring Boot para un sistema de gestión de guías de despacho.

Este proyecto implementa:
- API REST para crear, consultar, actualizar, eliminar y descargar guías.
- Publicación asíncrona de mensajes en RabbitMQ para procesar guías.
- Consumidor RabbitMQ que genera PDFs, almacena documentos en AWS S3 y persiste metadatos en Oracle Cloud.
- Autenticación y autorización JWT con Spring Security y Azure AD B2C.
- Soporte para Dead Letter Queue (DLQ) en RabbitMQ.

## Arquitectura

La aplicación está organizada en capas claras:
- `controller`: Exposición de endpoints REST.
- `service`: Lógica de negocio, orquestación de mensajes y acceso a S3.
- `messaging`: Productor y consumidor RabbitMQ.
- `config`: Configuración de seguridad, RabbitMQ y AWS S3.
- `persistence`: Repositorio JPA para Oracle Cloud.
- `dto`: Objetos de transferencia de datos.
- `model`: Representación de la guía de despacho.

## Flujo principal

1. El cliente llama a `POST /api/guias` o `PUT /api/guias/{id}`.
2. `GuiaDespachoService` valida y publica un `GuiaMensaje` en RabbitMQ gracias a `GuiaMessageProducer`.
3. El consumidor `GuiaMessageConsumer` lee la cola `guias-queue`.
4. El consumidor genera el PDF con `PdfGeneratorService`.
5. Guarda el PDF y la metadata en AWS S3 usando `S3StorageService`.
6. Persiste los datos procesados en Oracle Cloud usando `GuiaProcesadaRepository`.
7. Si ocurre una excepción durante el procesamiento, RabbitMQ envía el mensaje a la DLQ `guias-dlq`.

## Dependencias principales

- Java 17
- Spring Boot 3.3.4
- Spring Web
- Spring Validation
- Spring Security
- Spring OAuth2 Resource Server
- Spring AMQP (RabbitMQ)
- Spring Data JPA
- Oracle JDBC y Oracle PKI
- AWS SDK v2 para S3
- Apache PDFBox
- Jackson JSR-310

## Requisitos previos

- Java 17 instalado.
- Maven disponible (`mvn`).
- RabbitMQ accesible.
- Bucket AWS S3 configurado.
- Oracle Cloud Autonomous Database / Oracle con wallet si se usa conexión segura.
- Azure AD B2C para emitir JWT, o un issuer JWT compatible.

## Configuración

Los valores principales se configuran en `src/main/resources/application.properties` mediante variables de entorno.

### Variables de entorno clave

- `AZURE_AD_B2C_ISSUER_URI`: issuer JWT de Azure AD B2C.
- `AWS_S3_BUCKET_NAME`: nombre del bucket S3.
- `AWS_REGION`: región de AWS.
- `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN`: credenciales AWS.
- `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USER`, `RABBITMQ_PASS`: conexión RabbitMQ.
- `ORACLE_URL`: URL JDBC para Oracle.
- `ORACLE_USER`, `ORACLE_PASS`: credenciales Oracle.

### Ajustes importantes

- `spring.rabbitmq.listener.simple.default-requeue-rejected=false`: evita la reencolación en caso de errores y permite el uso de la DLQ.
- `spring.jpa.hibernate.ddl-auto=none`: no autoriza cambios automáticos de esquema.

## Ejecutar local

1. Configura las variables de entorno necesarias.
2. Construye el JAR:

```powershell
mvn clean package
```

3. Ejecuta la aplicación:

```powershell
java -jar target\guia-despacho-backend.jar
```

4. Accede en `http://localhost:8080`.

## Endpoints REST

Todos los endpoints están bajo `/api/guias`.

### Crear guía

- `POST /api/guias`
- Cuerpo JSON:
  ```json
  {
    "numeroPedido": "PED-123",
    "transportista": "Transporte Chile",
    "fecha": "2026-07-19",
    "destinatario": "Cliente Ejemplo",
    "direccionEntrega": "Av. Siempre Viva 742",
    "detallePedido": "Detalle del pedido"
  }
  ```
- Respuesta: guía con `id` y estado inicial `EN_COLA`.
- La creación es asíncrona: el mensaje se procesa luego por el consumidor RabbitMQ.

### Consultar guías

- `GET /api/guias`
- Query params opcionales:
  - `transportista`
  - `fecha` (formato `yyyy-MM-dd`)

### Obtener guía por id

- `GET /api/guias/{id}`

### Descargar PDF

- `GET /api/guias/{id}/descargar`
- Retorna el PDF generado desde S3.

### Actualizar guía

- `PUT /api/guias/{id}`
- Igual estructura que el POST.
- Publica un mensaje `ACTUALIZAR` en RabbitMQ.

### Eliminar guía

- `DELETE /api/guias/{id}`
- Publica un mensaje `ELIMINAR` en RabbitMQ.
- Elimina el PDF de S3.

## Seguridad

La aplicación está protegida por JWT con Azure AD B2C.

Roles esperados:
- `ROLE_GESTION_GUIAS`: acceso a creación, consulta, actualización y eliminación.
- `ROLE_DESCARGA_GUIAS`: acceso a descarga de PDF.

El claim de roles se lee desde `extension_rolTransporte` sin prefijo.

## RabbitMQ

Configuración en `RabbitMQConfig`:
- Exchange principal: `guias-exchange`
- Cola principal: `guias-queue`
- DLQ: `guias-dlq`
- Routing key principal: `guias.tickets`
- Routing key DLQ: `guias.error`

Flujo DLQ:
- Si `GuiaMessageConsumer` lanza cualquier excepción durante el procesamiento, el mensaje no se reencola.
- RabbitMQ lo redirige a `guias-dlq` mediante `guias-dlx`.

## AWS S3

Estructura de objetos en el bucket:

- `guias/{id}/metadata.json`
- `guias/{id}/guia.pdf`

`S3StorageService` administra:
- carga de metadata JSON.
- carga de PDF.
- lectura de PDF.
- eliminación de objetos.
- listado de metadata cuando se requieren guías.

## Oracle Cloud

El repositorio JPA persiste las guías procesadas en la tabla `GUIAS_PROCESADAS`.

Columnas esperadas (según `GuiaProcesadaEntity`):
- `ID`
- `NUMERO_PEDIDO`
- `TRANSPORTISTA`
- `FECHA`
- `DESTINATARIO`
- `DIRECCION_ENTREGA`
- `DETALLE_PEDIDO`
- `ESTADO`
- `RUTA_S3`
- `FECHA_PROCESAMIENTO`
- `MENSAJE_ERROR`

> Nota: el archivo `src/main/resources/schema.sql` contiene un script de tabla alternativa `guias_despacho`. La entidad JPA actual usa `GUIAS_PROCESADAS`, por lo que el esquema de Oracle debe ajustarse a esa tabla.

## Observaciones importantes

- La API de creación/actualización es asíncrona. El POST/PUT retorna el identificador antes de que el PDF esté disponible.
- La descarga del PDF comprueba la existencia de la guía en Oracle antes de leer S3.
- El consumidor realiza `deleteById` para `ELIMINAR`, mientras que `S3StorageService.eliminarGuia` borra los archivos del bucket.
- `spring.jpa.hibernate.ddl-auto` está deshabilitado: el esquema debe existir previamente.

## Potenciales mejoras

- Implementar retry/backoff en el consumidor RabbitMQ.
- Añadir un endpoint de estado de procesamiento.
- Registrar mensajes de error en la tabla Oracle y mejorar valores de `mensajeError`.
- Añadir pruebas unitarias e integración.

---

## Estructura de carpetas clave

- `src/main/java/com/transportes/guiadespacho/controller`
- `src/main/java/com/transportes/guiadespacho/service`
- `src/main/java/com/transportes/guiadespacho/messaging`
- `src/main/java/com/transportes/guiadespacho/config`
- `src/main/java/com/transportes/guiadespacho/persistence`
- `src/main/java/com/transportes/guiadespacho/dto`
- `src/main/java/com/transportes/guiadespacho/model`
