ALTER TABLE `tb_bookings`
DROP COLUMN `total_price`;

ALTER TABLE `tb_event_stock`
ADD COLUMN `ticket_price` decimal(10,2) NOT NULL;