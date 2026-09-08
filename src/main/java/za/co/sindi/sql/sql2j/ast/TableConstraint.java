package za.co.sindi.sql.sql2j.ast;

import java.util.List;
import java.util.Optional;

/**
 * A constraint declared at table level, e.g.
 * {@code CONSTRAINT pk_users PRIMARY KEY (id)} or an inline
 * {@code FOREIGN KEY (dept_id) REFERENCES departments(id)}.
 */
public sealed interface TableConstraint {

    Optional<String> name();

    record PrimaryKey(Optional<String> name, List<String> columns) implements TableConstraint {}

    record Unique(Optional<String> name, List<String> columns) implements TableConstraint {}

    record Check(Optional<String> name, Expression expression) implements TableConstraint {}

    record ForeignKey(
            Optional<String> name,
            List<String> columns,
            QualifiedName referencedTable,
            List<String> referencedColumns,
            Optional<ReferentialAction> onDelete,
            Optional<ReferentialAction> onUpdate
    ) implements TableConstraint {}
}
