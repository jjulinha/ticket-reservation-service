package com.unisul.seatreservation.dto;

public record FailureResponseEvent(String sagaId, String orderId, String reason)
        implements SagaResponseEvent{}
