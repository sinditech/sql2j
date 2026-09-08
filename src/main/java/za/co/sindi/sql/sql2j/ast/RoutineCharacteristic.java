package za.co.sindi.sql.sql2j.ast;

/**
 * One "characteristic" clause of a {@code CREATE FUNCTION}/{@code CREATE
 * PROCEDURE} statement — {@code LANGUAGE}, {@code DETERMINISTIC},
 * volatility, null-handling, or security mode. Modeled as a sealed
 * hierarchy (rather than a grab-bag of nullable fields on the statement
 * record) so adding a new characteristic (e.g. {@code COST}/{@code ROWS}/
 * {@code PARALLEL}) later is just one new record variant, with the compiler
 * flagging every exhaustive {@code switch} that needs a new case.
 */
public sealed interface RoutineCharacteristic {

    record Language(String name) implements RoutineCharacteristic {}

    record Deterministic(boolean deterministic) implements RoutineCharacteristic {}

    record Volatility(VolatilityCategory category) implements RoutineCharacteristic {}

    record NullHandling(NullHandlingMode mode) implements RoutineCharacteristic {}

    record Security(SecurityMode mode) implements RoutineCharacteristic {}
}
