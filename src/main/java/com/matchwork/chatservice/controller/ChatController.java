package com.matchwork.chatservice.controller;

import com.matchwork.chatservice.dto.SenderUnreadCount;
import com.matchwork.chatservice.model.ChatMessage;
import com.matchwork.chatservice.model.ChatMessage.MessageType;
import com.matchwork.chatservice.repository.ChatMessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
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


   @MessageMapping("/chat.sendPrivateMessage")
    public void sendPrivateMessage(@Payload ChatMessage message) {
        try {
            System.out.println("=== MENSAJE RECIBIDO ===");
            System.out.println("Tipo: " + message.getType());
            System.out.println("De: " + message.getSenderId() + " Para: " + message.getReceiverId());
            System.out.println("Contenido: " + message.getContent());
            
            // Establecer timestamp si no viene
            if (message.getTimestamp() == null) {
                message.setTimestamp(Instant.now());
            }
            
            // Guardar en BD (excepto para VIDEO_CALL si no quieres persistirlos)
            if (message.getType() != MessageType.VIDEO_CALL) {
                repository.save(message);
            }
            
            // Construir tópicos
            String receiverTopic = "/topic/private." + message.getReceiverId();
            String senderTopic = "/topic/private." + message.getSenderId();
            
            // Enviar mensaje
            messagingTemplate.convertAndSend(receiverTopic, message);
            messagingTemplate.convertAndSend(senderTopic, message);
            
        } catch (Exception e) {
            System.err.println("❌ ERROR al procesar mensaje: " + e.getMessage());
            e.printStackTrace();
        }
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



        /** 1) Contar mensajes no vistos totales */
    @GetMapping("/messages/unread/count/{userId}")
    public Map<String, Long> getUnreadCount(@PathVariable Long userId) {
        long total = repository.countByReceiverIdAndSeenFalse(userId);
        return Map.of("total", total);
    }

    /** 2) Contar no vistos por emisor (para sidebar) */
    @GetMapping("/messages/unread/by-sender/{userId}")
    public List<SenderUnreadCount> getUnreadBySender(@PathVariable Long userId) {
        return repository.countUnreadBySender(userId);
    }

    /** 3) Marcar como vistos todos de sender → receiver */
   @PutMapping("/messages/{senderId}/{receiverId}/seen")
    public ResponseEntity<Void> markAsSeen(
        @PathVariable Long senderId,
        @PathVariable Long receiverId) {

        repository.markAsSeen(senderId, receiverId);


        Map<String, Long> notification = Map.of(
                "senderId", senderId,
                "receiverId", receiverId,
                "readerId", receiverId
        );


        messagingTemplate.convertAndSend("/topic/read.receipt." + senderId, notification);
        messagingTemplate.convertAndSend("/topic/read.receipt." + receiverId, notification);
        return ResponseEntity.noContent().build();
    }

    /** 4) WebSocket para read‐receipt (opcional) */
    @MessageMapping("/chat.readReceipt")
    public void onReadReceipt(@Payload ReadReceipt receipt) {
        // receipt tiene senderId, receiverId
        repository.markAsSeen(receipt.getSenderId(), receipt.getReceiverId());
        // reenviar confirmación
        messagingTemplate.convertAndSendToUser(
        receipt.getSenderId().toString(),
        "/queue/read.receipt",
        receipt
        );
    }


     /** 5) Devuelve todos los interlocutores de userId */
    @GetMapping("/api/messages/conversations/{userId}")
    public List<Long> getConversationPartners(@PathVariable Long userId) {
        return repository.findDistinctConversationPartners(userId);
    }


    public static class ReadReceipt {
        private Long senderId;
        private Long receiverId;

        public Long getSenderId() {
            return senderId;
        }
        public void setSenderId(Long senderId) {
            this.senderId = senderId;
        }
        public Long getReceiverId() {
            return receiverId;
        }
        public void setReceiverId(Long receiverId) {
            this.receiverId = receiverId;
        }

        
    }




}