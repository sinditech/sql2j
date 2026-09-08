package za.co.sindi.sql.sql2j.ast;

/** {@code DROP INDEX [IF EXISTS] name}. */
public record DropIndexStatement(String indexName, boolean ifExists) implements DropStatement {}
