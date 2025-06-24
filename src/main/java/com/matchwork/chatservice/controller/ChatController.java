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
            
            
            if (message.getTimestamp() == null) {
                message.setTimestamp(Instant.now());
            }
            
           
            if (message.getType() != MessageType.VIDEO_CALL) {
                repository.save(message);
            }
            
           
            String receiverTopic = "/topic/private." + message.getReceiverId();
            String senderTopic = "/topic/private." + message.getSenderId();
            
           
            messagingTemplate.convertAndSend(receiverTopic, message);
            messagingTemplate.convertAndSend(senderTopic, message);
            
        } catch (Exception e) {
            System.err.println("ERROR al procesar mensaje: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @MessageMapping("/chat.sendMessage")
    public void sendPublicMessage(@Payload ChatMessage message) {
        try {
            System.out.println("=== MENSAJE PÚBLICO RECIBIDO ===");
            
            
            if (message.getTimestamp() == null) {
                message.setTimestamp(Instant.now());
            }
            if (message.getType() == null) {
                message.setType(ChatMessage.MessageType.CHAT);
            }
            
    
            ChatMessage saved = repository.save(message);
            System.out.println("Mensaje público guardado: " + saved.getId());
            
        
            String messageJson = objectMapper.writeValueAsString(saved);
            
       
            messagingTemplate.convertAndSend("/topic/public", messageJson);
            
        } catch (Exception e) {
            System.err.println("Error al enviar mensaje público: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @GetMapping("/api/messages/{senderId}/{receiverId}")
    public List<ChatMessage> getMessages(@PathVariable Long senderId,
                                       @PathVariable Long receiverId) {
        try {
            List<ChatMessage> messages = repository.findConversationBetweenUsers(senderId, receiverId);
            System.out.println("Mensajes encontrados: " + messages.size());
            return messages;
        } catch (Exception e) {
            System.err.println("Error al obtener mensajes: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }
    
    @GetMapping("/api/test")
    public String test() {
        try {
            long count = repository.count();
            return "Conectado a MongoDB. Total de mensajes: " + count;
        } catch (Exception e) {
            return "Error de conexión: " + e.getMessage();
        }
    }



   
    @GetMapping("/messages/unread/count/{userId}")
    public Map<String, Long> getUnreadCount(@PathVariable Long userId) {
        long total = repository.countByReceiverIdAndSeenFalse(userId);
        return Map.of("total", total);
    }

 
    @GetMapping("/messages/unread/by-sender/{userId}")
    public List<SenderUnreadCount> getUnreadBySender(@PathVariable Long userId) {
        return repository.countUnreadBySender(userId);
    }

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


    @MessageMapping("/chat.readReceipt")
    public void onReadReceipt(@Payload ReadReceipt receipt) {
  
        repository.markAsSeen(receipt.getSenderId(), receipt.getReceiverId());

        messagingTemplate.convertAndSendToUser(
        receipt.getSenderId().toString(),
        "/queue/read.receipt",
        receipt
        );
    }



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