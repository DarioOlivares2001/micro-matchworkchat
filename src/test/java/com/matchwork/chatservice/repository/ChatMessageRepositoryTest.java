package com.matchwork.chatservice.repository;

import com.matchwork.chatservice.model.ChatMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

@DataMongoTest
public class ChatMessageRepositoryTest {

    @Autowired
    private ChatMessageRepository repository;

    @BeforeEach
    void limpiarChat() {
        repository.deleteAll(); // Borra TODOS los mensajes antes de cada test
    }

    @Test
    void findConversationBetweenUsers_deberiaEncontrarMensajes() {
        var msg1 = new ChatMessage(1L, 2L, "hola", ChatMessage.MessageType.CHAT, false);
        var msg2 = new ChatMessage(2L, 1L, "qué tal", ChatMessage.MessageType.CHAT, false);
        repository.saveAll(List.of(msg1, msg2));

        var mensajes = repository.findConversationBetweenUsers(1L, 2L);
        assertThat(mensajes).hasSize(2);
    }

    @Test
    void findBySenderId_deberiaRetornarMensajesEnviados() {
        var msg1 = new ChatMessage(10L, 20L, "hola 10->20", ChatMessage.MessageType.CHAT, false);
        var msg2 = new ChatMessage(10L, 30L, "hola 10->30", ChatMessage.MessageType.JOIN, false);
        repository.saveAll(List.of(msg1, msg2));
        var encontrados = repository.findBySenderId(10L);
        assertThat(encontrados).hasSize(2);
        assertThat(encontrados.get(0).getSenderId()).isEqualTo(10L);
    }

    @Test
    void findByReceiverId_deberiaRetornarMensajesRecibidos() {
        var msg1 = new ChatMessage(99L, 22L, "hey", ChatMessage.MessageType.CHAT, false);
        repository.save(msg1);
        var recibidos = repository.findByReceiverId(22L);
        assertThat(recibidos).hasSize(1);
        assertThat(recibidos.get(0).getContent()).isEqualTo("hey");
    }

    @Test
    void findByType_deberiaFiltrarPorTipo() {
        var chat = new ChatMessage(1L, 2L, "es un chat", ChatMessage.MessageType.CHAT, false);
        var join = new ChatMessage(2L, 1L, "es un join", ChatMessage.MessageType.JOIN, false);
        repository.saveAll(List.of(chat, join));
        var chats = repository.findByType(ChatMessage.MessageType.CHAT);
        assertThat(chats).extracting(ChatMessage::getType).containsOnly(ChatMessage.MessageType.CHAT);
    }

    @Test
    void countByReceiverIdAndSeenFalse_deberiaContarNoLeidos() {
        repository.saveAll(List.of(
                new ChatMessage(1L, 42L, "msg1", ChatMessage.MessageType.CHAT, false),
                new ChatMessage(2L, 42L, "msg2", ChatMessage.MessageType.CHAT, false),
                new ChatMessage(3L, 42L, "msg3", ChatMessage.MessageType.CHAT, true)));
        long count = repository.countByReceiverIdAndSeenFalse(42L);
        assertThat(count).isEqualTo(2);
    }

    // Este es un poco más "integración", y a veces falla por tema de compatibilidad
    // del driver
    @Test
    void findDistinctConversationPartners_devuelveListaUnica() {
        repository.saveAll(List.of(
                new ChatMessage(11L, 12L, "a", ChatMessage.MessageType.CHAT, false),
                new ChatMessage(11L, 13L, "b", ChatMessage.MessageType.CHAT, false),
                new ChatMessage(14L, 11L, "c", ChatMessage.MessageType.CHAT, false)));
        var lista = repository.findDistinctConversationPartners(11L);
        // Debería contener [12L, 13L, 14L] (puede variar el orden)
        assertThat(lista).containsExactlyInAnyOrder(12L, 13L, 14L);
    }

}
