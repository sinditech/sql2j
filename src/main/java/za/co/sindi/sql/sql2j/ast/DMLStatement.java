package za.co.sindi.sql.sql2j.ast;

/**
 * Root of the AST: every top-level DDL statement the parser can produce.
 * <p>
 * This is a sealed interface with an explicit {@code permits} clause. That
 * closes the hierarchy so any {@code switch} that pattern-matches over
 * {@code Statement} (see {@link com.ASTPrinter.util.AstPrinter}) is checked for
 * exhaustiveness by the compiler &mdash; add a new statement kind and every
 * such switch fails to compile until it's handled, which is exactly the
 * guarantee the Visitor pattern traditionally provided, obtained here for
 * free from the type system instead of boilerplate {@code accept()} methods.
 */
public non-sealed interface DMLStatement extends Statement {
}
