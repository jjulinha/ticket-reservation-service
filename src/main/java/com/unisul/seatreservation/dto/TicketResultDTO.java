package com.unisul.seatreservation.dto;

import com.unisul.seatreservation.domain.Ticket;

import java.util.List;

public record TicketResultDTO(String ticketId, String ticketPrice, String ticketType) {

    public static TicketResultDTO from(Ticket ticket) {
        return new TicketResultDTO(
                ticket.getTicketId().toString(),
                ticket.getTicketPrice().toString(),
                ticket.getTicketType()
        );
    }

    public static List<TicketResultDTO> fromList(List<Ticket> tickets) {
        if (tickets == null) {
            return List.of();
        }

        return tickets.stream()
                .map(TicketResultDTO::from)
                .toList();
    }
}
