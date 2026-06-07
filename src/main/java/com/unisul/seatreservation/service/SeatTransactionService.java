package com.unisul.seatreservation.service;
import com.fasterxml.uuid.Generators;
import com.unisul.seatreservation.domain.Booking;
import com.unisul.seatreservation.domain.ReservationResult;
import com.unisul.seatreservation.domain.Ticket;
import com.unisul.seatreservation.dto.OrderCreatedEvent;
import com.unisul.seatreservation.mapper.BookingMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class SeatTransactionService {

    private final BookingMapper bookingMapper;

    public SeatTransactionService(BookingMapper bookingMapper) {
        this.bookingMapper = bookingMapper;
    }

    @Transactional
    public ReservationResult tryProcessStockReservation(OrderCreatedEvent event, UUID orderId, UUID userId, UUID eventId) {
        int totalItems = event.items().size();

        Optional<Booking> existing = Optional.ofNullable(bookingMapper.findBookingWithTickets(orderId));
        if (existing.isPresent()) {
            return ReservationResult.ALREADY_PROCESSED;
        }

        int rowsAffected = bookingMapper.decrementStock(eventId, totalItems);
        if (rowsAffected == 0) {
            return ReservationResult.OUT_OF_STOCK;
        }

        Booking booking = new Booking();
        booking.setOrderId(orderId);
        booking.setUserId(userId);
        booking.setEventId(eventId);
        booking.setBookingStatus("PENDING");
        booking.setTotalPrice(event.totalPrice());

        bookingMapper.insertBooking(booking);

        for (OrderCreatedEvent.ItemEvent item : event.items()) {
            Ticket ticket = new Ticket();
            ticket.setTicketId(Generators.timeBasedEpochGenerator().generate());
            ticket.setOrderId(orderId);
            ticket.setEventId(eventId);
            ticket.setSeatIdentifier(item.seatIdentifier());
            ticket.setTicketPrice(event.totalPrice());
            ticket.setTicketType(item.ticketType());

            bookingMapper.insertTicket(ticket);
        }

        return ReservationResult.SUCCESS;
    }

    @Transactional
    public void executeCompensation(UUID orderId) {
        List<Ticket> ticketsReservados = bookingMapper.findTicketsByOrderId(orderId);

        if (ticketsReservados != null && !ticketsReservados.isEmpty()) {
            UUID eventId = ticketsReservados.get(0).getEventId();
            int quantidadeParaDevolver = ticketsReservados.size();

            bookingMapper.updateBookingStatus(orderId, "COMPENSATED");
            bookingMapper.incrementStock(eventId, quantidadeParaDevolver);
        }
    }

    @Transactional
    public void executeRegisterNewEventStock(UUID eventId, int capacity) {
        bookingMapper.insertEventStock(eventId, capacity);
    }
}