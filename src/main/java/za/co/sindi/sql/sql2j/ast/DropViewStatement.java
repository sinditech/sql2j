package za.co.sindi.sql.sql2j.ast;

/** {@code DROP VIEW [IF EXISTS] name}. */
public record DropViewStatement(QualifiedName viewName, boolean ifExists) implements DropStatement {}
