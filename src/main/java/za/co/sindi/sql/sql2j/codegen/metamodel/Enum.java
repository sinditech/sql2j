/**
 * 
 */
package za.co.sindi.sql.sql2j.codegen.metamodel;

import java.util.List;

import za.co.sindi.sql.sql2j.ast.QualifiedName;

/**
 * @author Buhake Sindi
 * @since 17 August 2026
 */
public record Enum(QualifiedName name,
        List<String> labels) {

    public Enum withLabels(List<String> labels) {
        return new Enum(name, labels);
    }

    public Enum withName(QualifiedName newName) {
        return new Enum(newName, List.of());
    }
}
