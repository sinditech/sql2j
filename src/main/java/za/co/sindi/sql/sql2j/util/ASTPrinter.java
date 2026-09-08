package za.co.sindi.sql.sql2j.util;

import java.util.List;
import java.util.stream.Collectors;

import za.co.sindi.sql.sql2j.ast.ASTVisitor;
import za.co.sindi.sql.sql2j.ast.AlterAction;
import za.co.sindi.sql.sql2j.ast.AlterTableStatement;
import za.co.sindi.sql.sql2j.ast.CascadeOption;
import za.co.sindi.sql.sql2j.ast.ColumnConstraint;
import za.co.sindi.sql.sql2j.ast.ColumnDefinition;
import za.co.sindi.sql.sql2j.ast.CompositeAttribute;
import za.co.sindi.sql.sql2j.ast.CreateFunctionStatement;
import za.co.sindi.sql.sql2j.ast.CreateIndexStatement;
import za.co.sindi.sql.sql2j.ast.CreateProcedureStatement;
import za.co.sindi.sql.sql2j.ast.CreateSchemaStatement;
import za.co.sindi.sql.sql2j.ast.CreateTableStatement;
import za.co.sindi.sql.sql2j.ast.CreateTriggerStatement;
import za.co.sindi.sql.sql2j.ast.CreateTypeStatement;
import za.co.sindi.sql.sql2j.ast.CreateViewStatement;
import za.co.sindi.sql.sql2j.ast.DataType;
import za.co.sindi.sql.sql2j.ast.DropFunctionStatement;
import za.co.sindi.sql.sql2j.ast.DropIndexStatement;
import za.co.sindi.sql.sql2j.ast.DropProcedureStatement;
import za.co.sindi.sql.sql2j.ast.DropSchemaStatement;
import za.co.sindi.sql.sql2j.ast.DropTableStatement;
import za.co.sindi.sql.sql2j.ast.DropTriggerStatement;
import za.co.sindi.sql.sql2j.ast.DropTypeStatement;
import za.co.sindi.sql.sql2j.ast.DropViewStatement;
import za.co.sindi.sql.sql2j.ast.Expression;
import za.co.sindi.sql.sql2j.ast.FunctionModifier;
import za.co.sindi.sql.sql2j.ast.InlineIndexDefinition;
import za.co.sindi.sql.sql2j.ast.ParameterMode;
import za.co.sindi.sql.sql2j.ast.ReferentialAction;
import za.co.sindi.sql.sql2j.ast.RoutineBody;
import za.co.sindi.sql.sql2j.ast.RoutineCharacteristic;
import za.co.sindi.sql.sql2j.ast.RoutineParameter;
import za.co.sindi.sql.sql2j.ast.RoutineReturnType;
import za.co.sindi.sql.sql2j.ast.Statement;
import za.co.sindi.sql.sql2j.ast.TableConstraint;
import za.co.sindi.sql.sql2j.ast.TableOption;
import za.co.sindi.sql.sql2j.ast.TriggerAction;
import za.co.sindi.sql.sql2j.ast.TriggerEvent;
import za.co.sindi.sql.sql2j.ast.TriggerReference;
import za.co.sindi.sql.sql2j.ast.TriggerTiming;
import za.co.sindi.sql.sql2j.ast.TypeDefinition;
import za.co.sindi.sql.sql2j.ast.TypeOption;

/**
 * Renders an AST back into readable (if not byte-for-byte original) SQL.
 * <p>
 * This is the primary example {@link AstVisitor} implementation: it shows
 * how a consumer plugs a brand-new operation onto the AST without touching
 * any of the {@code com.sqlddl.ast} record classes at all.
 */
public final class ASTPrinter implements ASTVisitor<String> {

    /** Renders a single statement. */
    public String print(Statement statement) {
        return AstVisitor.dispatch(statement, this) + ";";
    }

    /** Renders a full script, one statement per line-pair. */
    public String printAll(List<Statement> statements) {
        return statements.stream().map(this::print).collect(Collectors.joining("\n\n"));
    }

    @Override
    public String visitCreateTable(CreateTableStatement s) {
        StringBuilder sb = new StringBuilder("CREATE TABLE ");
        if (s.ifNotExists()) sb.append("IF NOT EXISTS ");
        sb.append(s.tableName()).append(" (\n");

        List<String> parts = new java.util.ArrayList<>();
        for (ColumnDefinition column : s.columns()) {
            parts.add("    " + printColumn(column));
        }
        for (TableConstraint constraint : s.constraints()) {
            parts.add("    " + printTableConstraint(constraint));
        }
        for (InlineIndexDefinition index : s.indexes()) {
            parts.add("    " + printInlineIndex(index));
        }
        sb.append(String.join(",\n", parts));
        sb.append("\n)");
        if (!s.options().isEmpty()) {
            sb.append(' ').append(printTableOptions(s.options()));
        }
        return sb.toString();
    }

    private String printInlineIndex(InlineIndexDefinition index) {
        return "KEY " + index.name().map(n -> n + " ").orElse("") + "(" + String.join(", ", index.columns()) + ")";
    }

    private String printTableOptions(List<TableOption> options) {
        return options.stream()
                .map(o -> o.value().map(v -> o.name() + "=" + v).orElse(o.name()))
                .collect(Collectors.joining(" "));
    }

    @Override
    public String visitAlterTable(AlterTableStatement s) {
        return "ALTER TABLE " + s.tableName() + " " + printAlterAction(s.action());
    }

    @Override
    public String visitDropTable(DropTableStatement s) {
        String names = s.tableNames().stream().map(Object::toString).collect(Collectors.joining(", "));
        return "DROP TABLE " + (s.ifExists() ? "IF EXISTS " : "") + names + cascadeSuffix(s.cascadeOption());
    }

    @Override
    public String visitCreateIndex(CreateIndexStatement s) {
        return "CREATE " + (s.unique() ? "UNIQUE " : "") + "INDEX "
                + (s.ifNotExists() ? "IF NOT EXISTS " : "")
                + s.indexName() + " ON " + s.tableName()
                + " (" + String.join(", ", s.columns()) + ")";
    }

    @Override
    public String visitDropIndex(DropIndexStatement s) {
        return "DROP INDEX " + (s.ifExists() ? "IF EXISTS " : "") + s.indexName();
    }

    @Override
    public String visitCreateView(CreateViewStatement s) {
        StringBuilder sb = new StringBuilder("CREATE ");
        if (s.orReplace()) sb.append("OR REPLACE ");
        sb.append("VIEW ").append(s.viewName());
        if (!s.columnNames().isEmpty()) {
            sb.append(" (").append(String.join(", ", s.columnNames())).append(")");
        }
        sb.append(" AS ").append(s.query());
        return sb.toString();
    }

    @Override
    public String visitDropView(DropViewStatement s) {
        return "DROP VIEW " + (s.ifExists() ? "IF EXISTS " : "") + s.viewName();
    }

    @Override
    public String visitCreateSchema(CreateSchemaStatement s) {
        return "CREATE SCHEMA " + (s.ifNotExists() ? "IF NOT EXISTS " : "") + s.schemaName();
    }

    @Override
    public String visitDropSchema(DropSchemaStatement s) {
        return "DROP SCHEMA " + (s.ifExists() ? "IF EXISTS " : "") + s.schemaName() + cascadeSuffix(s.cascadeOption());
    }

    @Override
    public String visitCreateFunction(CreateFunctionStatement s) {
        StringBuilder sb = new StringBuilder("CREATE ");
        if (s.modifier() == FunctionModifier.REPLACE) sb.append("OR REPLACE ");
        else if (s.modifier() == FunctionModifier.ALTER) sb.append("OR ALTER ");
        sb.append("FUNCTION ").append(s.functionName())
                .append(" (").append(printParameters(s.parameters())).append(")\n");
        sb.append("RETURNS ").append(printReturnType(s.returnType()));
        for (RoutineCharacteristic c : s.characteristics()) {
            sb.append('\n').append(printCharacteristic(c));
        }
        sb.append("\nAS ").append(printBody(s.body()));
        return sb.toString();
    }

    @Override
    public String visitDropFunction(DropFunctionStatement s) {
        return "DROP FUNCTION " + (s.ifExists() ? "IF EXISTS " : "") + s.functionName()
                + s.parameterTypes().map(this::printTypeList).orElse("")
                + cascadeSuffix(s.cascadeOption());
    }

    @Override
    public String visitCreateProcedure(CreateProcedureStatement s) {
        StringBuilder sb = new StringBuilder("CREATE ");
        if (s.orReplace()) sb.append("OR REPLACE ");
        sb.append("PROCEDURE ").append(s.procedureName())
                .append(" (").append(printParameters(s.parameters())).append(")");
        for (RoutineCharacteristic c : s.characteristics()) {
            sb.append('\n').append(printCharacteristic(c));
        }
        sb.append("\nAS ").append(printBody(s.body()));
        return sb.toString();
    }

    @Override
    public String visitDropProcedure(DropProcedureStatement s) {
        return "DROP PROCEDURE " + (s.ifExists() ? "IF EXISTS " : "") + s.procedureName()
                + s.parameterTypes().map(this::printTypeList).orElse("")
                + cascadeSuffix(s.cascadeOption());
    }

    @Override
    public String visitCreateType(CreateTypeStatement s) {
        return "CREATE TYPE " + s.typeName() + printTypeDefinition(s.definition());
    }

    @Override
    public String visitDropType(DropTypeStatement s) {
        String names = s.typeNames().stream().map(Object::toString).collect(Collectors.joining(", "));
        return "DROP TYPE " + (s.ifExists() ? "IF EXISTS " : "") + names + cascadeSuffix(s.cascadeOption());
    }

    // ------------------------------------------------------------ type helpers

    private String printTypeDefinition(TypeDefinition definition) {
        return switch (definition) {
            case TypeDefinition.Shell() -> "";
            case TypeDefinition.Composite(var attributes) ->
                    " AS (" + attributes.stream().map(this::printCompositeAttribute).collect(Collectors.joining(", ")) + ")";
            case TypeDefinition.Enum(var labels) ->
                    " AS ENUM (" + labels.stream()
                            .map(l -> "'" + l.replace("'", "''") + "'")
                            .collect(Collectors.joining(", ")) + ")";
            case TypeDefinition.Range(var options) -> " AS RANGE (" + printTypeOptions(options) + ")";
            case TypeDefinition.Base(var options) -> " (" + printTypeOptions(options) + ")";
        };
    }

    private String printCompositeAttribute(CompositeAttribute attribute) {
        StringBuilder sb = new StringBuilder(attribute.name()).append(' ').append(attribute.dataType());
        attribute.collation().ifPresent(c -> sb.append(" COLLATE ").append(c));
        return sb.toString();
    }

    private String printTypeOptions(List<TypeOption> options) {
        return options.stream().map(this::printTypeOption).collect(Collectors.joining(", "));
    }

    private String printTypeOption(TypeOption option) {
        return option.value().map(v -> option.name() + " = " + v).orElse(option.name());
    }

    @Override
    public String visitCreateTrigger(CreateTriggerStatement s) {
        StringBuilder sb = new StringBuilder("CREATE ");
        if (s.orReplace()) sb.append("OR REPLACE ");
        if (s.constraintTrigger()) sb.append("CONSTRAINT ");
        sb.append("TRIGGER ").append(s.triggerName()).append('\n');
        sb.append(printTriggerTiming(s.timing())).append(' ')
                .append(s.events().stream().map(this::printTriggerEvent).collect(Collectors.joining(" OR ")))
                .append('\n');
        sb.append("ON ").append(s.tableName());
        if (!s.referencing().isEmpty()) {
            sb.append("\nREFERENCING ").append(s.referencing().stream().map(this::printTriggerReference).collect(Collectors.joining(" ")));
        }
        s.level().ifPresent(level -> sb.append("\nFOR EACH ").append(level));
        s.order().ifPresent(order -> sb.append('\n').append(order.position()).append(' ').append(order.otherTriggerName()));
        s.when().ifPresent(cond -> sb.append("\nWHEN (").append(printExpression(cond)).append(')'));
        sb.append('\n').append(printTriggerAction(s.action()));
        return sb.toString();
    }

    @Override
    public String visitDropTrigger(DropTriggerStatement s) {
        StringBuilder sb = new StringBuilder("DROP TRIGGER ");
        if (s.ifExists()) sb.append("IF EXISTS ");
        sb.append(s.triggerName());
        s.tableName().ifPresent(t -> sb.append(" ON ").append(t));
        sb.append(cascadeSuffix(s.cascadeOption()));
        return sb.toString();
    }

    // --------------------------------------------------------- trigger helpers

    private String printTriggerTiming(TriggerTiming timing) {
        return switch (timing) {
            case BEFORE -> "BEFORE";
            case AFTER -> "AFTER";
            case INSTEAD_OF -> "INSTEAD OF";
        };
    }

    private String printTriggerEvent(TriggerEvent event) {
        return switch (event) {
            case TriggerEvent.Insert ignored -> "INSERT";
            case TriggerEvent.Delete ignored -> "DELETE";
            case TriggerEvent.Truncate ignored -> "TRUNCATE";
            case TriggerEvent.Update(var columns) ->
                    "UPDATE" + (columns.isEmpty() ? "" : " OF " + String.join(", ", columns));
        };
    }

    private String printTriggerReference(TriggerReference ref) {
        return ref.transition() + " " + (ref.isTable() ? "TABLE " : "") + "AS " + ref.alias();
    }

    private String printTriggerAction(TriggerAction action) {
        return switch (action) {
            case TriggerAction.Body(var body) -> printBody(body);
            case TriggerAction.ExecuteFunction(var fn, var args, var legacy) ->
                    "EXECUTE " + (legacy ? "PROCEDURE " : "FUNCTION ") + fn + "("
                            + args.stream().map(this::printExpression).collect(Collectors.joining(", ")) + ")";
        };
    }

    @Override
    public String visitSelect(SelectStatement s) {
        return printSelectStatement(s);
    }

    @Override
    public String visitUpdate(UpdateStatement s) {
        StringBuilder sb = new StringBuilder("UPDATE ").append(s.tableName());
        s.alias().ifPresent(a -> sb.append(" AS ").append(a));
        sb.append("\nSET ").append(s.assignments().stream()
                .map(a -> a.column() + " = " + printExpression(a.value()))
                .collect(Collectors.joining(", ")));
        if (!s.from().isEmpty()) {
            sb.append("\nFROM ").append(s.from().stream().map(this::printTableReference).collect(Collectors.joining(", ")));
        }
        s.where().ifPresent(w -> sb.append("\nWHERE ").append(printExpression(w)));
        appendReturning(sb, s.returning());
        return sb.toString();
    }

    @Override
    public String visitDelete(DeleteStatement s) {
        StringBuilder sb = new StringBuilder("DELETE FROM ").append(s.tableName());
        s.alias().ifPresent(a -> sb.append(" AS ").append(a));
        if (!s.using().isEmpty()) {
            sb.append("\nUSING ").append(s.using().stream().map(this::printTableReference).collect(Collectors.joining(", ")));
        }
        s.where().ifPresent(w -> sb.append("\nWHERE ").append(printExpression(w)));
        appendReturning(sb, s.returning());
        return sb.toString();
    }

    private void appendReturning(StringBuilder sb, List<SelectItem> returning) {
        if (!returning.isEmpty()) {
            sb.append("\nRETURNING ").append(returning.stream().map(this::printSelectItem).collect(Collectors.joining(", ")));
        }
    }

    @Override
    public String visitInsert(InsertStatement s) {
        StringBuilder sb = new StringBuilder("INSERT ");
        s.priority().ifPresent(p -> sb.append(p).append(' '));
        if (s.ignore()) sb.append("IGNORE ");
        sb.append("INTO ").append(s.tableName());
        if (!s.partitions().isEmpty()) {
            sb.append(" PARTITION (").append(String.join(", ", s.partitions())).append(')');
        }
        if (!s.columns().isEmpty()) {
            sb.append(" (").append(String.join(", ", s.columns())).append(')');
        }
        sb.append('\n').append(printInsertSource(s.source()));
        s.rowAlias().ifPresent(a -> sb.append(" AS ").append(a));
        s.upsert().ifPresent(u -> sb.append('\n').append(printUpsertClause(u)));
        appendReturning(sb, s.returning());
        return sb.toString();
    }

    private String printInsertSource(InsertSource source) {
        return switch (source) {
            case InsertSource.Values(var rows) -> "VALUES " + rows.stream().map(this::printValuesRow).collect(Collectors.joining(", "));
            case InsertSource.Query(var query) -> printSelectStatement(query);
            case InsertSource.DefaultValues ignored -> "DEFAULT VALUES";
            case InsertSource.SetAssignments(var assignments) -> "SET " + printAssignments(assignments);
        };
    }

    private String printValuesRow(List<Expression> row) {
        return "(" + row.stream().map(this::printExpression).collect(Collectors.joining(", ")) + ")";
    }

    private String printAssignments(List<UpdateAssignment> assignments) {
        return assignments.stream().map(a -> a.column() + " = " + printExpression(a.value())).collect(Collectors.joining(", "));
    }

    private String printUpsertClause(UpsertClause upsert) {
        return switch (upsert) {
            case UpsertClause.OnDuplicateKeyUpdate(var assignments) -> "ON DUPLICATE KEY UPDATE " + printAssignments(assignments);
            case UpsertClause.OnConflict(var cols, var constraint, var doNothing, var doUpdateSet, var where) -> {
                StringBuilder sb = new StringBuilder("ON CONFLICT");
                if (!cols.isEmpty()) {
                    sb.append(" (").append(String.join(", ", cols)).append(')');
                } else {
                    constraint.ifPresent(c -> sb.append(" ON CONSTRAINT ").append(c));
                }
                if (doNothing) {
                    sb.append(" DO NOTHING");
                } else {
                    sb.append(" DO UPDATE SET ").append(printAssignments(doUpdateSet));
                    where.ifPresent(w -> sb.append(" WHERE ").append(printExpression(w)));
                }
                yield sb.toString();
            }
        };
    }

    @Override
    public String visitTruncate(TruncateStatement s) {
        StringBuilder sb = new StringBuilder("TRUNCATE TABLE ");
        sb.append(s.tables().stream().map(this::printTruncateTarget).collect(Collectors.joining(", ")));
        s.identityOption().ifPresent(opt -> sb.append(' ').append(opt).append(" IDENTITY"));
        sb.append(cascadeSuffix(s.cascadeOption()));
        return sb.toString();
    }

    private String printTruncateTarget(TruncateTarget target) {
        return (target.only() ? "ONLY " : "") + target.tableName() + (target.includeChildren() ? " *" : "");
    }

    // ---------------------------------------------------------- select helpers

    /** Renders a {@link SelectStatement}; reused both for top-level {@code SELECT}s and for subqueries. */
    public String printSelectStatement(SelectStatement s) {
        StringBuilder sb = new StringBuilder();
        if (!s.commonTableExpressions().isEmpty()) {
            sb.append("WITH ");
            if (s.commonTableExpressions().stream().anyMatch(CommonTableExpression::recursive)) {
                sb.append("RECURSIVE ");
            }
            sb.append(s.commonTableExpressions().stream().map(this::printCte).collect(Collectors.joining(", ")));
            sb.append('\n');
        }
        sb.append("SELECT ");
        if (s.distinct()) sb.append("DISTINCT ");
        sb.append(s.selectItems().stream().map(this::printSelectItem).collect(Collectors.joining(", ")));
        if (!s.from().isEmpty()) {
            sb.append("\nFROM ").append(s.from().stream().map(this::printTableReference).collect(Collectors.joining(", ")));
        }
        s.where().ifPresent(w -> sb.append("\nWHERE ").append(printExpression(w)));
        if (!s.groupBy().isEmpty()) {
            sb.append("\nGROUP BY ").append(s.groupBy().stream().map(this::printExpression).collect(Collectors.joining(", ")));
        }
        s.having().ifPresent(h -> sb.append("\nHAVING ").append(printExpression(h)));
        if (!s.orderBy().isEmpty()) {
            sb.append("\nORDER BY ").append(s.orderBy().stream().map(this::printOrderByItem).collect(Collectors.joining(", ")));
        }
        s.limit().ifPresent(l -> sb.append("\nLIMIT ").append(printExpression(l)));
        s.offset().ifPresent(o -> sb.append("\nOFFSET ").append(printExpression(o)));
        for (SetOperation op : s.setOperations()) {
            sb.append('\n').append(op.type()).append(op.all() ? " ALL" : "").append('\n').append(printSelectStatement(op.select()));
        }
        return sb.toString();
    }

    private String printCte(CommonTableExpression cte) {
        String cols = cte.columnNames().isEmpty() ? "" : " (" + String.join(", ", cte.columnNames()) + ")";
        return cte.name() + cols + " AS (" + printSelectStatement(cte.query()) + ")";
    }

    private String printSelectItem(SelectItem item) {
        return switch (item) {
            case SelectItem.All ignored -> "*";
            case SelectItem.AllFromTable(var table) -> table + ".*";
            case SelectItem.Expr(var expr, var alias) -> printExpression(expr) + alias.map(a -> " AS " + a).orElse("");
        };
    }

    private String printTableReference(TableReference ref) {
        return switch (ref) {
            case TableReference.Named(var name, var alias) -> name + alias.map(a -> " AS " + a).orElse("");
            case TableReference.Subquery(var query, var alias) ->
                    "(" + printSelectStatement(query) + ")" + alias.map(a -> " AS " + a).orElse("");
            case TableReference.Joined(var left, var joinType, var right, var on, var using) -> {
                String joinKeyword = joinType == JoinType.CROSS ? "CROSS JOIN" : joinType + " JOIN";
                String suffix = on.map(o -> " ON " + printExpression(o))
                        .orElseGet(() -> using.isEmpty() ? "" : " USING (" + String.join(", ", using) + ")");
                yield printTableReference(left) + " " + joinKeyword + " " + printTableReference(right) + suffix;
            }
        };
    }

    private String printOrderByItem(OrderByItem item) {
        String nulls = item.nulls().map(n -> " NULLS " + n).orElse("");
        return printExpression(item.expression()) + " " + item.direction() + nulls;
    }

    // --------------------------------------------------------- routine helpers

    private String printTypeList(List<DataType> types) {
        return " (" + types.stream().map(DataType::toString).collect(Collectors.joining(", ")) + ")";
    }

    private String printParameters(List<RoutineParameter> parameters) {
        return parameters.stream().map(this::printParameter).collect(Collectors.joining(", "));
    }

    private String printParameter(RoutineParameter p) {
        StringBuilder sb = new StringBuilder();
        if (p.mode() != ParameterMode.IN) {
            sb.append(p.mode()).append(' ');
        }
        p.name().ifPresent(n -> sb.append(n).append(' '));
        sb.append(p.dataType());
        p.defaultValue().ifPresent(v -> sb.append(" DEFAULT ").append(printExpression(v)));
        return sb.toString();
    }

    private String printReturnType(RoutineReturnType returnType) {
        return switch (returnType) {
            case RoutineReturnType.Scalar(var type) -> type.toString();
            case RoutineReturnType.SetOf(var type) -> "SETOF " + type;
            case RoutineReturnType.Table(var columns) ->
                    "TABLE (" + columns.stream().map(this::printColumn).collect(Collectors.joining(", ")) + ")";
        };
    }

    private String printCharacteristic(RoutineCharacteristic c) {
        return switch (c) {
            case RoutineCharacteristic.Language(var name) -> "LANGUAGE " + name;
            case RoutineCharacteristic.Deterministic(var deterministic) -> deterministic ? "DETERMINISTIC" : "NOT DETERMINISTIC";
            case RoutineCharacteristic.Volatility(var category) -> category.toString();
            case RoutineCharacteristic.NullHandling(var mode) -> switch (mode) {
                case STRICT -> "STRICT";
                case CALLED_ON_NULL_INPUT -> "CALLED ON NULL INPUT";
                case RETURNS_NULL_ON_NULL_INPUT -> "RETURNS NULL ON NULL INPUT";
            };
            case RoutineCharacteristic.Security(var mode) -> "SECURITY " + mode;
        };
    }

    private String printBody(RoutineBody body) {
        return switch (body) {
            case RoutineBody.DollarQuoted(var tag, var source) -> "$" + tag + "$" + source + "$" + tag + "$";
            case RoutineBody.QuotedString(var source) -> "'" + source.replace("'", "''") + "'";
            case RoutineBody.BeginEndBlock(var source) -> source;
            case RoutineBody.ReturnExpression(var expr) -> "RETURN " + printExpression(expr);
            case RoutineBody.SingleStatement(var source) -> source;
        };
    }

    // ------------------------------------------------------------- helpers

    private String cascadeSuffix(CascadeOption option) {
        return switch (option) {
            case CASCADE -> " CASCADE";
            case RESTRICT -> " RESTRICT";
            case NONE -> "";
        };
    }

    private String printColumn(ColumnDefinition column) {
        StringBuilder sb = new StringBuilder(column.name()).append(' ').append(column.dataType());
        for (ColumnConstraint c : column.constraints()) {
            sb.append(' ').append(printColumnConstraint(c));
        }
        return sb.toString();
    }

    private String printColumnConstraint(ColumnConstraint c) {
        return switch (c) {
            case ColumnConstraint.NotNull ignored -> "NOT NULL";
            case ColumnConstraint.Nullable ignored -> "NULL";
            case ColumnConstraint.PrimaryKey ignored -> "PRIMARY KEY";
            case ColumnConstraint.Unique ignored -> "UNIQUE";
            case ColumnConstraint.AutoIncrement ignored -> "AUTO_INCREMENT";
            case ColumnConstraint.DefaultValue(var value) -> "DEFAULT " + printExpression(value);
            case ColumnConstraint.Check(var name, var expr) ->
                    (name.map(n -> "CONSTRAINT " + n + " ").orElse("")) + "CHECK (" + printExpression(expr) + ")";
            case ColumnConstraint.Collate(var collation) -> "COLLATE " + collation;
            case ColumnConstraint.References(var table, var cols, var onDelete, var onUpdate) ->
                    "REFERENCES " + table + (cols.isEmpty() ? "" : " (" + String.join(", ", cols) + ")")
                            + refActionsSuffix(onDelete, onUpdate);
        };
    }

    private String printTableConstraint(TableConstraint c) {
        String prefix = c.name().map(n -> "CONSTRAINT " + n + " ").orElse("");
        return switch (c) {
            case TableConstraint.PrimaryKey(var name, var columns) ->
                    prefix + "PRIMARY KEY (" + String.join(", ", columns) + ")";
            case TableConstraint.Unique(var name, var columns) ->
                    prefix + "UNIQUE (" + String.join(", ", columns) + ")";
            case TableConstraint.Check(var name, var expr) ->
                    prefix + "CHECK (" + printExpression(expr) + ")";
            case TableConstraint.ForeignKey(var name, var cols, var refTable, var refCols, var onDelete, var onUpdate) ->
                    prefix + "FOREIGN KEY (" + String.join(", ", cols) + ") REFERENCES " + refTable
                            + (refCols.isEmpty() ? "" : " (" + String.join(", ", refCols) + ")")
                            + refActionsSuffix(onDelete, onUpdate);
        };
    }

    private String refActionsSuffix(java.util.Optional<ReferentialAction> onDelete, java.util.Optional<ReferentialAction> onUpdate) {
        StringBuilder sb = new StringBuilder();
        onDelete.ifPresent(a -> sb.append(" ON DELETE ").append(printAction(a)));
        onUpdate.ifPresent(a -> sb.append(" ON UPDATE ").append(printAction(a)));
        return sb.toString();
    }

    private String printAction(ReferentialAction action) {
        return switch (action) {
            case CASCADE -> "CASCADE";
            case SET_NULL -> "SET NULL";
            case SET_DEFAULT -> "SET DEFAULT";
            case RESTRICT -> "RESTRICT";
            case NO_ACTION -> "NO ACTION";
        };
    }

    private String printAlterAction(AlterAction action) {
        return switch (action) {
            case AlterAction.AddColumn(var column, var ifNotExists) ->
                    "ADD COLUMN " + (ifNotExists ? "IF NOT EXISTS " : "") + printColumn(column);
            case AlterAction.DropColumn(var name, var ifExists) ->
                    "DROP COLUMN " + (ifExists ? "IF EXISTS " : "") + name;
            case AlterAction.RenameColumn(var from, var to) ->
                    "RENAME COLUMN " + from + " TO " + to;
            case AlterAction.AlterColumnType(var name, var type) ->
                    "ALTER COLUMN " + name + " TYPE " + type;
            case AlterAction.AlterColumnSetDefault(var name, var value) ->
                    "ALTER COLUMN " + name + " SET DEFAULT " + printExpression(value);
            case AlterAction.AlterColumnDropDefault(var name) ->
                    "ALTER COLUMN " + name + " DROP DEFAULT";
            case AlterAction.AlterColumnSetNotNull(var name) ->
                    "ALTER COLUMN " + name + " SET NOT NULL";
            case AlterAction.AlterColumnDropNotNull(var name) ->
                    "ALTER COLUMN " + name + " DROP NOT NULL";
            case AlterAction.RenameTable(var newName) ->
                    "RENAME TO " + newName;
            case AlterAction.AddConstraint(var constraint) ->
                    "ADD " + printTableConstraint(constraint);
            case AlterAction.DropConstraint(var name, var ifExists) ->
                    "DROP CONSTRAINT " + (ifExists ? "IF EXISTS " : "") + name;
        };
    }

    /** Renders a scalar {@link Expression} tree back into SQL text. */
    public String printExpression(Expression expression) {
        return switch (expression) {
            case Expression.Literal(var value, var kind) -> switch (kind) {
                case STRING -> "'" + value.toString().replace("'", "''") + "'";
                case NUMBER, BOOLEAN -> String.valueOf(value);
                case NULL -> "NULL";
            };
            case Expression.ColumnReference(var name) -> name;
            case Expression.FunctionCall(var name, var args) ->
                    name + "(" + args.stream().map(this::printExpression).collect(Collectors.joining(", ")) + ")";
            case Expression.Parenthesized(var inner) -> "(" + printExpression(inner) + ")";
            case Expression.Unary(var op, var operand) ->
                    op + (op.equals("NOT") ? " " : "") + printExpression(operand);
            case Expression.Binary(var left, var op, var right) ->
                    printExpression(left) + " " + op + " " + printExpression(right);
            case Expression.Between(var target, var lower, var upper, var negated) ->
                    printExpression(target) + (negated ? " NOT BETWEEN " : " BETWEEN ")
                            + printExpression(lower) + " AND " + printExpression(upper);
            case Expression.In(var target, var values, var negated) ->
                    printExpression(target) + (negated ? " NOT IN (" : " IN (")
                            + values.stream().map(this::printExpression).collect(Collectors.joining(", ")) + ")";
            case Expression.Like(var target, var pattern, var negated) ->
                    printExpression(target) + (negated ? " NOT LIKE " : " LIKE ") + printExpression(pattern);
            case Expression.IsNull(var target, var negated) ->
                    printExpression(target) + (negated ? " IS NOT NULL" : " IS NULL");
//            case Expression.Subquery(var query) -> "(" + printSelectStatement(query) + ")";
//            case Expression.Exists(var query, var negated) ->
//                    (negated ? "NOT EXISTS (" : "EXISTS (") + printSelectStatement(query) + ")";
//            case Expression.InSubquery(var target, var query, var negated) ->
//                    printExpression(target) + (negated ? " NOT IN (" : " IN (") + printSelectStatement(query) + ")";
            case Expression.Default ignored -> "DEFAULT";
        };
    }
}
