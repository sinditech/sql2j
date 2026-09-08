package za.co.sindi.sql.sql2j.ast;

/** {@code TRUNCATE TABLE name}. */
public record TruncateTableStatement(
        QualifiedName tableName
) implements TruncateStatement {}
