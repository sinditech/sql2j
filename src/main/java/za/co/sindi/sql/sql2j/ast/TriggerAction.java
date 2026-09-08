package za.co.sindi.sql.sql2j.ast;

import java.util.List;

/**
 * What runs when a trigger fires — the single biggest syntactic split
 * across RDBMSes for {@code CREATE TRIGGER}:
 * <ul>
 *   <li><b>PostgreSQL</b> triggers never have an inline body at all; they
 *       always name a separately-defined trigger function:
 *       {@code EXECUTE FUNCTION fn()} (or the older {@code EXECUTE PROCEDURE fn()}).</li>
 *   <li><b>MySQL, Oracle, SQL Server, and SQLite</b> triggers embed the
 *       logic directly — a single statement or a {@code BEGIN ... END}
 *       block, exactly the same shape {@link RoutineBody} already models
 *       for {@code CREATE FUNCTION}/{@code PROCEDURE} bodies. Reusing that
 *       type here means the same body-capture logic (dollar-quoting,
 *       balanced {@code BEGIN...END} scanning, bare {@code RETURN}) works
 *       for trigger bodies without duplicating it.</li>
 * </ul>
 */
public sealed interface TriggerAction {

    record Body(RoutineBody body) implements TriggerAction {}

    record ExecuteFunction(QualifiedName functionName, List<Expression> arguments, boolean legacyProcedureSyntax) implements TriggerAction {}
}
