/**
 * 
 */
package za.co.sindi.sql.sql2j.codegen.metamodel;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import za.co.sindi.sql.sql2j.ast.DataType;
import za.co.sindi.sql.sql2j.ast.QualifiedName;

/**
 * @author Buhake Sindi
 * @since 17 August 2026
 */
public final class Record {

	private QualifiedName name;
    private final Map<String, Attribute> attributes = new LinkedHashMap<>();
    
	/**
	 * @param name
	 */
	public Record(QualifiedName name) {
		super();
		this.name = name;
	}

    public QualifiedName name() {
        return name;
    }

    public void rename(QualifiedName newName) {
        this.name = newName;
    }

    /** Columns in declaration order (later {@code ADD COLUMN}s appended at the end). */
    public Map<String, Attribute> attributes() {
        return attributes;
    }

    public void addAttribute(Attribute attribute) {
    	attributes.put(attribute.name(), attribute);
    }

    public void removeAttribute(String name) {
    	attributes.remove(name);
    }

    /** Renames a column while preserving its position in the (ordered) column map. */
    public void renameAttribute(String from, String to) {
        LinkedHashMap<String, Attribute> rebuilt = new LinkedHashMap<>();
        for (Map.Entry<String, Attribute> entry : attributes.entrySet()) {
            if (entry.getKey().equals(from)) {
                rebuilt.put(to, entry.getValue().withName(to));
            } else {
                rebuilt.put(entry.getKey(), entry.getValue());
            }
        }
        attributes.clear();
        attributes.putAll(rebuilt);
    }

    public void replaceAttribute(String name, Attribute updated) {
        if (attributes.containsKey(name)) {
        	attributes.put(name, updated);
        }
    }
    
    public static record Attribute(String name,
            DataType dataType,
            Optional<String> collation) {

        public Attribute withDataType(DataType newType) {
            return new Attribute(name, newType, collation);
        }

        public Attribute withName(String newName) {
            return new Attribute(newName, dataType, collation);
        }
    }
}
