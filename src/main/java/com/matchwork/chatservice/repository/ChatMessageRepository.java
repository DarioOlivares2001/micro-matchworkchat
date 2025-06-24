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
    
   
    List<ChatMessage> findBySenderIdAndReceiverId(Long senderId, Long receiverId);
    
   
    @Query("{ $or: [ " +
           "{ 'senderId': ?0, 'receiverId': ?1 }, " +
           "{ 'senderId': ?1, 'receiverId': ?0 } " +
           "] }")
    List<ChatMessage> findConversationBetweenUsers(Long userId1, Long userId2);
    
   
    List<ChatMessage> findBySenderId(Long senderId);
    
   
    List<ChatMessage> findByReceiverId(Long receiverId);
    
    
    List<ChatMessage> findByType(ChatMessage.MessageType type);

    
  long countByReceiverIdAndSeenFalse(Long receiverId);


  @Aggregation(pipeline = {
    "{ $match: { receiverId: ?0, seen: false } }",
    "{ $group: { _id: \"$senderId\", count: { $sum: 1 } } }"
  })
  List<SenderUnreadCount> countUnreadBySender(Long receiverId);


  @Query("{ 'senderId': ?0, 'receiverId': ?1, 'seen': false }")
  @Update("{ $set: { 'seen': true } }")
  void markAsSeen(Long senderId, Long receiverId);


  @Query(value = "{ $or: [ { 'senderId': ?0 }, { 'receiverId': ?0 } ] }",
         fields = "{ 'senderId': 1, 'receiverId': 1 }")
  List<ChatMessage> findAllByUser(@Param("0") Long userId);

  default List<Long> findDistinctConversationPartners(Long userId) {
    return findAllByUser(userId).stream()
      .map(m -> m.getSenderId().equals(userId)
                 ? m.getReceiverId()
                 : m.getSenderId())
      .distinct()
      .toList();
  }


}

