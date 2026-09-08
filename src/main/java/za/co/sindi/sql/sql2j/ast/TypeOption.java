package za.co.sindi.sql.sql2j.ast;

import java.util.Optional;

/**
 * One {@code option = value} entry (or a bare flag with no value, e.g.
 * {@code PASSEDBYVALUE}) inside a {@code CREATE TYPE ... AS RANGE (...)} or
 * base-type {@code CREATE TYPE name (...)} option list.
 * <p>
 * {@code value} is deliberately kept as reconstructed raw text rather than
 * parsed into a typed sub-grammar: PostgreSQL's own option list mixes
 * function-name identifiers ({@code INPUT = my_input_fn}), type references
 * ({@code SUBTYPE = int4range}), numbers ({@code INTERNALLENGTH = 4}),
 * bare keywords ({@code INTERNALLENGTH = VARIABLE}), and quoted strings
 * ({@code DEFAULT = 'x'}) — all under the same {@code =} syntax, with no
 * way to tell which kind a given option expects without hard-coding
 * PostgreSQL's per-option documentation into the parser. Capturing the
 * value as text (constant-folded consistently with the DEFAULT/CHECK
 * expression printer elsewhere) parses every option correctly without
 * guessing at its semantic type.
 */
public record TypeOption(String name, Optional<String> value) {}
