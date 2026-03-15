package fi.metropolia.erikroi.projekti.orders;

public record OrderItemDTO(
        Long id,
        String productName,
        Integer quantity,
        Double unitPrice
) {}