package com.unisul.seatreservation.dto;

import java.math.BigDecimal;

// Evento recebido do Serviço de Eventos quando um show é criado
public record EventCreatedEvent(String eventId, Integer capacity, BigDecimal ticketPrice) {}