package za.co.sindi.sql.sql2j.codegen;

import java.util.Set;

/**
 * Converts SQL {@code snake_case} identifiers to Java naming conventions.
 * Stateless; every method is a pure function, so this is a plain final
 * utility class rather than something instantiated per use.
 * 
 * @author Buhake Sindi
 * @since 19 August 2026
 */
public final class NameConverter {

    /** Words that collide with Java syntax; suffixed with an underscore if used verbatim as an identifier. */
    private static final Set<String> JAVA_RESERVED = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
            "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
            "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
            "volatile", "while", "var", "record", "yield"
    );

    private NameConverter() {}

    /**
     * {@code order_items} -&gt; {@code OrderItems}. Table/entity class-name case.
     * <p>
     * Also safe to call on already-{@code camelCase}/{@code PascalCase} input
     * (e.g. a generated Java method name), not just {@code snake_case} SQL
     * identifiers: a boundary is inserted wherever a lowercase letter is
     * immediately followed by an uppercase one, before splitting, so
     * {@code "reconcileInventory"} still becomes {@code "ReconcileInventory"}
     * rather than losing its internal capitalization.
     */
    public static String toPascalCase(String input) {
        String normalized = input.replaceAll("(?<=[a-z0-9])(?=[A-Z])", "_");
        StringBuilder sb = new StringBuilder();
        for (String word : normalized.split("[_\\s]+")) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }

    /** {@code created_at} -&gt; {@code createdAt}. Field/getter-setter case. */
    public static String toCamelCase(String snakeCase) {
        String pascal = toPascalCase(snakeCase);
        String camel = pascal.isEmpty() ? pascal : Character.toLowerCase(pascal.charAt(0)) + pascal.substring(1);
        return JAVA_RESERVED.contains(camel) ? camel + "_" : camel;
    }

    /**
     * Naive English singularization for turning a table name into an entity
     * class name, e.g. {@code customers -> Customer}, {@code categories ->
     * Category}, {@code addresses -> Address}. Best-effort by design (real
     * singularization is a whole linguistics problem); irregular plurals
     * (e.g. {@code people}, {@code children}) are left as-is.
     */
    public static String singularize(String word) {
        String lower = word.toLowerCase();
        if (lower.endsWith("ies") && lower.length() > 3) {
            return word.substring(0, word.length() - 3) + "y";
        }
        if (lower.endsWith("ses") || lower.endsWith("xes") || lower.endsWith("ches") || lower.endsWith("shes")) {
            return word.substring(0, word.length() - 2);
        }
        if (lower.endsWith("s") && !lower.endsWith("ss") && word.length() > 1) {
            return word.substring(0, word.length() - 1);
        }
        return word;
    }

    /** Table name -&gt; entity class name: singularize, then PascalCase. */
    public static String toEntityClassName(String tableName) {
        return toPascalCase(singularize(tableName));
    }

    /** Strips a trailing {@code _id}/{@code Id} suffix used for FK column -&gt; relationship field naming. */
    public static String stripIdSuffix(String columnName) {
        String lower = columnName.toLowerCase();
        if (lower.endsWith("_id") && columnName.length() > 3) {
            return columnName.substring(0, columnName.length() - 3);
        }
        if (lower.endsWith("id") && columnName.length() > 2) {
            return columnName.substring(0, columnName.length() - 2);
        }
        return columnName;
    }
}
