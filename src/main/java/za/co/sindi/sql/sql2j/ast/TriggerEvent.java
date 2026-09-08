package za.co.sindi.sql.sql2j.ast;

import java.util.List;

/**
 * One triggering event. MySQL allows exactly one; PostgreSQL, Oracle, SQL
 * Server, and SQLite all allow several {@code OR}'d together on one
 * trigger (e.g. {@code BEFORE INSERT OR UPDATE OR DELETE}) — hence
 * {@link CreateTriggerStatement#events()} is always a list, even though
 * it will only ever hold one element for a MySQL-parsed trigger.
 */
public sealed interface TriggerEvent {

    record Insert() implements TriggerEvent {}

    /** {@code UPDATE} or, with column-level granularity, {@code UPDATE OF col1, col2}. Empty list = any column. */
    record Update(List<String> columns) implements TriggerEvent {}

    record Delete() implements TriggerEvent {}

    /** PostgreSQL-only: statement-level {@code TRUNCATE} triggers. */
    record Truncate() implements TriggerEvent {}
}
