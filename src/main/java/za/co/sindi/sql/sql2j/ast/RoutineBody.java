package za.co.sindi.sql.sql2j.ast;

/**
 * The body of a {@code CREATE FUNCTION}/{@code CREATE PROCEDURE} statement.
 * <p>
 * Full procedural-language parsing (PL/pgSQL, PL/SQL, T-SQL, ...) is out of
 * scope for a DDL parser, exactly like full {@code SELECT} parsing is for
 * {@code CREATE VIEW} — so most bodies are captured as their raw source text
 * rather than their own statement-level AST. The one case that <em>is</em>
 * fully parsed into the existing {@link Expression} AST is a bare
 * {@code RETURN <expr>} body, since that's just a scalar expression the
 * parser already understands perfectly well.
 */
public sealed interface RoutineBody {

    /**
     * A PostgreSQL dollar-quoted body: {@code AS $$ ... $$} or {@code AS $tag$ ... $tag$}.
     * The unambiguous, fully-robust option — dollar-quoting exists precisely so the body's
     * content (quotes, semicolons, nested blocks, anything) never needs escaping or balanced
     * parsing to find where it ends.
     */
    record DollarQuoted(String tag, String source) implements RoutineBody {}

    /** A plain quoted-string body: {@code AS 'body text'}. */
    record QuotedString(String source) implements RoutineBody {}

    /**
     * A {@code BEGIN ... END} procedural block (MySQL/standard-SQL style), captured via
     * best-effort balanced-block scanning. Correctly handles nested {@code BEGIN...END} and
     * {@code END IF}/{@code END CASE}/{@code END LOOP}/{@code END WHILE}/{@code END FOR}
     * terminators. <b>Known limitation:</b> a bare {@code CASE ... END} <em>expression</em>
     * (as opposed to a {@code CASE ... END CASE} statement) inside the block can be
     * misinterpreted as closing the enclosing {@code BEGIN}, because both use an unadorned
     * {@code END}. Bodies containing such expressions should use a dollar-quoted body instead,
     * which sidesteps the ambiguity entirely.
     */
    record BeginEndBlock(String source) implements RoutineBody {}

    /** A bare {@code RETURN <expr>} body — fully parsed into the {@link Expression} AST. */
    record ReturnExpression(Expression expression) implements RoutineBody {}

    /**
     * A single, un-wrapped statement with no {@code AS}/{@code BEGIN...END}/dollar-quoting
     * at all — permitted for MySQL trigger bodies specifically (e.g.
     * {@code CREATE TRIGGER t BEFORE INSERT ON x FOR EACH ROW SET NEW.y = NOW();}), where a
     * one-line trigger is common enough that MySQL doesn't require a wrapper for it. Captured
     * as raw text up to the terminating {@code ;}, same as {@link BeginEndBlock}.
     */
    record SingleStatement(String source) implements RoutineBody {}
}
