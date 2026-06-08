package com.unisul.seatreservation.dto;

public sealed interface SagaResponseEvent
        permits OrderResponseEvent, FailureResponseEvent {

    String sagaId();
}