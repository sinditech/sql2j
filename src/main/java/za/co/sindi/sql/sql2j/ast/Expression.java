package za.co.sindi.sql.sql2j.ast;

import java.util.List;

/**
 * Scalar SQL expressions — originally built for {@code DEFAULT}/{@code CHECK}
 * clauses in DDL, and reused as-is for {@code WHERE}/{@code HAVING}/{@code ON}
 * conditions and {@code SELECT} items in {@code SELECT}/{@code UPDATE}/
 * {@code DELETE} statements, including scalar and predicate subqueries
 * ({@link Subquery}, {@link Exists}, {@link InSubquery}) that bridge into
 * {@link SelectStatement}.
 *
 * <p>This is a <b>sealed</b> hierarchy (Java's closed algebraic data type
 * facility). Combined with records for each variant, it lets any consumer
 * exhaustively {@code switch} over every possible shape of expression and
 * have the compiler flag it if a new variant is ever added and a switch
 * wasn't updated to handle it &mdash; the same safety a Visitor buys you,
 * without needing every node to implement {@code accept()}.
 */
public sealed interface Expression extends SQLNode {

    /** A literal value: number, string, boolean, or {@code NULL}. */
    record Literal(Object value, LiteralKind kind) implements Expression {
        public static Literal ofNull() {
            return new Literal(null, LiteralKind.NULL);
        }
    }

    enum LiteralKind { STRING, NUMBER, BOOLEAN, NULL }

    /** A bare column name reference, e.g. {@code price} inside {@code CHECK (price > 0)}. */
    record ColumnReference(String name) implements Expression {}

    /** A function call such as {@code CURRENT_TIMESTAMP} or {@code COALESCE(a, b)}. */
    record FunctionCall(String name, List<Expression> arguments) implements Expression {}

    /** A parenthesized sub-expression, kept explicit so pretty-printing can round-trip it. */
    record Parenthesized(Expression inner) implements Expression {}

    /** A prefix unary expression, e.g. {@code -1} or {@code NOT active}. */
    record Unary(String operator, Expression operand) implements Expression {}

    /** A binary expression, e.g. {@code a + b} or {@code price >= 0}. */
    record Binary(Expression left, String operator, Expression right) implements Expression {}

    /** {@code expr [NOT] BETWEEN lower AND upper}. */
    record Between(Expression target, Expression lower, Expression upper, boolean negated) implements Expression {}

    /** {@code expr [NOT] IN (v1, v2, ...)}. */
    record In(Expression target, List<Expression> values, boolean negated) implements Expression {}

    /** {@code expr [NOT] LIKE pattern}. */
    record Like(Expression target, Expression pattern, boolean negated) implements Expression {}

    /** {@code expr IS [NOT] NULL}. */
    record IsNull(Expression target, boolean negated) implements Expression {}

//    /**
//     * A scalar subquery used as a value, e.g. {@code price > (SELECT AVG(price) FROM products)}.
//     * Kept distinct from {@link Parenthesized} since a subquery is a {@link SelectStatement}, not
//     * another {@code Expression} — this is the bridge between the two ASTs.
//     */
//    record Subquery(SelectStatement query) implements Expression {}
//
//    /** {@code [NOT] EXISTS (SELECT ...)}. */
//    record Exists(SelectStatement query, boolean negated) implements Expression {}
//
//    /** {@code expr [NOT] IN (SELECT ...)} — the subquery counterpart to {@link In}'s expression list. */
//    record InSubquery(Expression target, SelectStatement query, boolean negated) implements Expression {}

    /**
     * A bare {@code DEFAULT} used as a value placeholder inside an {@code INSERT ... VALUES}
     * row (e.g. {@code VALUES (1, DEFAULT, 3)}), telling the database to fill that column
     * with its declared default rather than an explicit value. Distinct from
     * {@link ColumnConstraint.DefaultValue}, which is the *declaration* of what a column's
     * default value expression is — this is a reference to "whatever that is," used at the
     * call site.
     */
    record Default() implements Expression {}
}
