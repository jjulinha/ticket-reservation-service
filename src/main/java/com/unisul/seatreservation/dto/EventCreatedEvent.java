package com.unisul.seatreservation.dto;

// Evento recebido do Serviço de Eventos quando um show é criado
public record EventCreatedEvent(String eventId, Integer capacity) {}