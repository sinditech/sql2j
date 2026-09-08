package za.co.sindi.sql.sql2j.ast;

import java.util.List;

/** A single column in a {@code CREATE TABLE} (or a column added via {@code ALTER TABLE ADD COLUMN}). */
public record ColumnDefinition(String name, DataType dataType, List<ColumnConstraint> constraints) {

    public boolean isNotNull() {
        return constraints.stream().anyMatch(ColumnConstraint.NotNull.class::isInstance);
    }

    public boolean isPrimaryKey() {
        return constraints.stream().anyMatch(ColumnConstraint.PrimaryKey.class::isInstance);
    }
}
