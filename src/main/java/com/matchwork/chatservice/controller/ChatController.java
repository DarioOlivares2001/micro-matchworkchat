package com.matchwork.chatservice.controller;

import com.matchwork.chatservice.model.ChatMessage;
import com.matchwork.chatservice.repository.ChatMessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatMessageRepository repository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public ChatController(ChatMessageRepository repository,
                         SimpMessagingTemplate messagingTemplate) {
        this.repository = repository;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /*@MessageMapping("/chat.sendPrivateMessage")
    public void sendPrivateMessage(@Payload ChatMessage message) {
        try {
            System.out.println("=== MENSAJE RECIBIDO ===");
            System.out.println("SenderId: " + message.getSenderId());
            System.out.println("ReceiverId: " + message.getReceiverId());
            System.out.println("Content: " + message.getContent());
            System.out.println("Type: " + message.getType());
            
            // Validación de campos requeridos
            if (message.getSenderId() == null || message.getReceiverId() == null || 
                message.getContent() == null || message.getContent().trim().isEmpty()) {
                System.err.println("ERROR: Campos requeridos faltantes");
                return;
            }
            
            // Establecer valores por defecto
            if (message.getTimestamp() == null) {
                message.setTimestamp(Instant.now());
            }
            if (message.getType() == null) {
                message.setType(ChatMessage.MessageType.CHAT);
            }
            
            // Guardar en MongoDB
            ChatMessage saved = repository.save(message);
            System.out.println("✅ Mensaje guardado en BD con ID: " + saved.getId());
            
            // Convertir a JSON para el envío
            String messageJson = objectMapper.writeValueAsString(saved);
            
            // Enviar al receptor
            messagingTemplate.convertAndSendToUser(
                message.getReceiverId().toString(),
                "/queue/messages",
                messageJson
            );
            System.out.println("✅ Mensaje enviado al receptor: " + message.getReceiverId());
            
            // Enviar confirmación al emisor
            messagingTemplate.convertAndSendToUser(
                message.getSenderId().toString(),
                "/queue/messages",
                messageJson
            );
            System.out.println("✅ Mensaje enviado al emisor: " + message.getSenderId());
            
        } catch (Exception e) {
            System.err.println("❌ ERROR al procesar mensaje: " + e.getMessage());
            e.printStackTrace();
        }
    }*/

    @MessageMapping("/chat.sendPrivateMessage")
    public void sendPrivateMessage(@Payload ChatMessage message) {
        // 1) Guarda en BD
        ChatMessage saved = repository.save(message);

        // 2) Construye tus tópicos “a mano”
        String receiverTopic = "/topic/private." + saved.getReceiverId();
        String senderTopic   = "/topic/private." + saved.getSenderId();

        // 3) Emite a cada uno
        messagingTemplate.convertAndSend(receiverTopic, saved);
        // (opcional) eco al emisor
        messagingTemplate.convertAndSend(senderTopic, saved);
    }

    @MessageMapping("/chat.sendMessage")
    public void sendPublicMessage(@Payload ChatMessage message) {
        try {
            System.out.println("=== MENSAJE PÚBLICO RECIBIDO ===");
            
            // Establecer valores por defecto
            if (message.getTimestamp() == null) {
                message.setTimestamp(Instant.now());
            }
            if (message.getType() == null) {
                message.setType(ChatMessage.MessageType.CHAT);
            }
            
            // Guardar en MongoDB
            ChatMessage saved = repository.save(message);
            System.out.println("✅ Mensaje público guardado: " + saved.getId());
            
            // Convertir a JSON para el envío
            String messageJson = objectMapper.writeValueAsString(saved);
            
            // Enviar a todos los suscritos
            messagingTemplate.convertAndSend("/topic/public", messageJson);
            
        } catch (Exception e) {
            System.err.println("❌ Error al enviar mensaje público: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @GetMapping("/api/messages/{senderId}/{receiverId}")
    public List<ChatMessage> getMessages(@PathVariable Long senderId,
                                       @PathVariable Long receiverId) {
        try {
            List<ChatMessage> messages = repository.findConversationBetweenUsers(senderId, receiverId);
            System.out.println("📚 Mensajes encontrados: " + messages.size());
            return messages;
        } catch (Exception e) {
            System.err.println("❌ Error al obtener mensajes: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }
    
    @GetMapping("/api/test")
    public String test() {
        try {
            long count = repository.count();
            return "✅ Conectado a MongoDB. Total de mensajes: " + count;
        } catch (Exception e) {
            return "❌ Error de conexión: " + e.getMessage();
        }
    }
}