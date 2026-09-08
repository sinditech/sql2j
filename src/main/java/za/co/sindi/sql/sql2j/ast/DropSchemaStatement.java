package za.co.sindi.sql.sql2j.ast;

/** {@code DROP SCHEMA [IF EXISTS] name [CASCADE|RESTRICT]}. */
public record DropSchemaStatement(String schemaName, boolean ifExists, CascadeOption cascadeOption) implements DropStatement {}
