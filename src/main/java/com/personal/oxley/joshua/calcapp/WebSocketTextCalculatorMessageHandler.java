package com.personal.oxley.joshua.calcapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class WebSocketTextCalculatorMessageHandler extends TextWebSocketHandler {

    final private Pattern EXPRESSION = Pattern.compile("(\\d+)([\\-xX\\+/])(\\d+)");
    final private Set<InternalWebSocketSession> sessions = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Payload payload = Payload.builder()
                .name("" + session.getId())
                .type("id")
                .date(Instant.now().getEpochSecond())
                .id(sessions.size() + 1).build();
        String msg = objectMapper.writeValueAsString(payload);
        TextMessage newMessage = new TextMessage(msg);
        session.sendMessage(newMessage);

        payload.setType("username");

        msg = objectMapper.writeValueAsString(payload);
        TextMessage messageOtherSessions = new TextMessage(msg);
        sessions.forEach(s -> {
            try {
                s.session.sendMessage(messageOtherSessions);
            } catch (Exception e) {

            }
        });
        sessions.add(new InternalWebSocketSession(session));

    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Payload content = objectMapper.readValue(message.getPayload(), Payload.class);
        if (isValid(content)) {
            sessions.stream()
                    .forEach(saved -> {
                        try {
                            Payload newPayload = calculator(content);
                            String msg = objectMapper.writeValueAsString(newPayload);
                            TextMessage newMessage = new TextMessage(msg);
                            saved.session.sendMessage(newMessage);
                        } catch (IOException e) {
                            //Catch
                        }
                    });
        }
    }

    private boolean isValid(Payload content) {
        return content != null && content.getText() != null && content.getText().matches("[\\d\\sxX+\\-/*]+");
    }

    //This calculates everything
    private Payload calculator(Payload content) {
        Matcher matcher = EXPRESSION.matcher(content.getText().replaceAll("\\s+", ""));
        Integer total = null;
        if (matcher.find()) {
            StringBuilder builder = new StringBuilder(content.getText());
            total = Integer.valueOf(matcher.group(1));

            for (int i = 3; i <= matcher.groupCount(); i += 2) {
                String symbol = matcher.group(i - 1);
                Integer number = Integer.valueOf(matcher.group(i));
                switch (symbol) {
                    case "/":
                        total /= number;
                        break;
                    case "-":
                        total -= number;
                        break;
                    case "+":
                        total += number;
                        break;
                    case "X":
                    case "x":
                    case "*":
                        total *= number;
                        break;
                    default:
                        //Error;
                }
            }
            builder.append(" = " + total);
            return Payload.builder()
                    .text(builder.toString())
                    .date(Instant.now().getEpochSecond())
                    .id(content.getId())
                    .type(content.getType())
                    .name(content.getName())
                    .build();
        }
        return content;
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(new InternalWebSocketSession(session));
    }

    private static class InternalWebSocketSession {
        final WebSocketSession session;
        final String id;

        public InternalWebSocketSession(WebSocketSession session) {
            this.session = session;
            this.id = session.getId();
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            InternalWebSocketSession that = (InternalWebSocketSession) o;
            return Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }
}
