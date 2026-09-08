package za.co.sindi.sql.sql2j.ast;

import java.util.List;

/** {@code CREATE [UNIQUE] INDEX [IF NOT EXISTS] name ON table (col, ...)}. */
public record CreateIndexStatement(
        String indexName,
        boolean unique,
        boolean ifNotExists,
        QualifiedName tableName,
        List<String> columns
) implements CreateStatement {}
