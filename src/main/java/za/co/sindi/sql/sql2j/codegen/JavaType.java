/**
 * 
 */
package za.co.sindi.sql.sql2j.codegen;

import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

/**
 * A Java type a SQL column can be mapped to.
 *
 * @param simpleName    the type's simple name as it should appear in generated code, e.g. {@code BigDecimal}
 * @param qualifiedName the fully-qualified name to import, empty for {@code java.lang} types that need no import
 * 
 * @author Buhake Sindi
 * @since 18 August 2026
 */
public record JavaType(String simpleName, Optional<String> qualifiedName) {
	
	public static JavaType of(final String fqClassName) {
		String simple = fqClassName.substring(fqClassName.lastIndexOf('.') + 1);
		if (fqClassName.startsWith("java.lang")) return new JavaType(simple, Optional.empty());
        return new JavaType(simple, Optional.of(fqClassName));
	}
	
	public static JavaType of(final Class<?> type) {
		Objects.requireNonNull(type, "A Java class type is required.");
		String qualifiedName = type.getCanonicalName();
		String simpleName = qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
		if (type.isPrimitive()) return new JavaType(simpleName, Optional.empty());
		if (type.isArray() && type.getComponentType().isPrimitive()) return new JavaType(simpleName, Optional.empty());
		if ("java.lang".equals(type.getPackageName())) return new JavaType(simpleName, Optional.empty());
		if (type.isArray()) return new JavaType(simpleName, Optional.of(type.getComponentType().getCanonicalName()));
		return new JavaType(simpleName, Optional.of(qualifiedName));
	}
	
	public static void main(String[] args) {
		System.out.println(String.class.getPackageName());
		System.out.println(String.class.getName());
		System.out.println(String.class.getCanonicalName());
		System.out.println();
		System.out.println(String[].class.getPackageName());
		System.out.println(String[].class.getName());
		System.out.println(String[].class.getCanonicalName());
		System.out.println(String[].class.getComponentType().getCanonicalName());
		System.out.println();
		System.out.println(Entry.class.getPackageName());
		System.out.println(Entry.class.getCanonicalName());
	}
}
