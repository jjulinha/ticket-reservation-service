package com.unisul.seatreservation.mapper;

import com.unisul.seatreservation.domain.Booking;
import com.unisul.seatreservation.domain.Ticket;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Mapper
public interface BookingMapper {

    // Retorna o pedido completo populado com a sua lista interna de assentos (Tickets)
    Booking findBookingWithTickets(@Param("orderId") UUID orderId);

    // Retorna apenas a lista de assentos de um pedido (útil para otimizações)
    List<Ticket> findTicketsByOrderId(@Param("orderId") UUID orderId);

    int decrementStock(@Param("eventId") UUID eventId, @Param("quantity") int quantity);

    int incrementStock(@Param("eventId") UUID eventId, @Param("quantity") int quantity);

    void insertEventStock(@Param("eventId") UUID eventId, @Param("capacity") int capacity, @Param("ticketPrice") BigDecimal ticketPrice);

    BigDecimal getEventTicketPrice(@Param("eventId") UUID eventId);

    void insertBooking(Booking booking);

    void insertTicket(Ticket ticket);

    void updateBookingStatus(@Param("orderId") UUID orderId, @Param("status") String status);
}