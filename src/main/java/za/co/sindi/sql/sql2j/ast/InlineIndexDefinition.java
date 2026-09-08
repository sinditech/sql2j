package za.co.sindi.sql.sql2j.ast;

import java.util.List;
import java.util.Optional;

/**
 * A MySQL-only inline {@code KEY name (col, ...)} / {@code INDEX name (col, ...)}
 * item inside a {@code CREATE TABLE (...)} body — a plain performance index,
 * not an integrity constraint, so it's kept separate from {@link TableConstraint}
 * rather than shoehorned into that (constraint-focused) hierarchy.
 * <p>
 * Standard SQL and PostgreSQL have no equivalent: an index there is always
 * its own separate {@code CREATE INDEX} statement (see {@link CreateIndexStatement}).
 */
public record InlineIndexDefinition(Optional<String> name, List<String> columns) {}
