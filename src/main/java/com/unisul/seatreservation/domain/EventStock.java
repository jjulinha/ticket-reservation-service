package com.unisul.seatreservation.domain;

import java.math.BigDecimal;
import java.util.UUID;

public class EventStock {
    UUID eventId;
    Integer capacity;
    BigDecimal ticketPrice;

    public UUID getEventId() {
        return eventId;
    }
    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public Integer getCapacity() {
        return capacity;
    }
    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public BigDecimal getTicketPrice() {
        return ticketPrice;
    }
    public void setTicketPrice(BigDecimal ticketPrice) {
        this.ticketPrice = ticketPrice;
    }
}
