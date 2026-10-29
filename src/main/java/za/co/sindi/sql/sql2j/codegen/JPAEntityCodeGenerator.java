/**
 * 
 */
package za.co.sindi.sql.sql2j.codegen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import za.co.sindi.sql.sql2j.ast.Expression;
import za.co.sindi.sql.sql2j.ast.QualifiedName;
import za.co.sindi.sql.sql2j.ast.TableConstraint;
import za.co.sindi.sql.sql2j.codegen.metamodel.Enum;
import za.co.sindi.sql.sql2j.codegen.metamodel.Record;
import za.co.sindi.sql.sql2j.codegen.metamodel.Record.Attribute;
import za.co.sindi.sql.sql2j.codegen.metamodel.Schema;
import za.co.sindi.sql.sql2j.codegen.metamodel.Table;
import za.co.sindi.sql.sql2j.codegen.metamodel.Table.Column;
import za.co.sindi.sql.sql2j.util.ASTPrinter;

/**
 * @author Buhake Sindi
 * @since 18 August 2026
 */
public class JPAEntityCodeGenerator {
	
	private static final String INDENT = "    "; // "\t";

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
		Map<String, String> tableToClassName = new HashMap<>();
        for (Table table : schema.tables()) {
            tableToClassName.put(table.name().toString().toLowerCase(), NameConverter.toEntityClassName(table.name().name()));
        }

        GenerationContext ctx = new GenerationContext(schema, tableToClassName);
		List<GeneratedEntity> results = new ArrayList<>();
        for (Table table : schema.tables()) {
            results.addAll(generateForTable(table, ctx));
        }
        results.addAll(0, ctx.sharedCompanionsInOrder);
        return results;
	}
	/** Per-{@code generate()}-call state: the class-name map, plus dedup caches so a shared named type (an enum or composite type used by several columns/tables) is only generated once. */
    private final class GenerationContext {
        final Schema schema;
        final Map<String, String> tableToClassName;
        final Map<String, String> generatedSharedTypeClassNames = new HashMap<>(); // "type:schema.name" -> class name, for dedup
        final List<GeneratedEntity> sharedCompanionsInOrder = new ArrayList<>();
        final Set<String> emittedClassNames = new HashSet<>();

        GenerationContext(Schema schema, Map<String, String> tableToClassName) {
            this.schema = schema;
            this.tableToClassName = tableToClassName;
        }
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
            JavaType idClassFieldType,
            Optional<String> convertedJavaType,   // overrides javaType.simpleName() on the field itself: an enum or composite-type class name
            Optional<String> converterClassName,   // the @Convert(converter = ...) target, when convertedJavaType is present
            Optional<String> enumDefaultConstant  // enum-mapped column whose SQL DEFAULT is one of its labels: the Java constant name
    ) {}

    private List<GeneratedEntity> generateForTable(Table table, GenerationContext ctx) {
//        String className = NameConverter.toEntityClassName(table.name().name());
    	String className = ctx.tableToClassName.get(table.name().toString().toLowerCase());
        List<TableConstraint.ForeignKey> foreignKeys = table.constraints().stream()
                .filter(TableConstraint.ForeignKey.class::isInstance)
                .map(TableConstraint.ForeignKey.class::cast)
                .toList();

        List<GeneratedEntity> companions = new ArrayList<>();
        List<FieldPlan> fields = planFields(table, foreignKeys, ctx, companions);
        boolean compositeKey = table.primaryKeyColumns().size() > 1;
        String idClassName = className + (config.compositeKeyStrategy() == CompositeKeyStrategy.ID_CLASS ? "Id" : "Key");

        List<GeneratedEntity> results = new ArrayList<>();
        if (!compositeKey) {
            results.add(renderEntity(table, className, fields, false, idClassName));
        } else if (config.compositeKeyStrategy() == CompositeKeyStrategy.EMBEDDED_ID) {
            results.add(renderEntityWithEmbeddedId(table, className, fields, idClassName));
            results.add(renderIdClass(idClassName, fields, true));
        } else {
            results.add(renderEntity(table, className, fields, true, idClassName));
            results.add(renderIdClass(idClassName, fields, false));
        }
        results.addAll(companions);
        return results;
    }

    /** Walks columns in declaration order, collapsing each FK's participating columns into one relationship field. */
    private List<FieldPlan> planFields(Table table, List<TableConstraint.ForeignKey> foreignKeys, GenerationContext ctx, List<GeneratedEntity> companions) {
        List<FieldPlan> plans = new ArrayList<>();
        Set<String> consumed = new HashSet<>();
        Set<TableConstraint.ForeignKey> processedFks = Collections.newSetFromMap(new IdentityHashMap<>());
        String entityClassName = ctx.tableToClassName.get(table.name().toString().toLowerCase());

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
                plans.add(relationshipFieldPlan(table, fk, ctx));
            } else {
                consumed.add(columnName);
                plans.add(scalarFieldPlan(table.columns().get(columnName), table, entityClassName, ctx, companions));
            }
        }
        return plans;
    }

    private FieldPlan scalarFieldPlan(Column column, Table table, String entityClassName, GenerationContext ctx, List<GeneratedEntity> companions) {
        JavaType javaType = typeConverter.toJavaType(column.dataType(), column.nullable());
        boolean unmapped = typeConverter.isUnmapped(column.dataType());
        boolean pk = table.primaryKeyColumns().contains(column.name());
        String fieldName = NameConverter.toCamelCase(column.name());

        Optional<String> convertedType = Optional.empty();
        Optional<String> converterClass = Optional.empty();
        Optional<String> enumDefaultConstant = Optional.empty();

        Optional<EnumSource> enumSource = detectEnumSource(table, column, ctx.schema);
        if (enumSource.isPresent()) {
            String enumClassName = resolveSharedOrPerColumnClassName(enumSource.get(), entityClassName, fieldName, ctx);
            ensureEnumGenerated(enumSource.get(), enumClassName, ctx, companions);
            convertedType = Optional.of(enumClassName);
            converterClass = Optional.of(enumClassName + "Converter");
            enumDefaultConstant = enumDefaultConstantFor(column, enumSource.get().labels());
        } else {
            Optional<Record> compositeType = detectRecord(table, column, ctx.schema);
            if (compositeType.isPresent()) {
                String valueClassName = resolveNamedTypeClassName(compositeType.get().name(), ctx); //resolveNamedTypeClassName(compositeType.get(), ctx);
                ensureRecordGenerated(compositeType.get(), valueClassName, ctx, companions); //ensureCompositeTypeGenerated(compositeType.get(), valueClassName, ctx, companions);
                convertedType = Optional.of(valueClassName);
                converterClass = Optional.of(valueClassName + "Converter");
            }
        }
                
        return new FieldPlan(
                fieldName, javaType, false, pk,
                column.nullable(), column.unique(), column.autoIncrement(), column.defaultValue(),
                List.of(column.name()), List.of(), unmapped, javaType, convertedType, converterClass, enumDefaultConstant
        );
    }

    private FieldPlan relationshipFieldPlan(Table table, TableConstraint.ForeignKey fk, GenerationContext ctx) {
        String targetClassName = ctx.schema.findTable(fk.referencedTable())
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
                ? resolveDerivedIdJavaType(fk.referencedTable(), ctx)
                : JavaType.of(config.packageName() + "." + targetClassName);

        return new FieldPlan(
                fieldName, JavaType.of(config.packageName() + "." + targetClassName), true, allColumnsArePk,
                anyColumnNullable, false, false, Optional.empty(),
                fk.columns(), fk.referencedColumns(), false, idClassFieldType, Optional.empty(), Optional.empty(), Optional.empty()
        );
    }

    /**
     * Resolves the Java type an {@code @IdClass} companion must use for a derived-identity field: the
     * referenced table's own primary-key type when it has exactly one PK column, the referenced table's
     * own {@code <Entity>Id} companion type when it has a composite PK of its own (nested derived
     * identity), or a conservative {@code Long} fallback if the referenced table wasn't resolvable at all
     * (e.g. it lives outside the parsed script).
     */
    private JavaType resolveDerivedIdJavaType(QualifiedName referencedTable, GenerationContext ctx) {
    	final JavaType defaultResolvedIdType = JavaType.of(Long.class);
        Optional<Table> target = ctx.schema.findTable(referencedTable);
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
    
 // ---------------------------------------------------------- enum detection

    /** One detected source of enum labels for a column, with a stable cache key so a named type is only generated once. */
    private record EnumSource(String cacheKey, Optional<String> sharedClassNameHint, List<String> labels) {}

    private Optional<EnumSource> detectEnumSource(Table table, Column column, Schema schema) {
//        Optional<CreateTypeStatement> namedType = schema.findType(column.dataType().name(), table.name().schema());
//        if (namedType.isPresent() && namedType.get().definition() instanceof TypeDefinition.Enum(var labels)) {
//            CreateTypeStatement typeStatement = namedType.get();
//            String key = "type:" + Schema.key(typeStatement.typeName());
//            String classNameHint = NameConverter.toEntityClassName(typeStatement.typeName().name());
//            return Optional.of(new EnumSource(key, Optional.of(classNameHint), labels));
//        }
    	Optional<Enum> enumModel = schema.findEnum(column.dataType().name());
    	if (enumModel.isPresent()) {
    		Enum _enum = enumModel.get();
    		String key = "type:" + Schema.key(column.dataType().name());
    		String classNameHint = NameConverter.toEntityClassName(column.dataType().name().name());
    		return Optional.of(new EnumSource(key, Optional.of(classNameHint), _enum.labels()));
    	}
        if (column.dataType().isInlineEnumOrSet()) {
            String key = "column:" + Schema.key(table.name()) + "." + column.name().toLowerCase();
            return Optional.of(new EnumSource(key, Optional.empty(), column.dataType().labelValues()));
        }
        List<String> checkLabels = findCheckDerivedEnumLabels(table, column.name());
        if (!checkLabels.isEmpty()) {
            String key = "column:" + Schema.key(table.name()) + "." + column.name().toLowerCase();
            return Optional.of(new EnumSource(key, Optional.empty(), checkLabels));
        }
        return Optional.empty();
    }

    /** Scans the table's CHECK constraints for the "poor man's enum" idiom: {@code CHECK (column IN ('a', 'b', 'c'))}. */
    private List<String> findCheckDerivedEnumLabels(Table table, String columnName) {
        for (TableConstraint constraint : table.constraints()) {
            if (!(constraint instanceof TableConstraint.Check(var _, var expr))) {
                continue;
            }
            if (expr instanceof Expression.In(var target, var values, var negated)
                    && !negated
                    && target instanceof Expression.ColumnReference(var refName)
                    && refName.equalsIgnoreCase(columnName)
                    && !values.isEmpty()
                    && values.stream().allMatch(v -> v instanceof Expression.Literal lit && lit.kind() == Expression.LiteralKind.STRING)) {
                return values.stream().map(v -> (String) ((Expression.Literal) v).value()).toList();
            }
        }
        return List.of();
    }

    /** The Java constant name for each label, in label order, de-duplicating any that normalize to the same name. Shared by enum rendering and default-value translation so they always agree. */
    private List<String> enumConstantNames(List<String> labels) {
        List<String> names = new ArrayList<>();
        Set<String> used = new HashSet<>();
        for (String label : labels) {
            String base = NameConverter.toJavaEnumConstant(label);
            String candidate = base;
            int suffix = 2;
            while (!used.add(candidate)) {
                candidate = base + "_" + suffix++;
            }
            names.add(candidate);
        }
        return names;
    }

    /** If the column's SQL DEFAULT is a string literal equal to one of the enum's labels, the Java constant it corresponds to. */
    private Optional<String> enumDefaultConstantFor(Column column, List<String> labels) {
        if (column.defaultValue().isPresent()
                && column.defaultValue().get() instanceof Expression.Literal lit
                && lit.kind() == Expression.LiteralKind.STRING) {
            int index = labels.indexOf((String) lit.value());
            if (index >= 0) {
                return Optional.of(enumConstantNames(labels).get(index));
            }
        }
        return Optional.empty();
    }

    private String resolveSharedOrPerColumnClassName(EnumSource source, String entityClassName, String fieldName, GenerationContext ctx) {
        String existing = ctx.generatedSharedTypeClassNames.get(source.cacheKey());
        if (existing != null) {
            return existing;
        }
        String candidate = source.sharedClassNameHint().orElseGet(() -> entityClassName + NameConverter.toPascalCase(fieldName));
        return uniqueClassName(candidate, ctx);
    }

    private String resolveNamedTypeClassName(/* CreateTypeStatement typeStatement */ QualifiedName typeName, GenerationContext ctx) {
//        String key = "type:" + Schema.key(typeStatement.typeName());
    	String key = "type:" + Schema.key(typeName);
        String existing = ctx.generatedSharedTypeClassNames.get(key);
        if (existing != null) {
            return existing;
        }
        return uniqueClassName(NameConverter.toEntityClassName(typeName.name()), ctx);
    }

    private String uniqueClassName(String candidate, GenerationContext ctx) {
        String name = candidate;
        int suffix = 2;
        while (ctx.emittedClassNames.contains(name) || ctx.tableToClassName.containsValue(name)) {
            name = candidate + suffix++;
        }
        return name;
    }

    private void ensureEnumGenerated(EnumSource source, String enumClassName, GenerationContext ctx, List<GeneratedEntity> localCompanions) {
        if (ctx.generatedSharedTypeClassNames.containsKey(source.cacheKey())) {
            return;
        }
        ctx.generatedSharedTypeClassNames.put(source.cacheKey(), enumClassName);
        ctx.emittedClassNames.add(enumClassName);
        GeneratedEntity enumClass = renderEnumClass(enumClassName, source.labels());
        GeneratedEntity converter = renderEnumConverter(enumClassName);
        // Named (shared) types go in generation order at the front of the result list; per-column ones
        // ride along with the table that defined them, exactly like any other companion class.
        if (source.sharedClassNameHint().isPresent()) {
            ctx.sharedCompanionsInOrder.add(enumClass);
            ctx.sharedCompanionsInOrder.add(converter);
        } else {
            localCompanions.add(enumClass);
            localCompanions.add(converter);
        }
    }

    private GeneratedEntity renderEnumClass(String className, List<String> labels) {
        List<String> constants = new ArrayList<>();
        Set<String> usedNames = new HashSet<>();
        for (String label : labels) {
            String base = NameConverter.toJavaEnumConstant(label);
            String constantName = base;
            int suffix = 2;
            while (!usedNames.add(constantName)) {
                constantName = base + "_" + suffix++;
            }
            constants.add(INDENT + constantName + "(\"" + label.replace("\"", "\\\"").replace("\\", "\\\\") + "\")");
        }

        StringBuilder body = new StringBuilder();
        body.append(String.join(",\n", constants)).append(";\n\n");
        body.append(INDENT).append("private final String value;\n\n");
        body.append(INDENT).append(className).append("(String value) {\n");
        body.append(INDENT).append(INDENT).append("this.value = value;\n");
        body.append(INDENT).append("}\n\n");
        body.append(INDENT).append("public static ").append(className).append(" of(final String value) {\n");
        body.append(INDENT).append(INDENT).append("for (").append(className).append(" candidate : values()) {\n");
        body.append(INDENT).append(INDENT).append(INDENT).append("if (candidate.value.equals(value)) {\n");
        body.append(INDENT).append(INDENT).append(INDENT).append(INDENT).append("return candidate;\n");
        body.append(INDENT).append(INDENT).append(INDENT).append("}\n");
        body.append(INDENT).append(INDENT).append("}\n");
        body.append(INDENT).append(INDENT).append("throw new IllegalArgumentException(\"Unknown ").append(className).append(" value: \" + value);\n");
        body.append(INDENT).append("}\n\n");
        body.append(INDENT).append("/** The exact underlying SQL value — may differ from {@link #name()} when the label wasn't already a valid Java identifier. */\n");
        body.append(INDENT).append("@Override\n");
        body.append(INDENT).append("public String toString() {\n");
        body.append(INDENT).append(INDENT).append("return value;\n");
        body.append(INDENT).append("}\n");

        StringBuilder source = new StringBuilder();
        source.append("package ").append(config.packageName()).append(";\n\n");
        source.append("/**\n");
        source.append(" * Generated from a SQL enum source (a named {@code CREATE TYPE ... AS ENUM}, an inline\n");
        source.append(" * {@code ENUM(...)}/{@code SET(...)} column type, or a {@code CHECK (col IN (...))} constraint).\n");
        source.append(" * Persisted via {@link ").append(className).append("Converter} (pure JPA {@code AttributeConverter} —\n");
        source.append(" * see its Javadoc for why {@code @Convert} is used here instead of {@code @Enumerated}).\n");
        source.append(" */\n");
        source.append("public enum ").append(className).append(" {\n\n");
        source.append(body);
        source.append("}\n");
        return new GeneratedEntity(config.packageName(), className, source.toString());
    }

    private GeneratedEntity renderEnumConverter(String enumClassName) {
        String converterClassName = enumClassName + "Converter";
        StringBuilder source = new StringBuilder();
        source.append("package ").append(config.packageName()).append(";\n\n");
        source.append("import ").append(config.persistenceNamespace()).append(".AttributeConverter;\n");
        source.append("import ").append(config.persistenceNamespace()).append(".Converter;\n\n");
        source.append("/**\n");
        source.append(" * Pure-JPA {@code AttributeConverter} for {@link ").append(enumClassName).append("}. Used via {@code @Convert}\n");
        source.append(" * rather than {@code @Enumerated(EnumType.STRING)} so the exact original SQL string value round-trips\n");
        source.append(" * even when it isn't a valid Java identifier as written (e.g. {@code \"in-progress\"}) — a bare\n");
        source.append(" * {@code @Enumerated(EnumType.STRING)} would instead persist the Java constant's own {@code name()}.\n");
        source.append(" */\n");
        source.append("@Converter(autoApply = true)\n");
        source.append("public class ").append(converterClassName)
                .append(" implements AttributeConverter<").append(enumClassName).append(", String> {\n\n");
        source.append(INDENT).append("@Override\n");
        source.append(INDENT).append("public String convertToDatabaseColumn(").append(enumClassName).append(" attribute) {\n");
        source.append(INDENT).append(INDENT).append("return attribute == null ? null : attribute.getValue();\n");
        source.append(INDENT).append("}\n\n");
        source.append(INDENT).append("@Override\n");
        source.append(INDENT).append("public ").append(enumClassName).append(" convertToEntityAttribute(String dbData) {\n");
        source.append(INDENT).append(INDENT).append("return dbData == null ? null : ").append(enumClassName).append(".of(dbData);\n");
        source.append(INDENT).append("}\n");
        source.append("}\n");
        return new GeneratedEntity(config.packageName(), converterClassName, source.toString());
    }

    // ------------------------------------------------------ composite-type columns

//    private Optional<CreateTypeStatement> detectCompositeType(Table table, Column column, Schema schema) {
//        Optional<CreateTypeStatement> t = schema.findType(column.dataType().name(), table.name().schema());
//        if (t.isPresent() && t.get().definition() instanceof TypeDefinition.Composite) {
//            return t;
//        }
//        return Optional.empty();
//    }
    
    private Optional<Record> detectRecord(Table table, Column column, Schema schema) {
    	Optional<Record> _record = schema.findRecord(column.dataType().name());
    	if (_record.isPresent()) return _record;
    	return Optional.empty();
    }

//    private void ensureCompositeTypeGenerated(CreateTypeStatement typeStatement, String className, GenerationContext ctx, List<GeneratedEntity> localCompanions) {
//        String key = "type:" + typeStatement.typeName().toString().toLowerCase();
//        if (ctx.generatedSharedTypeClassNames.containsKey(key)) {
//            return;
//        }
//        ctx.generatedSharedTypeClassNames.put(key, className);
//        ctx.emittedClassNames.add(className);
//        TypeDefinition.Composite composite = (TypeDefinition.Composite) typeStatement.definition();
//        GeneratedEntity valueClass = renderCompositeValueClass(className, composite, typeStatement.typeName().toString());
//        GeneratedEntity converter = renderCompositeConverterStub(className, typeStatement.typeName().toString());
//        ctx.sharedCompanionsInOrder.add(valueClass);
//        ctx.sharedCompanionsInOrder.add(converter);
//    }
    
    private void ensureRecordGenerated(Record _record, String className, GenerationContext ctx, List<GeneratedEntity> localCompanions) {
        String key = "type:" + _record.name().toString().toLowerCase();
        if (ctx.generatedSharedTypeClassNames.containsKey(key)) {
            return;
        }
        ctx.generatedSharedTypeClassNames.put(key, className);
        ctx.emittedClassNames.add(className);
        GeneratedEntity valueClass = renderRecordValueClass(className, _record, _record.name().toString());
        GeneratedEntity converter = renderRecordConverterStub(className, _record.name().toString());
        ctx.sharedCompanionsInOrder.add(valueClass);
        ctx.sharedCompanionsInOrder.add(converter);
    }

//    private GeneratedEntity renderCompositeValueClass(String className, TypeDefinition.Composite composite, String sqlTypeName) {
//        TreeSet<String> imports = new TreeSet<>();
//        List<String> components = new ArrayList<>();
//        for (CompositeAttribute attribute : composite.attributes()) {
//            JavaType javaType = typeConverter.toJavaType(attribute.dataType(), true);
//            javaType.qualifiedName().ifPresent(imports::add);
//            components.add(javaType.simpleName() + " " + NameConverter.toCamelCase(attribute.name()));
//        }
//
//        StringBuilder source = new StringBuilder();
//        source.append("package ").append(config.packageName()).append(";\n\n");
//        for (String imp : imports) {
//            source.append("import ").append(imp).append(";\n");
//        }
//        if (!imports.isEmpty()) {
//            source.append('\n');
//        }
//        source.append("/**\n");
//        source.append(" * Value type mirroring PostgreSQL composite type {@code ").append(sqlTypeName).append("}.\n");
//        source.append(" * Persisted via {@link ").append(className).append("Converter} — see its Javadoc: it's a stub.\n");
//        source.append(" */\n");
//        source.append("public record ").append(className).append('(').append(String.join(", ", components)).append(") {}\n");
//        return new GeneratedEntity(config.packageName(), className, source.toString());
//    }
    
    private GeneratedEntity renderRecordValueClass(String className, Record _record, String sqlTypeName) {
        TreeSet<String> imports = new TreeSet<>();
        List<String> components = new ArrayList<>();
        for (Attribute attribute : _record.attributes().values()) {
            JavaType javaType = typeConverter.toJavaType(attribute.dataType(), true);
            javaType.qualifiedName().ifPresent(imports::add);
            components.add(javaType.simpleName() + " " + NameConverter.toCamelCase(attribute.name()));
        }

        StringBuilder source = new StringBuilder();
        source.append("package ").append(config.packageName()).append(";\n\n");
        for (String imp : imports) {
            source.append("import ").append(imp).append(";\n");
        }
        if (!imports.isEmpty()) {
            source.append('\n');
        }
        source.append("/**\n");
        source.append(" * Value type mirroring PostgreSQL composite type {@code ").append(sqlTypeName).append("}.\n");
        source.append(" * Persisted via {@link ").append(className).append("Converter} — see its Javadoc: it's a stub.\n");
        source.append(" */\n");
        source.append("public record ").append(className).append('(').append(String.join(", ", components)).append(") {}\n");
        return new GeneratedEntity(config.packageName(), className, source.toString());
    }

//    private GeneratedEntity renderCompositeConverterStub(String className, String sqlTypeName) {
//        String converterClassName = className + "Converter";
//        StringBuilder source = new StringBuilder();
//        source.append("package ").append(config.packageName()).append(";\n\n");
//        source.append("import ").append(config.persistenceNamespace()).append(".AttributeConverter;\n");
//        source.append("import ").append(config.persistenceNamespace()).append(".Converter;\n\n");
//        source.append("/**\n");
//        source.append(" * <b>Stub.</b> {@code ").append(sqlTypeName).append("} is a PostgreSQL composite type — a single\n");
//        source.append(" * column whose wire format is {@code (value1,value2,...)} with its own quoting/escaping rules\n");
//        source.append(" * (a value containing a comma, parenthesis, or double quote needs it escaped). This generator\n");
//        source.append(" * intentionally does not fabricate that parsing logic — getting it subtly wrong is worse than\n");
//        source.append(" * not generating it at all. Implement both methods below against your actual driver/column\n");
//        source.append(" * behavior before using this converter.\n");
//        source.append(" */\n");
//        source.append("@Converter(autoApply = true)\n");
//        source.append("public class ").append(converterClassName)
//                .append(" implements AttributeConverter<").append(className).append(", String> {\n\n");
//        source.append(INDENT).append("@Override\n");
//        source.append(INDENT).append("public String convertToDatabaseColumn(").append(className).append(" attribute) {\n");
//        source.append(INDENT).append(INDENT).append("// TODO: serialize to PostgreSQL's composite text format: (field1,field2,...)\n");
//        source.append(INDENT).append(INDENT).append("throw new UnsupportedOperationException(\"TODO: implement ").append(sqlTypeName).append(" serialization\");\n");
//        source.append(INDENT).append("}\n\n");
//        source.append(INDENT).append("@Override\n");
//        source.append(INDENT).append("public ").append(className).append(" convertToEntityAttribute(String dbData) {\n");
//        source.append(INDENT).append(INDENT).append("// TODO: parse PostgreSQL's composite text format: (field1,field2,...)\n");
//        source.append(INDENT).append(INDENT).append("throw new UnsupportedOperationException(\"TODO: implement ").append(sqlTypeName).append(" parsing\");\n");
//        source.append(INDENT).append("}\n");
//        source.append("}\n");
//        return new GeneratedEntity(config.packageName(), converterClassName, source.toString());
//    }
    
    private GeneratedEntity renderRecordConverterStub(String className, String sqlTypeName) {
        String converterClassName = className + "Converter";
        StringBuilder source = new StringBuilder();
        source.append("package ").append(config.packageName()).append(";\n\n");
        source.append("import ").append(config.persistenceNamespace()).append(".AttributeConverter;\n");
        source.append("import ").append(config.persistenceNamespace()).append(".Converter;\n\n");
        source.append("/**\n");
        source.append(" * <b>Stub.</b> {@code ").append(sqlTypeName).append("} is a PostgreSQL composite type — a single\n");
        source.append(" * column whose wire format is {@code (value1,value2,...)} with its own quoting/escaping rules\n");
        source.append(" * (a value containing a comma, parenthesis, or double quote needs it escaped). This generator\n");
        source.append(" * intentionally does not fabricate that parsing logic — getting it subtly wrong is worse than\n");
        source.append(" * not generating it at all. Implement both methods below against your actual driver/column\n");
        source.append(" * behavior before using this converter.\n");
        source.append(" */\n");
        source.append("@Converter(autoApply = true)\n");
        source.append("public class ").append(converterClassName)
                .append(" implements AttributeConverter<").append(className).append(", String> {\n\n");
        source.append(INDENT).append("@Override\n");
        source.append(INDENT).append("public String convertToDatabaseColumn(").append(className).append(" attribute) {\n");
        source.append(INDENT).append(INDENT).append("// TODO: serialize to PostgreSQL's composite text format: (field1,field2,...)\n");
        source.append(INDENT).append(INDENT).append("throw new UnsupportedOperationException(\"TODO: implement ").append(sqlTypeName).append(" serialization\");\n");
        source.append(INDENT).append("}\n\n");
        source.append(INDENT).append("@Override\n");
        source.append(INDENT).append("public ").append(className).append(" convertToEntityAttribute(String dbData) {\n");
        source.append(INDENT).append(INDENT).append("// TODO: parse PostgreSQL's composite text format: (field1,field2,...)\n");
        source.append(INDENT).append(INDENT).append("throw new UnsupportedOperationException(\"TODO: implement ").append(sqlTypeName).append(" parsing\");\n");
        source.append(INDENT).append("}\n");
        source.append("}\n");
        return new GeneratedEntity(config.packageName(), converterClassName, source.toString());
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
            body.append('\n').append(renderAccessors(f.fieldName(), effectiveFieldType(f)));
        }

        if (config.generateEqualsAndHashCode() && !table.primaryKeyColumns().isEmpty()) {
            List<String> pkFieldNames = fields.stream().filter(FieldPlan::primaryKey).map(FieldPlan::fieldName).toList();
            body.append('\n').append(renderEqualsAndHashCode(className, pkFieldNames));
        }

        StringBuilder source = new StringBuilder();
        source.append("package ").append(config.packageName()).append(";\n\n");
        for (String imp : imports) {
            source.append("import ").append(imp).append(";\n");
        }
        source.append('\n');
        appendCheckConstraintJavadoc(source, table);

        source.append("@Entity\n");
        source.append(renderTableAnnotation(table)).append('\n');
        if (compositeKey) {
            source.append("@IdClass(").append(idClassName).append(".class)\n");
        }
        source.append("public class ").append(className).append(" {\n\n");
        source.append(body);
        source.append("}\n");

        return new GeneratedEntity(config.packageName(), className, source.toString());
    }

    /** The {@code @EmbeddedId}-strategy entity: PK scalar columns live only in the embeddable; PK relationship columns use {@code @MapsId} instead of {@code @Id}. */
    private GeneratedEntity renderEntityWithEmbeddedId(Table table, String className, List<FieldPlan> fields, String idClassName) {
        TreeSet<String> imports = new TreeSet<>();
        imports.add(config.persistenceNamespace() + ".*");
        imports.add("java.util.Objects");
        for (FieldPlan f : fields) {
            if (!(f.primaryKey() && !f.relationship())) { // skip pk-only scalar fields; they don't appear on the entity
                f.javaType().qualifiedName().ifPresent(imports::add);
            }
        }

        StringBuilder body = new StringBuilder();
        body.append(INDENT).append("@EmbeddedId\n");
        body.append(INDENT).append("private ").append(idClassName).append(" id;\n\n");

        List<FieldPlan> nonPkScalarFields = new ArrayList<>();
        for (FieldPlan f : fields) {
            if (f.primaryKey() && !f.relationship()) {
                continue; // lives only inside the embeddable
            }
            if (f.primaryKey() && f.relationship()) {
                body.append(renderMapsIdRelationshipField(f)).append('\n');
            } else {
                body.append(renderField(f)).append('\n');
                nonPkScalarFields.add(f);
            }
        }

        body.append(INDENT).append("public ").append(className).append("() {\n");
        body.append(INDENT).append("}\n\n");
        body.append(renderAccessors("id", idClassName));
        for (FieldPlan f : fields) {
            if (f.primaryKey() && !f.relationship()) {
                continue;
            }
            body.append('\n').append(renderAccessors(f.fieldName(), effectiveFieldType(f)));
        }

        if (config.generateEqualsAndHashCode()) {
            body.append('\n').append(renderEqualsAndHashCode(className, List.of("id")));
        }

        StringBuilder source = new StringBuilder();
        source.append("package ").append(config.packageName()).append(";\n\n");
        for (String imp : imports) {
            source.append("import ").append(imp).append(";\n");
        }
        source.append('\n');
        appendCheckConstraintJavadoc(source, table);
        source.append("@Entity\n");
        source.append(renderTableAnnotation(table)).append('\n');
        source.append("public class ").append(className).append(" {\n\n");
        source.append(body);
        source.append("}\n");
        return new GeneratedEntity(config.packageName(), className, source.toString());
    }

    private String renderMapsIdRelationshipField(FieldPlan f) {
        StringBuilder sb = new StringBuilder();
        sb.append(INDENT).append("@MapsId(\"").append(f.fieldName()).append("\")\n");
        sb.append(INDENT).append("@ManyToOne(fetch = FetchType.").append(config.toOneFetchType())
                .append(f.nullable() ? "" : ", optional = false").append(")\n");
        sb.append(INDENT).append(renderJoinColumns(f)).append('\n');
        sb.append(INDENT).append("private ").append(f.javaType().simpleName()).append(' ').append(f.fieldName()).append(";\n");
        return sb.toString();
    }

    private void appendCheckConstraintJavadoc(StringBuilder source, Table table) {
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

    /** The Java type that should appear on the field/accessor: the converted (enum/composite) type when present, else the mapped SQL type. */
    private String effectiveFieldType(FieldPlan f) {
        return f.convertedJavaType().orElse(f.javaType().simpleName());
    }

    private String renderField(FieldPlan f) {
        StringBuilder sb = new StringBuilder();
        if (f.unmappedType() && f.convertedJavaType().isEmpty()) {
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
            sb.append(INDENT).append("private ").append(f.javaType().simpleName()).append(' ').append(f.fieldName()).append(";\n");
            return sb.toString();
        }

        sb.append(INDENT).append(renderColumnAnnotation(f)).append('\n');
        if (f.converterClassName().isPresent()) {
            sb.append(INDENT).append("@Convert(converter = ").append(f.converterClassName().get()).append(".class)\n");
        }
        String type = effectiveFieldType(f);
        sb.append(INDENT).append("private ").append(type).append(' ').append(f.fieldName())
                .append(javaFieldInitializer(f)).append(";\n");
        return sb.toString();
    }

    /**
     * A plain Java field initializer for a SQL {@code DEFAULT}, when the default is a literal simple
     * enough to translate safely (a number, string, boolean, or {@code NULL}) — pure Java, no
     * annotation, no provider extensions. Anything else (a function call like {@code NOW()}, an arbitrary
     * expression) is left as a Javadoc note instead of guessing at a Java equivalent; fabricating one
     * for an arbitrary SQL function would be more likely to be subtly wrong than to help.
     */
    private String javaFieldInitializer(FieldPlan f) {
        if (f.enumDefaultConstant().isPresent() && f.convertedJavaType().isPresent()) {
            return " = " + f.convertedJavaType().get() + "." + f.enumDefaultConstant().get();
        }
        if (f.defaultValue().isEmpty() || f.convertedJavaType().isPresent()) {
            return ""; // a composite-mapped field's default (if any) isn't translated into the generated type
        }
        Expression value = f.defaultValue().get();
        if (!(value instanceof Expression.Literal literal)) {
            return "";
        }
        return switch (literal.kind()) {
            case STRING -> " = \"" + ((String) literal.value()).replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
            case BOOLEAN -> " = " + literal.value();
            case NUMBER -> literalNumberInitializer(literal.value(), f.javaType().simpleName());
            case NULL -> "";
        };
    }

	private String literalNumberInitializer(Object value, String javaTypeName) {
	    String suffix = switch (javaTypeName) {
	        case "Long" -> "L";
	        case "Float" -> "f";
	        case "Double" -> "d";
	        case "BigDecimal" -> ""; // BigDecimal has no numeric-literal suffix; left as a plain numeric initializer (auto-boxing a double into BigDecimal isn't valid Java, so this is intentionally conservative)
	        default -> "";
	    };
	    if (javaTypeName.equals("BigDecimal")) {
	        return ""; // needs BigDecimal.valueOf(...), which isn't a field-initializer literal; skip rather than emit invalid code
	    }
	    return " = " + value + suffix;
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

    private String renderAccessors(String fieldName, String javaTypeName) {
        String accessorSuffix = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        StringBuilder sb = new StringBuilder();
        sb.append(INDENT).append("public ").append(javaTypeName).append(" get").append(accessorSuffix).append("() {\n");
        sb.append(INDENT).append(INDENT).append("return ").append(fieldName).append(";\n");
        sb.append(INDENT).append("}\n\n");
        sb.append(INDENT).append("public void set").append(accessorSuffix).append("(").append(javaTypeName).append(' ').append(fieldName).append(") {\n");
        sb.append(INDENT).append(INDENT).append("this.").append(fieldName).append(" = ").append(fieldName).append(";\n");
        sb.append(INDENT).append("}\n");
        return sb.toString();
    }

    private String renderEqualsAndHashCode(String className, List<String> pkFieldNames) {
        StringBuilder sb = new StringBuilder();
        sb.append(INDENT).append("@Override\n");
        sb.append(INDENT).append("public boolean equals(Object o) {\n");
        sb.append(INDENT).append(INDENT).append("if (this == o) return true;\n");
        sb.append(INDENT).append(INDENT).append("if (!(o instanceof ").append(className).append(" other)) return false;\n");
        String comparison = pkFieldNames.stream()
                .map(name -> "Objects.equals(" + name + ", other." + name + ")")
                .collect(Collectors.joining(" && "));
        sb.append(INDENT).append(INDENT).append("return ").append(comparison).append(";\n");
        sb.append(INDENT).append("}\n\n");

        sb.append(INDENT).append("@Override\n");
        sb.append(INDENT).append("public int hashCode() {\n");
        sb.append(INDENT).append(INDENT).append("// Stable across proxy (un)initialization, per the entity's persistent identity.\n");
        sb.append(INDENT).append(INDENT).append("return getClass().hashCode();\n");
        sb.append(INDENT).append("}\n");
        return sb.toString();
    }

    // ------------------------------------------------------------ render: id class

    private GeneratedEntity renderIdClass(String idClassName, List<FieldPlan> fields, boolean embeddable) {
        List<FieldPlan> pkFields = fields.stream().filter(FieldPlan::primaryKey).toList();

        TreeSet<String> imports = new TreeSet<>();
        imports.add("java.io.Serializable");
        imports.add("java.util.Objects");
        for (FieldPlan f : pkFields) {
            f.idClassFieldType().qualifiedName().ifPresent(imports::add);
        }
        if (embeddable) {
            imports.add(config.persistenceNamespace() + ".Embeddable");
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
            body.append('\n').append(renderAccessors(f.fieldName(), f.idClassFieldType().simpleName()));
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
        String baseName = idClassName.substring(0, idClassName.length() - 2);
        if (embeddable) {
            source.append("/** Composite-key companion for {@link ").append(baseName).append("}, embedded via {@code @EmbeddedId}. */\n");
            source.append("@Embeddable\n");
        } else {
            source.append("/** Composite-key companion for {@link ").append(baseName).append("}, used with {@code @IdClass}. */\n");
        }
        source.append("public class ").append(idClassName).append(" implements Serializable {\n\n");
        source.append(body);
        source.append("}\n");

        return new GeneratedEntity(config.packageName(), idClassName, source.toString());
    }
}
