package za.co.sindi.sql.sql2j.ast;

import java.util.List;
import java.util.Optional;

/**
 * {@code CREATE TRIGGER}, unified across RDBMSes into one AST shape even
 * though the concrete syntax varies more here than almost anywhere else in
 * SQL DDL. The fields are a superset of what any single dialect uses; a
 * trigger parsed from any one dialect simply leaves the fields that
 * dialect doesn't have at their empty/default value:
 *
 * <table>
 *   <caption>Field origin by dialect</caption>
 *   <tr><th>Field</th><th>Present in</th></tr>
 *   <tr><td>{@code orReplace}</td><td>PostgreSQL, Oracle; SQL Server spells it {@code OR ALTER}</td></tr>
 *   <tr><td>{@code constraintTrigger}</td><td>PostgreSQL only ({@code CONSTRAINT TRIGGER}, deferrable)</td></tr>
 *   <tr><td>{@code events} with more than one entry</td><td>PostgreSQL, Oracle, SQL Server, SQLite (MySQL allows exactly one)</td></tr>
 *   <tr><td>{@code referencing}</td><td>PostgreSQL transition tables, Oracle {@code OLD}/{@code NEW}/{@code PARENT} aliases</td></tr>
 *   <tr><td>{@code when}</td><td>PostgreSQL, Oracle, SQLite (not MySQL)</td></tr>
 *   <tr><td>{@code action} as {@link TriggerAction.ExecuteFunction}</td><td>PostgreSQL only</td></tr>
 *   <tr><td>{@code action} as {@link TriggerAction.Body}</td><td>MySQL, Oracle, SQL Server, SQLite</td></tr>
 *   <tr><td>{@code order}</td><td>MySQL only ({@code FOLLOWS}/{@code PRECEDES})</td></tr>
 * </table>
 *
 * See {@code com.sqlddl.parser.Parser#parseCreateTrigger} for exactly which
 * of these this parser enforces per {@link com.sqlddl.dialect.SqlDialect}
 * (concretely implemented for {@code MYSQL}/{@code POSTGRESQL}/{@code GENERIC};
 * the AST shape itself already accommodates Oracle/SQL-Server/SQLite syntax
 * for a future dialect profile, per the extension recipe documented in the README).
 */
public record CreateTriggerStatement(
        QualifiedName triggerName,
        boolean orReplace,
        boolean constraintTrigger,
        TriggerTiming timing,
        List<TriggerEvent> events,
        QualifiedName tableName,
        List<TriggerReference> referencing,
        Optional<TriggerLevel> level,
        Optional<Expression> when,
        TriggerAction action,
        Optional<TriggerOrder> order
) implements CreateStatement {}
