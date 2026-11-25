# Microservicio de catálogo CampusLab

Administra recursos académicos y procesa los movimientos de stock asociados a las reservas.

## Tecnologías

- Java 21.
- Spring Boot.
- Spring Web MVC.
- Spring Security OAuth2 Resource Server.
- Spring Data JPA y MySQL.
- Spring AMQP y RabbitMQ.
- Maven y Docker.

## Funcionalidades

- Consultar recursos.
- Administrar el catálogo según el rol autorizado.
- Validar stock y disponibilidad.
- Procesar de forma asíncrona los movimientos de stock.
- Publicar el resultado del movimiento para que Bookings actualice la reserva.

## API de recursos

La ruta base de recursos es:

```text
/api/catalog/resources
```

Las operaciones de escritura requieren autorización. La autorización del BFF complementa las validaciones del servicio.

## Procesamiento de stock

Los movimientos se coordinan mediante RabbitMQ:

- `RESERVE`: compromete unidades disponibles.
- `RELEASE`: libera una asignación de stock según sus reglas.
- `FINISH`: finaliza una asignación.

El servicio diferencia el stock total de las unidades disponibles. Al finalizar una reserva, los recursos reutilizables pueden volver a estar disponibles; los insumos consumidos se descuentan según las reglas del catálogo.

Las reglas de negocio impiden disponibilidad negativa y movimientos incompatibles.

## Concurrencia e idempotencia

El recurso se bloquea durante la transacción para controlar movimientos simultáneos. Las asignaciones se identifican por reserva y los resultados por operación. El procesamiento repetido de un mismo mensaje no debe descontar stock nuevamente.

El cambio de stock y el resultado pendiente de publicación se guardan dentro de la misma transacción.

## RabbitMQ

- Exchange principal: `cmd.direct`.
- Cola de entrada para movimientos: `q.cmd.stock`.
- Cola de salida para resultados: `q.result.stock`.
- Exchanges, colas y bindings definidos en la configuración de mensajería.
- ACK manual después de completar el procesamiento.
- Reintentos acotados para errores recuperables y DLQ para mensajes que no pueden procesarse.

Catalog utiliza RabbitMQ para el flujo de stock. Los eventos Kafka de reservas y auditoría son responsabilidad de Bookings y Notifier.

## Configuración

Variables principales:

```text
DB_HOST
DB_PORT
DB_NAME
DB_USERNAME
DB_PASSWORD
RABBITMQ_ADDRESSES
RABBITMQ_USER
RABBITMQ_PASSWORD
RABBITMQ_VHOST
```

Base de datos: `campuslab_catalog`  
Puerto interno: `8081`

Las credenciales y direcciones del entorno se configuran fuera del repositorio.

## Compilación

En PowerShell, desde la carpeta del repositorio:

```powershell
.\mvnw.cmd clean package -DskipTests
```

Este comando compila y empaqueta el servicio, pero omite la ejecución de pruebas.

## Despliegue

El servicio se construye y ejecuta mediante Docker Compose en el repositorio `infra-campuslab`.
