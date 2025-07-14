package com.matchwork.chatservice.controller;

import com.matchwork.chatservice.model.ChatMessage;
import com.matchwork.chatservice.repository.ChatMessageRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatController.class)
public class ChatControllerTest {

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
}
