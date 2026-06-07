-- `seat-reservation`.tb_bookings definition

CREATE TABLE `tb_bookings` (
                               `order_id` binary(16) NOT NULL,
                               `user_id` binary(16) NOT NULL,
                               `event_id` binary(16) NOT NULL,
                               `booking_status` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
                               `total_price` decimal(10,2) NOT NULL,
                               `created_at` timestamp NULL DEFAULT NULL,
                               PRIMARY KEY (`order_id`),
                               KEY `idx_bookings_user` (`user_id`),
                               KEY `idx_bookings_event` (`event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- `seat-reservation`.tb_event_stock definition

CREATE TABLE `tb_event_stock` (
                                  `event_id` binary(16) NOT NULL,
                                  `available_capacity` int NOT NULL,
                                  PRIMARY KEY (`event_id`),
                                  CONSTRAINT `chk_positive_stock` CHECK ((`available_capacity` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- `seat-reservation`.tb_tickets definition

CREATE TABLE `tb_tickets` (
                              `ticket_id` binary(16) NOT NULL,
                              `order_id` binary(16) NOT NULL,
                              `event_id` binary(16) NOT NULL,
                              `seat_identifier` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
                              `ticket_price` decimal(10,2) NOT NULL,
                              `ticket_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
                              PRIMARY KEY (`ticket_id`),
                              KEY `fk_tickets_booking` (`order_id`),
                              KEY `idx_tickets_event` (`event_id`),
                              CONSTRAINT `fk_tickets_booking` FOREIGN KEY (`order_id`) REFERENCES `tb_bookings` (`order_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;