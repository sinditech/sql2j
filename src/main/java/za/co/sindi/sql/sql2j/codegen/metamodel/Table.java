/**
 * 
 */
package za.co.sindi.sql.sql2j.codegen.metamodel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import za.co.sindi.sql.sql2j.ast.QualifiedName;
import za.co.sindi.sql.sql2j.ast.TableConstraint;

/**
 * @author Buhake Sindi
 * @since 17 August 2026
 */
public final class Table {

	private QualifiedName name;
    private final Map<String, Column> columns = new LinkedHashMap<>();
    private final Set<String> primaryKeyColumns = new LinkedHashSet<>();
    private final List<TableConstraint> constraints = new ArrayList<>();
    
	/**
	 * @param name
	 */
	public Table(QualifiedName name) {
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
    public Map<String, Column> columns() {
        return columns;
    }

    public void addColumn(Column column) {
        columns.put(column.name(), column);
    }

    public void removeColumn(String name) {
        columns.remove(name);
        primaryKeyColumns.remove(name);
    }

    /** Renames a column while preserving its position in the (ordered) column map. */
    public void renameColumn(String from, String to) {
        LinkedHashMap<String, Column> rebuilt = new LinkedHashMap<>();
        for (Map.Entry<String, Column> entry : columns.entrySet()) {
            if (entry.getKey().equals(from)) {
                rebuilt.put(to, entry.getValue().withName(to));
            } else {
                rebuilt.put(entry.getKey(), entry.getValue());
            }
        }
        columns.clear();
        columns.putAll(rebuilt);
        if (primaryKeyColumns.remove(from)) {
            primaryKeyColumns.add(to);
        }
    }

    public void replaceColumn(String name, Column updated) {
        if (columns.containsKey(name)) {
            columns.put(name, updated);
        }
    }

    public Set<String> primaryKeyColumns() {
        return primaryKeyColumns;
    }

    public void addPrimaryKeyColumn(String column) {
        primaryKeyColumns.add(column);
    }

    /** Table-level constraints (PK/UNIQUE/FOREIGN KEY/CHECK), including ones synthesized from column-level clauses. */
    public List<TableConstraint> constraints() {
        return constraints;
    }

    public void addConstraint(TableConstraint constraint) {
        constraints.add(constraint);
    }

    public void removeConstraintByName(String constraintName) {
        constraints.removeIf(c -> c.name().map(constraintName::equals).orElse(false));
    }
}
