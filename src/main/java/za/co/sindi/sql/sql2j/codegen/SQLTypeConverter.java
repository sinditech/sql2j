/**
 * 
 */
package za.co.sindi.sql.sql2j.codegen;

import za.co.sindi.sql.sql2j.ast.DataType;

/**
 * @author Buhake Sindi
 * @since 18 August 2026
 */
public abstract class SQLTypeConverter {
	
//	protected JavaType torimitive(final String fullyQualifiedClassName, boolean nullable) {
//		return switch (fullyQualifiedClassName) {
//			case String s when s.equals(Character.class.getCanonicalName()) && !nullable -> new JavaType(char.class.getCanonicalName());
//			case String s when s.equals(Boolean.class.getCanonicalName()) && !nullable -> new JavaType(boolean.class.getCanonicalName());
//			case String s when s.equals(Byte.class.getCanonicalName()) && !nullable -> new JavaType(byte.class.getCanonicalName());
//			case String s when s.equals(Short.class.getCanonicalName()) && !nullable -> new JavaType(short.class.getCanonicalName());
//			case String s when s.equals(Integer.class.getCanonicalName()) && !nullable -> new JavaType(int.class.getCanonicalName());
//			case String s when s.equals(Long.class.getCanonicalName()) && !nullable -> new JavaType(long.class.getCanonicalName());
//			case String s when s.equals(Double.class.getCanonicalName()) && !nullable -> new JavaType(double.class.getCanonicalName());
//			case String s when s.equals(Float.class.getCanonicalName()) && !nullable -> new JavaType(float.class.getCanonicalName());
//			
//			default -> new JavaType(fullyQualifiedClassName);
//		};
//	}
	
	protected JavaType torimitive(final Class<?> type, boolean nullable) {
		return switch (type) {
			case Class<?> c when c == Character.class && !nullable -> JavaType.of(char.class);
			case Class<?> c when c == Boolean.class && !nullable -> JavaType.of(boolean.class);
			case Class<?> c when c == Byte.class && !nullable -> JavaType.of(byte.class);
			case Class<?> c when c == Short.class && !nullable -> JavaType.of(short.class);
			case Class<?> c when c == Integer.class && !nullable -> JavaType.of(int.class);
			case Class<?> c when c == Long.class && !nullable -> JavaType.of(long.class);
			case Class<?> c when c == Double.class && !nullable -> JavaType.of(double.class);
			case Class<?> c when c == Float.class && !nullable -> JavaType.of(float.class);
			
			default -> JavaType.of(type);
		};
	}

	public abstract JavaType toJavaType(final DataType dataType, boolean nullable);
	public abstract boolean isUnmapped(DataType dataType);
}
