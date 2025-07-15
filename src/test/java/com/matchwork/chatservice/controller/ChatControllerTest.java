package com.matchwork.chatservice.controller;

import com.matchwork.chatservice.dto.SenderUnreadCount;
import com.matchwork.chatservice.model.ChatMessage;
import com.matchwork.chatservice.repository.ChatMessageRepository;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private ChatMessageRepository repo;

    @Mock
    private org.springframework.messaging.simp.SimpMessagingTemplate template;

    @InjectMocks
    private ChatController controller;

    @Test
    void sendPrivateMessage_guardaYenvia() {
        ChatMessage msg = new ChatMessage();
        msg.setSenderId(1L);
        msg.setReceiverId(2L);
        msg.setContent("hola");
        msg.setType(ChatMessage.MessageType.CHAT);

        when(repo.save(any(ChatMessage.class))).thenReturn(msg);

        controller.sendPrivateMessage(msg);

        verify(repo).save(msg);
        verify(template).convertAndSend(eq("/topic/private.2"), any(ChatMessage.class));
        verify(template).convertAndSend(eq("/topic/private.1"), any(ChatMessage.class));
    }

    @Test
    void sendPublicMessage_guardaYenvia() throws Exception {
        ChatMessage msg = new ChatMessage();
        msg.setSenderId(1L);
        msg.setContent("¡Hola público!");
        msg.setType(ChatMessage.MessageType.CHAT);

        when(repo.save(any())).thenReturn(msg);

        controller.sendPublicMessage(msg);

        verify(repo).save(any(ChatMessage.class));
        verify(template).convertAndSend(eq("/topic/public"), any(String.class));
    }

    @Test
    void getConversationPartners_retornaLista() {
        Long userId = 1L;
        List<Long> partners = List.of(2L, 3L, 5L);

        when(repo.findDistinctConversationPartners(userId)).thenReturn(partners);

        List<Long> result = controller.getConversationPartners(userId);

        assertEquals(partners, result);
        verify(repo).findDistinctConversationPartners(userId);
    }

    @Test
    void getUnreadBySender_retornaLista() {
        Long userId = 2L;
        List<SenderUnreadCount> lista = List.of(
                new SenderUnreadCount() {
                    public Long get_id() {
                        return 1L;
                    }

                    public Integer getCount() {
                        return 2;
                    }
                });
        when(repo.countUnreadBySender(userId)).thenReturn(lista);

        List<SenderUnreadCount> res = controller.getUnreadBySender(userId);

        assertEquals(1, res.size());
        assertEquals(1L, res.get(0).get_id());
        assertEquals(2, res.get(0).getCount());
    }

    @Test
    void markAsSeen_llamaRepoYnotifica() {
        Long senderId = 1L, receiverId = 2L;

        ResponseEntity<Void> resp = controller.markAsSeen(senderId, receiverId);

        assertEquals(HttpStatus.NO_CONTENT, resp.getStatusCode());

        verify(repo).markAsSeen(senderId, receiverId);

        // Verifica que se envíen los dos mensajes correctos
        verify(template).convertAndSend(eq("/topic/read.receipt.1"), any(Map.class));
        verify(template).convertAndSend(eq("/topic/read.receipt.2"), any(Map.class));
    }

    @Test
    void test_apiTest() {
        when(repo.count()).thenReturn(5L);
        String res = controller.test();
        assertTrue(res.contains("Total de mensajes: 5"));
    }

    @Test
    void markAsSeen_cambiaEstadoVisto() {
        Long senderId = 1L, receiverId = 2L;

        // Mocks
        doNothing().when(repo).markAsSeen(senderId, receiverId);

        ResponseEntity<Void> resp = controller.markAsSeen(senderId, receiverId);

        assertEquals(HttpStatus.NO_CONTENT, resp.getStatusCode());
        verify(repo).markAsSeen(senderId, receiverId);
        verify(template).convertAndSend(eq("/topic/read.receipt.1"), any(Map.class));
        verify(template).convertAndSend(eq("/topic/read.receipt.2"), any(Map.class));
    }

    // ...

    // @Test
    // void getUnreadCount_devuelveCantidadCorrecta() throws Exception {
    //     when(repo.countByReceiverIdAndSeenFalse(4L)).thenReturn(7L);

    //     mockMvc.perform(get("/api/messages/unread/count/4"))
    //             .andExpect(status().isOk())
    //             .andExpect(jsonPath("$.total").value(7));
    // }

    @Test
    void getUnreadBySender_retornaListaCorrecta() {
        Long userId = 2L;
        List<SenderUnreadCount> lista = List.of(
                new SenderUnreadCount() {
                    public Long get_id() {
                        return 10L;
                    }

                    public Integer getCount() {
                        return 5;
                    }
                },
                new SenderUnreadCount() {
                    public Long get_id() {
                        return 99L;
                    }

                    public Integer getCount() {
                        return 1;
                    }
                });
        when(repo.countUnreadBySender(userId)).thenReturn(lista);

        List<SenderUnreadCount> res = controller.getUnreadBySender(userId);

        assertEquals(2, res.size());
        assertEquals(10L, res.get(0).get_id());
        assertEquals(5, res.get(0).getCount());
        assertEquals(99L, res.get(1).get_id());
        assertEquals(1, res.get(1).getCount());
    }

    @Test
    void sendPrivateMessage_enviaYguarda() {
        ChatMessage msg = new ChatMessage(1L, 2L, "privado!", ChatMessage.MessageType.CHAT, false);

        when(repo.save(any())).thenReturn(msg);

        controller.sendPrivateMessage(msg);

        verify(repo).save(msg);
        verify(template).convertAndSend(eq("/topic/private.2"), eq(msg));
        verify(template).convertAndSend(eq("/topic/private.1"), eq(msg));
    }

    @Test
    void sendPublicMessage_enviaPublico() throws Exception {
        ChatMessage msg = new ChatMessage(1L, null, "¡Hola todos!", ChatMessage.MessageType.CHAT, false);

        when(repo.save(any())).thenReturn(msg);

        controller.sendPublicMessage(msg);

        verify(repo).save(any());
        verify(template).convertAndSend(eq("/topic/public"), any(String.class));
    }

    @Test
    void getMessages_recuperaMensajesCorrectos() {
        ChatMessage m1 = new ChatMessage(1L, 2L, "hola", ChatMessage.MessageType.CHAT, false);
        ChatMessage m2 = new ChatMessage(2L, 1L, "holi", ChatMessage.MessageType.CHAT, false);

        when(repo.findConversationBetweenUsers(1L, 2L)).thenReturn(List.of(m1, m2));

        List<ChatMessage> res = controller.getMessages(1L, 2L);

        assertEquals(2, res.size());
        assertEquals("hola", res.get(0).getContent());
        assertEquals("holi", res.get(1).getContent());
    }

}
