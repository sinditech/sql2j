package za.co.sindi.sql.sql2j.ast;

/**
 * One entry of a trigger's {@code REFERENCING} clause, e.g. PostgreSQL's
 * {@code REFERENCING NEW TABLE AS new_rows} (a whole-statement transition
 * table) or Oracle's {@code REFERENCING OLD AS old_row} (a single-row
 * alias). {@code isTable} distinguishes the two: {@code true} for a
 * transition table (statement-level triggers), {@code false} for a plain
 * per-row alias.
 */
public record TriggerReference(TriggerTransition transition, boolean isTable, String alias) {}
