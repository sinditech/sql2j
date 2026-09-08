/**
 * 
 */
package za.co.sindi.sql.sql2j.codegen.metamodel;

import java.util.Optional;

import za.co.sindi.sql.sql2j.ast.DataType;
import za.co.sindi.sql.sql2j.ast.Expression;

/**
 * @author Buhake Sindi
 * @since 17 August 2026
 */
public record Column(String name,
        DataType dataType,
        boolean nullable,
        boolean unique,
        boolean autoIncrement,
        Optional<Expression> defaultValue) {

	public Column withNullable(boolean newNullable) {
        return new Column(name, dataType, newNullable, unique, autoIncrement, defaultValue);
    }

    public Column withDataType(DataType newType) {
        return new Column(name, newType, nullable, unique, autoIncrement, defaultValue);
    }

    public Column withDefaultValue(Optional<Expression> newDefault) {
        return new Column(name, dataType, nullable, unique, autoIncrement, newDefault);
    }

    public Column withName(String newName) {
        return new Column(newName, dataType, nullable, unique, autoIncrement, defaultValue);
    }
    
    public Column withUnique(boolean unique) {
        return new Column(name, dataType, nullable, unique, autoIncrement, defaultValue);
    }
}
