/**
 * 
 */
package za.co.sindi.sql.sql2j.codegen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import za.co.sindi.sql.sql2j.ast.Expression;
import za.co.sindi.sql.sql2j.ast.QualifiedName;
import za.co.sindi.sql.sql2j.ast.TableConstraint;
import za.co.sindi.sql.sql2j.codegen.metamodel.Column;
import za.co.sindi.sql.sql2j.codegen.metamodel.Schema;
import za.co.sindi.sql.sql2j.codegen.metamodel.Table;
import za.co.sindi.sql.sql2j.util.ASTPrinter;

/**
 * @author Buhake Sindi
 * @since 18 August 2026
 */
public class JPAEntityCodeGenerator {
	
	private static final String INDENT = "\t"; // "    ";

	private final ASTPrinter exprPrinter = new ASTPrinter();
	private final JPAEntityGeneratorConfig config;
	private final SQLTypeConverter typeConverter;
	
	/**
	 * @param config
	 * @param typeConverter
	 */
	public JPAEntityCodeGenerator(JPAEntityGeneratorConfig config, SQLTypeConverter typeConverter) {
		super();
		this.config = config;
		this.typeConverter = typeConverter;
	}
	
	public List<GeneratedEntity> generateCode(final Schema schema) {
		List<GeneratedEntity> results = new ArrayList<>();
        for (Table table : schema.tables()) {
            results.addAll(generateForTable(table, schema));
        }
        return results;
	}
	
	// ------------------------------------------------------------ per table

    /** Everything about one generated relationship or scalar field, computed once and rendered by both the entity and (if needed) the ID class. */
    private record FieldPlan(
            String fieldName,
            JavaType javaType,
            boolean relationship,
            boolean primaryKey,
            boolean nullable,
            boolean unique,
            boolean autoIncrement,
            Optional<Expression> defaultValue,
            List<String> sourceColumns,
            List<String> referencedColumns,
            boolean unmappedType,
            JavaType idClassFieldType
    ) {}

    private List<GeneratedEntity> generateForTable(Table table, Schema schema) {
        String className = NameConverter.toEntityClassName(table.name().name());
        List<TableConstraint.ForeignKey> foreignKeys = table.constraints().stream()
                .filter(TableConstraint.ForeignKey.class::isInstance)
                .map(TableConstraint.ForeignKey.class::cast)
                .toList();

        List<FieldPlan> fields = planFields(table, foreignKeys, schema);
        boolean compositeKey = table.primaryKeyColumns().size() > 1;
        String idClassName = className + (config.useIdClassForCompositeKeys() ? "Id" : "Key");

        GeneratedEntity entity = renderEntity(table, className, fields, compositeKey, idClassName);
        if (compositeKey) {
        	GeneratedEntity idClass = renderIdClass(idClassName, fields);
        	return List.of(entity, idClass);
        }

        return List.of(entity);
    }

    /** Walks columns in declaration order, collapsing each FK's participating columns into one relationship field. */
    private List<FieldPlan> planFields(Table table, List<TableConstraint.ForeignKey> foreignKeys,
                                        Schema schema) {
        List<FieldPlan> plans = new ArrayList<>();
        Set<String> consumed = new HashSet<>();
        Set<TableConstraint.ForeignKey> processedFks = Collections.newSetFromMap(new IdentityHashMap<>());

        for (String columnName : table.columns().keySet()) {
            if (consumed.contains(columnName)) {
                continue;
            }
            Optional<TableConstraint.ForeignKey> owningFk = foreignKeys.stream()
                    .filter(fk -> !processedFks.contains(fk) && fk.columns().contains(columnName))
                    .findFirst();

            if (owningFk.isPresent()) {
                TableConstraint.ForeignKey fk = owningFk.get();
                processedFks.add(fk);
                consumed.addAll(fk.columns());
                plans.add(relationshipFieldPlan(table, fk, schema));
            } else {
                consumed.add(columnName);
                plans.add(scalarFieldPlan(table.columns().get(columnName), table));
            }
        }
        return plans;
    }

    private FieldPlan scalarFieldPlan(Column column, Table table) {
        JavaType javaType = typeConverter.toJavaType(column.dataType(), column.nullable());
        boolean unmapped = typeConverter.isUnmapped(column.dataType());
        boolean pk = table.primaryKeyColumns().contains(column.name());
        return new FieldPlan(
                NameConverter.toCamelCase(column.name()), javaType, false, pk,
                column.nullable(), column.unique(), column.autoIncrement(), column.defaultValue(),
                List.of(column.name()), List.of(), unmapped, javaType
        );
    }

    private FieldPlan relationshipFieldPlan(Table table, TableConstraint.ForeignKey fk,
                                             Schema schema) {
        String targetClassName = schema.findTable(fk.referencedTable())
                .map(t -> NameConverter.toEntityClassName(t.name().name()))
                .orElseGet(() -> NameConverter.toEntityClassName(fk.referencedTable().name()));

        String fieldName = fk.columns().size() == 1
                ? nonBlank(NameConverter.toCamelCase(NameConverter.stripIdSuffix(fk.columns().get(0))),
                            NameConverter.toCamelCase(targetClassName))
                : NameConverter.toCamelCase(targetClassName);

        boolean anyColumnNullable = fk.columns().stream()
                .anyMatch(c -> table.columns().containsKey(c) ? table.columns().get(c).nullable() : true);
        boolean allColumnsArePk = fk.columns().stream().allMatch(table.primaryKeyColumns()::contains);

        // Per JPA "derived identity" rules (spec 2.4.1.3): when a @ManyToOne field is also part of an
        // @IdClass-based composite key, the ID class's matching field must be typed as the *target*
        // entity's own primary-key type (e.g. Long) — NOT the target entity type itself.
        JavaType idClassFieldType = allColumnsArePk
                ? resolveDerivedIdJavaType(fk.referencedTable(), schema)
                : JavaType.of(config.packageName() + "." + targetClassName);

        return new FieldPlan(
                fieldName, JavaType.of(config.packageName() + "." + targetClassName), true, allColumnsArePk,
                anyColumnNullable, false, false, Optional.empty(),
                fk.columns(), fk.referencedColumns(), false, idClassFieldType
        );
    }

    /**
     * Resolves the Java type an {@code @IdClass} companion must use for a derived-identity field: the
     * referenced table's own primary-key type when it has exactly one PK column, the referenced table's
     * own {@code <Entity>Id} companion type when it has a composite PK of its own (nested derived
     * identity), or a conservative {@code Long} fallback if the referenced table wasn't resolvable at all
     * (e.g. it lives outside the parsed script).
     */
    private JavaType resolveDerivedIdJavaType(QualifiedName referencedTable, Schema schema) {
    	final JavaType defaultResolvedIdType = JavaType.of(Long.class);
        Optional<Table> target = schema.findTable(referencedTable);
        if (target.isEmpty()) {
            return defaultResolvedIdType;
        }
        Table rt = target.get();
        Set<String> pkColumns = rt.primaryKeyColumns();
        if (pkColumns.size() == 1) {
            Column pkColumn = rt.columns().get(pkColumns.iterator().next());
            return pkColumn != null ? typeConverter.toJavaType(pkColumn.dataType(), false) : defaultResolvedIdType;
        }
        if (pkColumns.size() > 1) {
            String targetClassName = NameConverter.toEntityClassName(rt.name().name()); //tableToClassName.get(Schema.key(rt.name()));
            return JavaType.of(config.packageName() + "." + targetClassName + "Id");
        }
        return defaultResolvedIdType;
    }

    private static String nonBlank(String preferred, String fallback) {
        return (preferred == null || preferred.isBlank()) ? fallback : preferred;
    }

    // -------------------------------------------------------------- render: entity

    private GeneratedEntity renderEntity(Table table, String className, List<FieldPlan> fields,
                                          boolean compositeKey, String idClassName) {
        TreeSet<String> imports = new TreeSet<>();
        imports.add(config.persistenceNamespace() + ".*");
        if (config.generateEqualsAndHashCode() && !table.primaryKeyColumns().isEmpty()) {
            imports.add("java.util.Objects");
        }
        for (FieldPlan f : fields) {
            f.javaType().qualifiedName().ifPresent(imports::add);
        }

        StringBuilder body = new StringBuilder();
        for (FieldPlan f : fields) {
            body.append(renderField(f)).append('\n');
        }

        body.append(INDENT).append("public ").append(className).append("() {\n");
        body.append(INDENT).append("}\n");

        for (FieldPlan f : fields) {
            body.append('\n').append(renderAccessors(f));
        }

        if (config.generateEqualsAndHashCode() && !table.primaryKeyColumns().isEmpty()) {
            body.append('\n').append(renderEqualsAndHashCode(className, fields));
        }

        StringBuilder source = new StringBuilder();
        source.append("package ").append(config.packageName()).append(";\n\n");
        for (String imp : imports) {
        	if (!imp.startsWith(config.packageName()))
        		source.append("import ").append(imp).append(";\n");
        }
        source.append('\n');

        List<TableConstraint.Check> checks = table.constraints().stream()
                .filter(TableConstraint.Check.class::isInstance).map(TableConstraint.Check.class::cast).toList();
        if (!checks.isEmpty()) {
            source.append("/**\n");
            source.append(" * Generated from SQL table {@code ").append(table.name()).append("}.\n");
            source.append(" * <p>Note: the following CHECK constraint(s) are documented here but not enforced by JPA:\n");
            for (TableConstraint.Check check : checks) {
                source.append(" * <ul><li>{@code ").append(exprPrinter.printExpression(check.expression())).append("}</li></ul>\n");
            }
            source.append(" */\n");
        }

        source.append("@Entity\n");
        source.append(renderTableAnnotation(table)).append('\n');
        if (compositeKey && config.useIdClassForCompositeKeys()) {
            source.append("@IdClass(").append(idClassName).append(".class)\n");
        }
        source.append("public class ").append(className).append(" {\n\n");
        source.append(body);
        source.append("}\n");

        return new GeneratedEntity(config.packageName(), className, source.toString());
    }
    
	private String renderTableAnnotation(Table table) {
        StringBuilder sb = new StringBuilder("@Table(name = \"").append(table.name().name()).append("\"");
        table.name().schema().ifPresent(s -> sb.append(", schema = \"").append(s).append("\""));

        List<TableConstraint.Unique> multiColumnUniques = table.constraints().stream()
                .filter(TableConstraint.Unique.class::isInstance).map(TableConstraint.Unique.class::cast)
                .filter(u -> u.columns().size() > 1)
                .toList();
        if (!multiColumnUniques.isEmpty()) {
            sb.append(",\n").append(INDENT).append(INDENT).append("uniqueConstraints = {\n");
            List<String> rendered = new ArrayList<>();
            for (TableConstraint.Unique u : multiColumnUniques) {
                String cols = u.columns().stream().collect(Collectors.joining("\", \"", "\"", "\""));
                rendered.add(INDENT.repeat(3) + "@UniqueConstraint(" + u.name().map(n -> "name = \"" + n + "\", ").orElse("")
                        + "columnNames = {" + cols + "})");
            }
            sb.append(String.join(",\n", rendered));
            sb.append('\n').append(INDENT).append(INDENT).append('}');
        }
        sb.append(')');
        return sb.toString();
    }

    private String renderField(FieldPlan f) {
        StringBuilder sb = new StringBuilder();
        if (f.unmappedType()) {
            sb.append(INDENT).append("// WARNING: unrecognized SQL type on column '")
                    .append(f.sourceColumns().get(0)).append("'; defaulted to String, verify manually\n");
        }
        if (f.primaryKey()) {
            sb.append(INDENT).append("@Id\n");
            if (!f.relationship() && f.autoIncrement()) {
                sb.append(INDENT).append("@GeneratedValue(strategy = GenerationType.IDENTITY)\n");
            }
        }
        if (f.relationship()) {
            sb.append(INDENT).append("@ManyToOne(fetch = FetchType.").append(config.toOneFetchType())
                    .append(f.nullable() ? "" : ", optional = false").append(")\n");
            sb.append(INDENT).append(renderJoinColumns(f)).append('\n');
        } else {
            sb.append(INDENT).append(renderColumnAnnotation(f)).append('\n');
        }
        sb.append(INDENT).append("private ").append(f.javaType().simpleName()).append(' ').append(f.fieldName()).append(";\n");
        return sb.toString();
    }

    private String renderJoinColumns(FieldPlan f) {
        if (f.sourceColumns().size() == 1) {
            return "@JoinColumn(" + joinColumnAttributes(f.sourceColumns().get(0), f.referencedColumns(), 0, f.nullable()) + ")";
        }
        StringBuilder sb = new StringBuilder("@JoinColumns({\n");
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < f.sourceColumns().size(); i++) {
            lines.add(INDENT.repeat(2) + "@JoinColumn(" + joinColumnAttributes(f.sourceColumns().get(i), f.referencedColumns(), i, f.nullable()) + ")");
        }
        sb.append(String.join(",\n", lines));
        sb.append('\n').append(INDENT).append("})");
        return sb.toString();
    }

    private String joinColumnAttributes(String column, List<String> referencedColumns, int index, boolean nullable) {
        StringBuilder sb = new StringBuilder("name = \"").append(column).append('"');
        if (index < referencedColumns.size()) {
            sb.append(", referencedColumnName = \"").append(referencedColumns.get(index)).append('"');
        }
        if (!nullable) {
            sb.append(", nullable = false");
        }
        return sb.toString();
    }

    private String renderColumnAnnotation(FieldPlan f) {
        StringBuilder sb = new StringBuilder("@Column(name = \"").append(f.sourceColumns().get(0)).append('"');
        if (!f.nullable()) {
            sb.append(", nullable = false");
        }
        if (f.unique()) {
            sb.append(", unique = true");
        }
        sb.append(')');
        return sb.toString();
    }

    private String renderAccessors(FieldPlan f) {
        String accessorSuffix = Character.toUpperCase(f.fieldName().charAt(0)) + f.fieldName().substring(1);
        String type = f.javaType().simpleName();
        StringBuilder sb = new StringBuilder();
        sb.append(INDENT).append("public ").append(type).append("boolean".equals(type) ? " is" : " get").append(accessorSuffix).append("() {\n");
        sb.append(INDENT).append(INDENT).append("return ").append(f.fieldName()).append(";\n");
        sb.append(INDENT).append("}\n\n");
        sb.append(INDENT).append("public void set").append(accessorSuffix).append("(").append(type).append(' ').append(f.fieldName()).append(") {\n");
        sb.append(INDENT).append(INDENT).append("this.").append(f.fieldName()).append(" = ").append(f.fieldName()).append(";\n");
        sb.append(INDENT).append("}\n");
        return sb.toString();
    }

    private String renderIdClassAccessor(FieldPlan f) {
        String accessorSuffix = Character.toUpperCase(f.fieldName().charAt(0)) + f.fieldName().substring(1);
        String type = f.idClassFieldType().simpleName();
        StringBuilder sb = new StringBuilder();
        sb.append(INDENT).append("public ").append(type).append(" get").append(accessorSuffix).append("() {\n");
        sb.append(INDENT).append(INDENT).append("return ").append(f.fieldName()).append(";\n");
        sb.append(INDENT).append("}\n\n");
        sb.append(INDENT).append("public void set").append(accessorSuffix).append("(").append(type).append(' ').append(f.fieldName()).append(") {\n");
        sb.append(INDENT).append(INDENT).append("this.").append(f.fieldName()).append(" = ").append(f.fieldName()).append(";\n");
        sb.append(INDENT).append("}\n");
        return sb.toString();
    }

    private String renderEqualsAndHashCode(String className, List<FieldPlan> fields) {
        List<FieldPlan> pkFields = fields.stream().filter(FieldPlan::primaryKey).toList();
        StringBuilder sb = new StringBuilder();
        sb.append(INDENT).append("@Override\n");
        sb.append(INDENT).append("public boolean equals(Object o) {\n");
        sb.append(INDENT).append(INDENT).append("if (this == o) return true;\n");
        sb.append(INDENT).append(INDENT).append("if (!(o instanceof ").append(className).append(" other)) return false;\n");
        String comparison = pkFields.stream()
                .map(f -> "Objects.equals(" + f.fieldName() + ", other." + f.fieldName() + ")")
                .collect(Collectors.joining(" && "));
        sb.append(INDENT).append(INDENT).append("return ").append(comparison).append(";\n");
        sb.append(INDENT).append("}\n\n");

        sb.append(INDENT).append("@Override\n");
        sb.append(INDENT).append("public int hashCode() {\n");
        sb.append(INDENT).append(INDENT).append("// Stable across Hibernate proxy (un)initialization, per the entity's persistent identity.\n");
        sb.append(INDENT).append(INDENT).append("return getClass().hashCode();\n");
        sb.append(INDENT).append("}\n");
        return sb.toString();
    }

    // ------------------------------------------------------------ render: id class

    private GeneratedEntity renderIdClass(String idClassName, List<FieldPlan> fields) {
        List<FieldPlan> pkFields = fields.stream().filter(FieldPlan::primaryKey).toList();

        TreeSet<String> imports = new TreeSet<>();
        imports.add("java.io.Serializable");
        imports.add("java.util.Objects");
        for (FieldPlan f : pkFields) {
            f.idClassFieldType().qualifiedName().ifPresent(imports::add);
        }

        StringBuilder body = new StringBuilder();
        body.append(INDENT).append("private static final long serialVersionUID = 1L;\n\n");
        for (FieldPlan f : pkFields) {
            body.append(INDENT).append("private ").append(f.idClassFieldType().simpleName()).append(' ').append(f.fieldName()).append(";\n");
        }
        body.append('\n');

        body.append(INDENT).append("public ").append(idClassName).append("() {\n").append(INDENT).append("}\n\n");

        String args = pkFields.stream().map(f -> f.idClassFieldType().simpleName() + " " + f.fieldName()).collect(Collectors.joining(", "));
        body.append(INDENT).append("public ").append(idClassName).append('(').append(args).append(") {\n");
        for (FieldPlan f : pkFields) {
            body.append(INDENT).append(INDENT).append("this.").append(f.fieldName()).append(" = ").append(f.fieldName()).append(";\n");
        }
        body.append(INDENT).append("}\n");

        for (FieldPlan f : pkFields) {
            body.append('\n').append(renderIdClassAccessor(f));
        }

        body.append('\n').append(INDENT).append("@Override\n");
        body.append(INDENT).append("public boolean equals(Object o) {\n");
        body.append(INDENT).append(INDENT).append("if (this == o) return true;\n");
        body.append(INDENT).append(INDENT).append("if (!(o instanceof ").append(idClassName).append(" other)) return false;\n");
        String comparison = pkFields.stream()
                .map(f -> "Objects.equals(" + f.fieldName() + ", other." + f.fieldName() + ")")
                .collect(Collectors.joining(" && "));
        body.append(INDENT).append(INDENT).append("return ").append(comparison).append(";\n");
        body.append(INDENT).append("}\n\n");

        body.append(INDENT).append("@Override\n");
        body.append(INDENT).append("public int hashCode() {\n");
        String hashArgs = pkFields.stream().map(FieldPlan::fieldName).collect(Collectors.joining(", "));
        body.append(INDENT).append(INDENT).append("return Objects.hash(").append(hashArgs).append(");\n");
        body.append(INDENT).append("}\n");

        StringBuilder source = new StringBuilder();
        source.append("package ").append(config.packageName()).append(";\n\n");
        for (String imp : imports) {
            source.append("import ").append(imp).append(";\n");
        }
        source.append('\n');
        source.append("/** Composite-key companion for {@link ").append(idClassName, 0, idClassName.length() - 2).append("}, used with {@code @IdClass}. */\n");
        source.append("public class ").append(idClassName).append(" implements Serializable {\n\n");
        source.append(body);
        source.append("}\n");

        return new GeneratedEntity(config.packageName(), idClassName, source.toString());
    }
}
