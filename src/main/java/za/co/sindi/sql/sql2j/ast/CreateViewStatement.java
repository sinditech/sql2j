package za.co.sindi.sql.sql2j.ast;

import java.util.List;

/**
 * {@code CREATE [OR REPLACE] VIEW name [(col, ...)] AS <query>}.
 * <p>
 * Full {@code SELECT} parsing is deliberately out of scope for a <em>DDL</em>
 * parser, so the query body is kept as an opaque, best-effort reconstructed
 * SQL string rather than its own AST. Everything else about the statement is
 * fully structural.
 */
public record CreateViewStatement(
        QualifiedName viewName,
        boolean orReplace,
        List<String> columnNames,
        String query
) implements CreateStatement {}
