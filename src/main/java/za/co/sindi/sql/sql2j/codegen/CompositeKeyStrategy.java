package za.co.sindi.sql.sql2j.codegen;

/**
 * How {@link JpaEntityGenerator} represents a table's composite primary key
 * in generated code — both are pure-JPA, spec-defined mechanisms; the
 * choice between them is a team/style preference this generator makes
 * configurable rather than picking for you.
 */
public enum CompositeKeyStrategy {
    /** A companion class with plain fields, referenced via {@code @IdClass} on the entity and {@code @Id} on each matching entity field. */
    ID_CLASS,
    /** A companion {@code @Embeddable} class referenced via a single {@code @EmbeddedId} field; relationship fields that participate in the key use {@code @MapsId}. */
    EMBEDDED_ID
}
