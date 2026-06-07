package com.unisul.seatreservation.dto;

// Evento enviado para o Pagamento ou enviado de volta para o Order em caso de erro
public record OrderResponseEvent(String sagaId, String orderId, String reason) {}