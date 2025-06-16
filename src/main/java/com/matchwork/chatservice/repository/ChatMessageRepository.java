package com.matchwork.chatservice.repository;

import com.matchwork.chatservice.dto.SenderUnreadCount;
import com.matchwork.chatservice.model.ChatMessage;

import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
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

    // todos los no vistos para un receptor
  long countByReceiverIdAndSeenFalse(Long receiverId);

  // opcional: desglosado por cada sender
  @Aggregation(pipeline = {
    "{ $match: { receiverId: ?0, seen: false } }",
    "{ $group: { _id: \"$senderId\", count: { $sum: 1 } } }"
  })
  List<SenderUnreadCount> countUnreadBySender(Long receiverId);

  // marcar como visto
  @Query("{ 'senderId': ?0, 'receiverId': ?1, 'seen': false }")
  @Update("{ $set: { 'seen': true } }")
  void markAsSeen(Long senderId, Long receiverId);


  @Query(value = "{ $or: [ { 'senderId': ?0 }, { 'receiverId': ?0 } ] }",
         fields = "{ 'senderId': 1, 'receiverId': 1 }")
  List<ChatMessage> findAllByUser(@Param("0") Long userId);

  /**  
   * Transforma esa lista en un Set de interlocutores distintos.  
   * Lo puedes implementar en tu servicio o directamente aquí:  
   */
  default List<Long> findDistinctConversationPartners(Long userId) {
    return findAllByUser(userId).stream()
      .map(m -> m.getSenderId().equals(userId)
                 ? m.getReceiverId()
                 : m.getSenderId())
      .distinct()
      .toList();
  }


}

