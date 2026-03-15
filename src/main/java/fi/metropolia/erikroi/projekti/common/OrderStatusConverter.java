package fi.metropolia.erikroi.projekti.common;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class OrderStatusConverter implements AttributeConverter<OrderStatus, String> {

    @Override
    public String convertToDatabaseColumn(OrderStatus status) {
        if (status == null) return null;
        return status.name(); // Converts Enum to String (e.g., "PENDING")
    }

    @Override
    public OrderStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) return null;
        return OrderStatus.valueOf(dbData.toUpperCase()); // Converts String back to Enum
    }
}