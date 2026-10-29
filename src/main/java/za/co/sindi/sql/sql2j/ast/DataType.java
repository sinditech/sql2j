package za.co.sindi.sql.sql2j.ast;

import java.util.List;

/**
 * A column data type, e.g. {@code VARCHAR(255)}, {@code DECIMAL(10,2)},
 * {@code INT}, or {@code TIMESTAMP WITH TIME ZONE}.
 *
 * @param name       the base type name, e.g. {@code VARCHAR}
 * @param parameters numeric parameters such as length / precision / scale
 * @param modifiers  trailing keyword modifiers, e.g. {@code ["WITH", "TIME", "ZONE"]}
 * @param labelValues MySQL/MariaDB inline {@code ENUM(...)}/{@code SET(...)} string-literal
 *                    values, in declaration order; empty for every other type. A direct,
 *                    unnamed-type counterpart to PostgreSQL's {@code CREATE TYPE ... AS ENUM}
 *                    — see {@code JpaEntityGenerator} for how both feed Java enum generation.
 */
public record DataType(QualifiedName name, List<Integer> parameters, List<String> modifiers, List<String> labelValues) {
	
	/** Sentinel stored in {@code parameters} for SQL Server's {@code VARCHAR(MAX)}/{@code NVARCHAR(MAX)}/{@code VARBINARY(MAX)}. */
    public static final int MAX = Integer.MAX_VALUE;

    public static DataType simple(String name) {
        return new DataType(QualifiedName.of(name), List.of(), List.of(), List.of());
    }

    /** {@code true} for MySQL/MariaDB's {@code ENUM(...)} or {@code SET(...)}. */
    public boolean isInlineEnumOrSet() {
        return !labelValues.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(name.toString());
        if (!parameters.isEmpty()) {
            sb.append('(');
            for (int i = 0; i < parameters.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(parameters.get(i) == MAX ? "MAX" : String.valueOf(parameters.get(i)));
            }
            sb.append(')');
        }
        if (!labelValues.isEmpty()) {
            sb.append('(');
            for (int i = 0; i < labelValues.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append('\'').append(labelValues.get(i).replace("'", "''")).append('\'');
            }
            sb.append(')');
        }
        if (!modifiers.isEmpty()) {
            sb.append(' ').append(String.join(" ", modifiers));
        }
        return sb.toString();
    }
}
