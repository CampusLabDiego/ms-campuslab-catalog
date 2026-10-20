# Microservicio de catálogo CampusLab

Administra laboratorios, equipos e insumos y procesa los movimientos
de stock asociados a reservas.

## Tecnologías

- Java 21 y Spring Boot.
- Spring Web MVC.
- Spring Security OAuth2 Resource Server.
- Spring Data JPA y MySQL.
- Spring AMQP.
- Maven y Docker.

## Funcionalidades

- Consulta de recursos.
- Administración del catálogo para Admin.
- Validación de stock y disponibilidad.
- Procesamiento asíncrono de movimientos de stock.
- Publicación del resultado hacia bookings.

Ruta base de recursos:

```text
/api/catalog/resources
```

## Stock

Se diferencia entre:

- Stock total: unidades registradas.
- Stock disponible: unidades que pueden comprometerse.

Los movimientos utilizan:

- RESERVE: compromete unidades disponibles.
- RELEASE: libera una asignación según sus reglas.
- FINISH: finaliza la asignación.

Al finalizar:

- Los recursos reutilizables recuperan disponibilidad.
- Los insumos consumidos reducen el stock total según la regla aplicada.

Las reglas impiden disponibilidad negativa y movimientos incompatibles.

## Concurrencia e idempotencia

El procesamiento bloquea el recurso durante la transacción.

Las asignaciones se identifican por reserva y los resultados por
operación. Una entrega repetida no debe descontar stock nuevamente.

El cambio de stock y el resultado pendiente de publicación se
guardan dentro de una misma transacción.

## RabbitMQ

- Entrada: q.cmd.stock.
- Salida: q.result.stock.
- Exchange principal: cmd.direct.
- DLQ y listeners para registrar mensajes rechazados.
- ACK manual después del procesamiento confirmado.
- Reintentos acotados para errores recuperables del consumidor.

Los nombres y bindings están centralizados en las clases de configuración.

## Seguridad

El servicio valida los tokens recibidos.
La autorización por rol también se aplica en el BFF.

Los movimientos internos de stock se procesan mediante RabbitMQ.

## Configuración

Variables principales:

- DB_HOST, DB_PORT, DB_NAME, DB_USERNAME, DB_PASSWORD.
- RABBITMQ_ADDRESSES, RABBITMQ_USER, RABBITMQ_PASSWORD, RABBITMQ_VHOST.

Base de datos: campuslab_catalog.
Puerto del servicio: 8081.

## Compilación

```powershell
.\mvnw.cmd clean package -DskipTests
```

Este comando compila y empaqueta; omite la ejecución de pruebas.

El despliegue está definido en infra-campuslab.