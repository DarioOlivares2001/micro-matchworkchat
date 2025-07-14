# Chat Service – MatchWork

Este microservicio maneja la funcionalidad de mensajería en tiempo real entre profesionales y empresas dentro de la plataforma MatchWork. Está basado en WebSocket utilizando STOMP y SockJS, y almacena los mensajes en MongoDB Atlas.

## Tecnologías
- Spring Boot 3
- WebSocket (STOMP + SockJS)
- MongoDB Atlas
- Docker
- EC2 – AWS

## Requisitos
- Java 17 o superior
- Maven 3.8+
- MongoDB Atlas (cluster activo)
- Docker

## Ejecución local

```bash
# Clonar repositorio
git clone https://github.com/DarioOlivares2001/micro-matchworkchat.git
cd micro-matchworkchat

# Compilar y ejecutar
./mvnw spring-boot:run

Configuracion MongoDB

spring.data.mongodb.uri=mongodb+srv://<usuario>:<password>@<cluster>.mongodb.net/chatdb
spring.application.name=chatservice
server.port=8082
spring.websocket.allowed-origins=*

Endpoints WebSocket

Conexión WebSocket: /ws

Enviar mensaje: /app/chat

Suscripción a canal: /topic/mensajes/{sala}


Estructura de mensaje

{
  "remitente": "usuario@email.com",
  "destinatario": "empresa@email.com",
  "emisorId" : "user22f",
  "receptorId" : "user485"
  "contenido": "Hola, estoy interesado en la oferta.",
  "timestamp": "2025-07-11T15:30:00Z"
}


Despliegue en Producción

- AWS EC2 – IP pública: 34.193.228.160

- Puerto: 8082

- Integrado con frontend desplegado en Vercel

- Accedido mediante API Gateway


Seguridad

- Sin autenticación directa en este servicio (token validado desde el Gateway)

- CORS habilitado para origen: https://matchwork-wo14.vercel.app

- Control de usuarios y chats encriptados almacenados en MongoDB Atlas
