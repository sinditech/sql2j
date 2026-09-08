package za.co.sindi.sql.sql2j.ast;

import java.util.Optional;

/**
 * One trailing table option after {@code CREATE TABLE (...)}  — MySQL's
 * {@code ENGINE=InnoDB}/{@code DEFAULT CHARSET=utf8mb4}/{@code AUTO_INCREMENT=100}
 * style, or a PostgreSQL {@code WITH (fillfactor = 70)} storage parameter.
 * <p>
 * Kept as reconstructed raw text for the same reason {@link TypeOption} is:
 * these option lists mix identifiers, numbers, and quoted strings under one
 * {@code name = value} syntax that's set by the RDBMS, not by a fixed SQL
 * standard, so there's no single typed sub-grammar that would fit every
 * option correctly.
 */
public record TableOption(String name, Optional<String> value) {}
