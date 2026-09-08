/**
 * 
 */
package za.co.sindi.sql.sql2j.codegen.metamodel;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import za.co.sindi.sql.sql2j.ast.QualifiedName;

/**
 * @author Buhake Sindi
 * @since 17 August 2026
 */
public final class Schema {

	private final Optional<String> name;
	private final Map<String, Table> tables = new LinkedHashMap<>();
	private final Map<String, Enum> enums = new LinkedHashMap<>();
	
	/**
	 * 
	 */
	public Schema() {
		super();
		this.name = Optional.empty();
	}
	
	/**
	 * @param name
	 */
	public Schema(String name) {
		super();
		this.name = Optional.of(name);
	}
	
	public Optional<String> name() {
		return name;
	}

	public Collection<Table> tables() {
		return tables.values();
	}
	
	public void addTable(final Table table) {
		tables.put(key(table.name()), table);
	}
	 
	public Optional<Table> findTable(QualifiedName name) {
		return Optional.ofNullable(tables.get(key(name)));
	}
	
	public void removeTable(QualifiedName name) {
		tables.remove(key(name));
	}
	
	public Collection<Enum> enums() {
		return enums.values();
	}
	
	public void addEnum(final Enum _enum) {
		enums.put(key(_enum.name()), _enum);
	}
	 
	public Optional<Enum> findEnum(QualifiedName name) {
		return Optional.ofNullable(enums.get(key(name)));
	}
	
	public void removeEnum(QualifiedName name) {
		enums.remove(key(name));
	}

    static String key(QualifiedName name) {
        // Case-insensitive, schema-qualified lookup key.
        return name.toString().toLowerCase();
    }
}
