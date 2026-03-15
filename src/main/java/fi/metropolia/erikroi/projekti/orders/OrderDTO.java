package fi.metropolia.erikroi.projekti.orders;

import fi.metropolia.erikroi.projekti.common.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDTO(
        Long id,
        LocalDateTime orderDate,
        OrderStatus status,
        Double totalAmount,
        List<OrderItemDTO> items
) {}