package com.matchwork.chatservice.repository;

import com.matchwork.chatservice.model.ChatMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
public class ChatMessageRepositoryTest { 

    @Autowired
    private ChatMessageRepository repository;

    @Test
    void findConversationBetweenUsers_deberiaEncontrarMensajes() {
        var msg1 = new ChatMessage(1L, 2L, "hola", ChatMessage.MessageType.CHAT, false);
        var msg2 = new ChatMessage(2L, 1L, "qué tal", ChatMessage.MessageType.CHAT, false);
        repository.saveAll(List.of(msg1, msg2));

        var mensajes = repository.findConversationBetweenUsers(1L, 2L);
        assertThat(mensajes).hasSize(2);
    }
}
