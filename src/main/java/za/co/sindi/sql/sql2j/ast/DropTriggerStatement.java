package za.co.sindi.sql.sql2j.ast;

/**
 * {@code DROP TRIGGER [IF EXISTS] name [ON table_name] [CASCADE|RESTRICT]}.
 * {@code tableName} is {@code Optional} because MySQL/standard-SQL drop a
 * trigger purely by (schema-qualified) name, while PostgreSQL requires
 * naming the table too ({@code ON table_name}).
 */
public record DropTriggerStatement(
        QualifiedName triggerName,
        boolean ifExists,
        java.util.Optional<QualifiedName> tableName,
        CascadeOption cascadeOption
) implements DropStatement {}
