package za.co.sindi.sql.sql2j.ast;

import java.util.List;

/**
 * A column data type, e.g. {@code VARCHAR(255)}, {@code DECIMAL(10,2)},
 * {@code INT}, or {@code TIMESTAMP WITH TIME ZONE}.
 *
 * @param name       the base type name, e.g. {@code VARCHAR}
 * @param parameters numeric parameters such as length / precision / scale
 * @param modifiers  trailing keyword modifiers, e.g. {@code ["WITH", "TIME", "ZONE"]}
 */
public record DataType(QualifiedName name, List<Integer> parameters, List<String> modifiers) {

    public static DataType simple(String name) {
        return new DataType(QualifiedName.of(name), List.of(), List.of());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(name.toString());
        if (!parameters.isEmpty()) {
            sb.append('(');
            for (int i = 0; i < parameters.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(parameters.get(i));
            }
            sb.append(')');
        }
        if (!modifiers.isEmpty()) {
            sb.append(' ').append(String.join(" ", modifiers));
        }
        return sb.toString();
    }
}
