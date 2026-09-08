package za.co.sindi.sql.sql2j.ast;

import java.util.List;

/**
 * The body of a PostgreSQL {@code CREATE TYPE} statement. PostgreSQL
 * supports five distinct shapes, each with a genuinely different grammar
 * after the type name — modeled as a sealed hierarchy for the same reason
 * {@link RoutineBody} and {@link RoutineReturnType} are: an exhaustive
 * {@code switch} over the variants is checked by the compiler, so handling
 * (or deliberately not handling) each shape is a conscious choice, not an
 * accident.
 */
public sealed interface TypeDefinition {

    /** {@code CREATE TYPE name AS (attr type [COLLATE collation], ...)} — a composite (row) type. */
    record Composite(List<CompositeAttribute> attributes) implements TypeDefinition {}

    /** {@code CREATE TYPE name AS ENUM ('label', ...)}. */
    record Enum(List<String> labels) implements TypeDefinition {}

    /** {@code CREATE TYPE name AS RANGE (SUBTYPE = type, ...)}. */
    record Range(List<TypeOption> options) implements TypeDefinition {}

    /**
     * {@code CREATE TYPE name (INPUT = fn, OUTPUT = fn, ...)} — a base type, the
     * low-level form used by extension authors to wrap a C-level type implementation.
     * Rarely hand-written outside of extension code, but structurally simple: just
     * a parenthesized {@code option = value} list, exactly like {@link Range}.
     */
    record Base(List<TypeOption> options) implements TypeDefinition {}

    /**
     * {@code CREATE TYPE name;} — a "shell" type: a forward declaration with no body
     * at all, used so a base type's I/O functions can reference the type before its
     * full definition exists (they're necessarily circular).
     */
    record Shell() implements TypeDefinition {}
}
