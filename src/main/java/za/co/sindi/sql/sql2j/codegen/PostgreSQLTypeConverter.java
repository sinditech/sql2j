/**
 * 
 */
package za.co.sindi.sql.sql2j.codegen;

import za.co.sindi.sql.sql2j.ast.DataType;

/**
 * @author Buhake Sindi
 * @since 18 August 2026
 */
public class PostgreSQLTypeConverter extends SQLTypeConverter {

	@Override
	public JavaType toJavaType(DataType dataType, boolean nullable) {
		// TODO Auto-generated method stub
		String name = dataType.name().name().toUpperCase();
        return switch (name) {
            case "BIGINT", "BIGSERIAL", "SERIAL8", "INT8" -> torimitive(java.lang.Long.class, nullable);
            case "INT", "INTEGER", "SERIAL", "SERIAL4", "INT4" -> torimitive(java.lang.Integer.class, nullable);
            case "SMALLINT", "SMALLSERIAL", "SERIAL2", "INT2" -> torimitive(java.lang.Short.class, nullable);
            case "TINYINT" -> torimitive(java.lang.Byte.class, nullable);
            case "DECIMAL", "NUMERIC", "NUMBER" -> JavaType.of(java.math.BigDecimal.class);
            case "FLOAT", "REAL", "FLOAT4" -> torimitive(java.lang.Float.class, nullable);
            case "DOUBLE", "FLOAT8" -> torimitive(java.lang.Double.class, nullable);
            case "BOOLEAN", "BOOL", "BIT" -> torimitive(java.lang.Boolean.class, nullable);
            case "CHAR", "CHARACTER" -> (dataType.parameters().size() == 1 && dataType.parameters().get(0) == 1) ? torimitive(java.lang.Character.class, nullable) : torimitive(java.lang.String.class, nullable);
            case "VARCHAR", "TEXT", "CLOB", "NVARCHAR", "NCHAR", "LONGTEXT", "MEDIUMTEXT" ->
                    JavaType.of(java.lang.String.class);
            case "DATE" -> JavaType.of(java.time.LocalDate.class);
            case "TIME" -> JavaType.of(java.time.LocalTime.class);
            case "TIMESTAMP", "DATETIME", "TIMESTAMPTZ" -> dataType.modifiers().contains("WITH")
                    ? JavaType.of(java.time.OffsetDateTime.class)
                    : JavaType.of(java.time.LocalDateTime.class);
            case "UUID" -> JavaType.of(java.util.UUID.class);
            case "BLOB", "BYTEA", "VARBINARY", "BINARY", "LONGBLOB" -> JavaType.of(byte[].class);
            case "JSON", "JSONB" -> JavaType.of(java.lang.String.class); // no standard JPA JSON type without a converter
            default -> JavaType.of(java.lang.String.class); // unknown type: safest fallback, flagged with a comment by the caller
        };
	}

	@Override
	public boolean isUnmapped(DataType dataType) {
		// TODO Auto-generated method stub
		return switch (dataType.name().name().toUpperCase()) {
            case "BIGINT", "BIGSERIAL", "SERIAL8", "INT8", "INT", "INTEGER", "SERIAL", "SERIAL4", "INT4",
                    "SMALLINT", "SMALLSERIAL", "SERIAL2", "INT2", "TINYINT", "DECIMAL", "NUMERIC", "NUMBER",
                    "FLOAT", "REAL", "FLOAT4", "DOUBLE", "FLOAT8", "BOOLEAN", "BOOL", "BIT",
                    "VARCHAR", "CHAR", "CHARACTER", "TEXT", "CLOB", "NVARCHAR", "NCHAR", "LONGTEXT", "MEDIUMTEXT",
                    "DATE", "TIME", "TIMESTAMP", "DATETIME", "TIMESTAMPTZ", "UUID",
                    "BLOB", "BYTEA", "VARBINARY", "BINARY", "LONGBLOB", "JSON", "JSONB" -> false;
            default -> true;
        };
	}
}
