/**
 * 
 */
package za.co.sindi.sql.sql2j.codegen.metamodel;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author Buhake Sindi
 * @since 17 August 2026
 */
public final class Schemas {

	private static final String DEFAULT_SCHEMA_KEY = "#default";
	private static Map<String, Schema> schemas = new LinkedHashMap<>();
	
	private Schemas() {
		throw new AssertionError("Private constructor.");
	}
	
	public static Schema getOrCreate(final String name) {
		String lowercaseName = Objects.requireNonNull(name, "A schema name is required.").trim().toLowerCase();
		if (!schemas.containsKey(lowercaseName)) schemas.put(lowercaseName, DEFAULT_SCHEMA_KEY.equals(name) ? new Schema() : new Schema(name.trim()));
		return schemas.get(lowercaseName);
	}
	
	public static Schema getDefault() {
		return getOrCreate(DEFAULT_SCHEMA_KEY);
	}
	
	public static Set<String> names() {
		return schemas.keySet();
	}
}
