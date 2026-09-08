package za.co.sindi.sql.sql2j.ast;

import java.util.List;
import java.util.Optional;

/**
 * A constraint attached directly to a single column definition, e.g. the
 * {@code NOT NULL} in {@code age INT NOT NULL}.
 */
public sealed interface ColumnConstraint {

    record NotNull() implements ColumnConstraint {}

    record Nullable() implements ColumnConstraint {}

    record PrimaryKey() implements ColumnConstraint {}

    record Unique() implements ColumnConstraint {}

    record AutoIncrement() implements ColumnConstraint {}

    record DefaultValue(Expression value) implements ColumnConstraint {}

    record Check(Optional<String> name, Expression expression) implements ColumnConstraint {}

    record Collate(String collation) implements ColumnConstraint {}

    record References(
            QualifiedName referencedTable,
            List<String> referencedColumns,
            Optional<ReferentialAction> onDelete,
            Optional<ReferentialAction> onUpdate
    ) implements ColumnConstraint {}
}
