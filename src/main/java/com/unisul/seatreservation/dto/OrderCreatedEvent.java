package com.unisul.seatreservation.dto;

import java.math.BigDecimal;
import java.util.List;

// Evento recebido do Order Service
public record OrderCreatedEvent(String sagaId, String orderId, String userId, String eventId,
                                BigDecimal totalPrice, List<ItemEvent> items) {
    public record ItemEvent(String ticketType, Integer quantity, String seatIdentifier) {}
}