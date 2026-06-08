package com.unisul.seatreservation.dto;

import java.util.List;

// Evento enviado para o Pagamento ou enviado de volta para o Order em caso de erro
public record OrderResponseEvent(String sagaId, String orderId, List<TicketResultDTO> ticketList)
        implements  SagaResponseEvent{}