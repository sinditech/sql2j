package za.co.sindi.sql.sql2j.ast;

/** {@code CREATE SCHEMA [IF NOT EXISTS] name}. */
public record CreateSchemaStatement(String schemaName, boolean ifNotExists) implements CreateStatement {}
