package com.matchwork.chatservice.controller;

import com.matchwork.chatservice.dto.SenderUnreadCount;
import com.matchwork.chatservice.model.ChatMessage;
import com.matchwork.chatservice.repository.ChatMessageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatController.class)
public class ChatControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatMessageRepository repository;

    @MockBean
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @Test
    void getMessages_devuelveMensajes() throws Exception {
        var msg = new ChatMessage(1L, 2L, "hola!", ChatMessage.MessageType.CHAT, false);
        when(repository.findConversationBetweenUsers(1L, 2L)).thenReturn(List.of(msg));

        mockMvc.perform(get("/api/messages/1/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("hola!"));
    }

    @Test
    void getUnreadCount_retornaNumero() throws Exception {
        when(repository.countByReceiverIdAndSeenFalse(5L)).thenReturn(3L);

        mockMvc.perform(get("/api/messages/unread/count/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3));
    }

    @Test
    void getUnreadBySender_devuelveLista() throws Exception {
        // Mock lista de resultados (1 mensaje no leído del user 8)
        List<SenderUnreadCount> unreadList = List.of(
                new com.matchwork.chatservice.dto.SenderUnreadCount() {
                    public Long get_id() {
                        return 8L;
                    }

                    public Integer getCount() {
                        return 1;
                    }
                });
        when(repository.countUnreadBySender(7L)).thenReturn(unreadList);

        mockMvc.perform(get("/api/messages/unread/by-sender/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]._id").value(8))
                .andExpect(jsonPath("$[0].count").value(1));
    }

    @Test
    void getConversationPartners_devuelveLista() throws Exception {
        when(repository.findDistinctConversationPartners(4L)).thenReturn(List.of(9L, 10L));

        mockMvc.perform(get("/api/messages/conversations/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(9))
                .andExpect(jsonPath("$[1]").value(10));
    }

    @Test
    void testEndpoint_devuelveString() throws Exception {
        when(repository.count()).thenReturn(15L);

        mockMvc.perform(get("/api/messages/api/test"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Total de mensajes: 15")));
    }

    /////////////////////////////////////////////////////////////////////////////
    ///

}
