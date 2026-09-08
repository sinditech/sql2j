package za.co.sindi.sql.sql2j.ast;

/** {@code CREATE TYPE name <definition>} — see {@link TypeDefinition} for the five shapes PostgreSQL supports. */
public record CreateTypeStatement(QualifiedName typeName, TypeDefinition definition) implements CreateStatement {}
