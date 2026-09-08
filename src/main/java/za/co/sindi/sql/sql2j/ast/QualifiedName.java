package za.co.sindi.sql.sql2j.ast;

import java.util.Objects;
import java.util.Optional;

/**
 * A possibly schema-qualified object name, e.g. {@code public.users} or just
 * {@code users}.
 */
public record QualifiedName(Optional<String> database, Optional<String> schema, String name) {

    public QualifiedName {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(schema, "schema");
        Objects.requireNonNull(database, "database");
    }

    public static QualifiedName of(String name) {
        return new QualifiedName(Optional.empty(), Optional.empty(), name);
    }

    public static QualifiedName of(String schema, String name) {
        return new QualifiedName(Optional.empty(), Optional.of(schema), name);
    }
    
    public static QualifiedName of(String database, String schema, String name) {
        return new QualifiedName(Optional.of(database), Optional.of(schema), name);
    }

    @Override
    public String toString() {
        return database.flatMap(db -> schema.map(s -> db + "." + s + "." + name)).orElse(schema.map(s -> s + "." + name).orElse(name));
    }
}
