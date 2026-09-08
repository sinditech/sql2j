package za.co.sindi.sql.sql2j.ast;

import java.util.List;

/**
 * {@code CREATE TABLE [IF NOT EXISTS] name (col ..., ..., CONSTRAINT ...) [table-options]}.
 * <p>
 * The AST shape is the same across every dialect: {@code indexes} and
 * {@code options} simply stay empty unless the parser is running in a
 * dialect that actually has the corresponding syntax (MySQL's inline
 * {@code KEY}/{@code INDEX} items and {@code ENGINE=}/{@code CHARSET=}
 * table options, or PostgreSQL's {@code WITH (...)} storage parameters) —
 * see {@code com.sqlddl.dialect.DialectProfile}.
 */
public record CreateTableStatement(
        QualifiedName tableName,
        boolean ifNotExists,
        List<ColumnDefinition> columns,
        List<TableConstraint> constraints,
        List<InlineIndexDefinition> indexes,
        List<TableOption> options
) implements CreateStatement {}
