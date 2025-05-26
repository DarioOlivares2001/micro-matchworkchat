package com.matchwork.chatservice.repository;

import com.matchwork.chatservice.model.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    
    /**
     * Encuentra mensajes entre dos usuarios específicos (solo en una dirección)
     */
    List<ChatMessage> findBySenderIdAndReceiverId(Long senderId, Long receiverId);
    
    /**
     * Encuentra toda la conversación entre dos usuarios (en ambas direcciones)
     * Ordena por timestamp ascendente para mostrar cronológicamente
     */
    @Query("{ $or: [ " +
           "{ 'senderId': ?0, 'receiverId': ?1 }, " +
           "{ 'senderId': ?1, 'receiverId': ?0 } " +
           "] }")
    List<ChatMessage> findConversationBetweenUsers(Long userId1, Long userId2);
    
    /**
     * Encuentra mensajes por senderId
     */
    List<ChatMessage> findBySenderId(Long senderId);
    
    /**
     * Encuentra mensajes por receiverId
     */
    List<ChatMessage> findByReceiverId(Long receiverId);
    
    /**
     * Encuentra mensajes por tipo
     */
    List<ChatMessage> findByType(ChatMessage.MessageType type);
}