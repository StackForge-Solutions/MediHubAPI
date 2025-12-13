package com.MediHubAPI.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Converts OffsetDateTime <-> Timestamp for MySQL.
 * Stores UTC timestamps in DB while preserving OffsetDateTime in Java.
 */
@Converter(autoApply = true)
public class OffsetDateTimeUtcConverter implements AttributeConverter<OffsetDateTime, Timestamp> {

    @Override
    public Timestamp convertToDatabaseColumn(OffsetDateTime attribute) {
        // Convert OffsetDateTime to UTC timestamp
        return (attribute == null) ? null : Timestamp.from(attribute.toInstant());
    }

    @Override
    public OffsetDateTime convertToEntityAttribute(Timestamp dbData) {
        // Convert stored timestamp back to OffsetDateTime with UTC offset
        return (dbData == null) ? null : dbData.toInstant().atOffset(ZoneOffset.UTC);
    }
}
