/**
 * 
 */
package za.co.sindi.sql.sql2j.codegen;

import java.util.List;
import java.util.Optional;

import za.co.sindi.sql.sql2j.ast.ASTVisitor;
import za.co.sindi.sql.sql2j.ast.AlterAction;
import za.co.sindi.sql.sql2j.ast.AlterTableStatement;
import za.co.sindi.sql.sql2j.ast.ColumnConstraint;
import za.co.sindi.sql.sql2j.ast.ColumnDefinition;
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
import za.co.sindi.sql.sql2j.ast.QualifiedName;
import za.co.sindi.sql.sql2j.ast.Statement;
import za.co.sindi.sql.sql2j.ast.TableConstraint;
import za.co.sindi.sql.sql2j.ast.TypeDefinition;
import za.co.sindi.sql.sql2j.codegen.metamodel.Column;
import za.co.sindi.sql.sql2j.codegen.metamodel.Schema;
import za.co.sindi.sql.sql2j.codegen.metamodel.Schemas;
import za.co.sindi.sql.sql2j.codegen.metamodel.Table;

/**
 * @author Buhake Sindi
 * @since 18 August 2026
 */
public class SchemaGenerator implements ASTVisitor<Void> {
	
	/** Convenience one-shot entry point. */
    public static void build(List<Statement> statements) {
    	SchemaGenerator generator = new SchemaGenerator();
        for (Statement statement : statements) {
        	ASTVisitor.dispatch(statement, generator);
        }
    }
	
	private Schema resolveSchema(final QualifiedName qName) {
		return qName.schema().isEmpty() ? Schemas.getDefault() : Schemas.getOrCreate(qName.schema().get());
	}

    private Column resolveColumn(ColumnDefinition column) {
        boolean nullable = true;
        boolean unique = false;
        boolean autoIncrement = isSerialType(column.dataType());
        Optional<Expression> defaultValue = Optional.empty();

        for (ColumnConstraint c : column.constraints()) {
            switch (c) {
                case ColumnConstraint.NotNull _ -> nullable = false;
                case ColumnConstraint.Nullable _ -> nullable = true;
                case ColumnConstraint.PrimaryKey _ -> nullable = false; // PK columns are implicitly NOT NULL
                case ColumnConstraint.Unique _ -> unique = true;
                case ColumnConstraint.AutoIncrement _ -> autoIncrement = true;
                case ColumnConstraint.DefaultValue(var value) -> defaultValue = Optional.of(value);
                default -> { /* References/Check/Collate don't affect Column's own flags */ }
            }
        }
        return new Column(column.name(), column.dataType(), nullable, unique, autoIncrement, defaultValue);
    }

    private boolean isSerialType(DataType type) {
        return switch (type.name().name()) {
            case "SERIAL", "SERIAL4", "BIGSERIAL", "SERIAL8", "SMALLSERIAL", "SERIAL2" -> true;
            default -> false;
        };
    }
    
    /** Column-level clauses (inline PK / REFERENCES / CHECK) also contribute to table-level bookkeeping. */
    private void foldColumnLevelTableEffects(Table table, ColumnDefinition column) {
        for (ColumnConstraint c : column.constraints()) {
            switch (c) {
                case ColumnConstraint.PrimaryKey _ -> table.addPrimaryKeyColumn(column.name());
                case ColumnConstraint.References(var refTable, var refCols, var onDelete, var onUpdate) ->
                        table.addConstraint(new TableConstraint.ForeignKey(
                                Optional.empty(), List.of(column.name()), refTable, refCols, onDelete, onUpdate));
                case ColumnConstraint.Check(var name, var expr) ->
                        table.addConstraint(new TableConstraint.Check(name, expr));
                default -> { /* NotNull/Nullable/Unique/Default/AutoIncrement/Collate already folded into ResolvedColumn */ }
            }
        }
    }

    private void applyAlterAction(Table table, AlterAction action) {
        switch (action) {
            case AlterAction.AddColumn(var column, var ifNotExists) -> {
                if (!ifNotExists || !table.columns().containsKey(column.name())) {
                    table.addColumn(resolveColumn(column));
                    foldColumnLevelTableEffects(table, column);
                }
            }
            case AlterAction.DropColumn(var name, var ifExists) -> {
                if (!ifExists || table.columns().containsKey(name)) {
                    table.removeColumn(name);
                }
            }
            case AlterAction.RenameColumn(var from, var to) -> table.renameColumn(from, to);
            case AlterAction.AlterColumnType(var name, var newType) -> updateColumn(table, name, c -> c.withDataType(newType));
            case AlterAction.AlterColumnSetDefault(var name, var value) ->
                    updateColumn(table, name, c -> c.withDefaultValue(Optional.of(value)));
            case AlterAction.AlterColumnDropDefault(var name) ->
                    updateColumn(table, name, c -> c.withDefaultValue(Optional.empty()));
            case AlterAction.AlterColumnSetNotNull(var name) -> updateColumn(table, name, c -> c.withNullable(false));
            case AlterAction.AlterColumnDropNotNull(var name) -> updateColumn(table, name, c -> c.withNullable(true));
            case AlterAction.RenameTable(var newName) -> {
                table.rename(newName);
                resolveSchema(table.name()).removeTable(table.name()); // stale key, if any
                resolveSchema(table.name()).addTable(table);
            }
            case AlterAction.AddConstraint(var constraint) -> {
                table.addConstraint(constraint);
                if (constraint instanceof TableConstraint.PrimaryKey pk) {
                    pk.columns().forEach(table::addPrimaryKeyColumn);
                }
            }
            case AlterAction.DropConstraint(var name, _) -> table.removeConstraintByName(name);
        }
    }

    private void updateColumn(Table table, String name, java.util.function.UnaryOperator<Column> update) {
        Column current = table.columns().get(name);
        if (current != null) {
            table.replaceColumn(name, update.apply(current));
        }
    }

	@Override
	public Void visitCreateTable(CreateTableStatement statement) {
		// TODO Auto-generated method stub
		Table table = new Table(statement.tableName());

        for (ColumnDefinition column : statement.columns()) {
            table.addColumn(resolveColumn(column));
            foldColumnLevelTableEffects(table, column);
        }
        for (TableConstraint constraint : statement.constraints()) {
            table.addConstraint(constraint);
            if (constraint instanceof TableConstraint.PrimaryKey pk) {
                pk.columns().forEach(table::addPrimaryKeyColumn);
            }
            if (constraint instanceof TableConstraint.Unique unique) {
            	if (unique.name().isEmpty() && unique.columns().size() == 1) {
            		updateColumn(table, unique.columns().get(0), c -> c.withUnique(true));
            	}
            }
        }

        resolveSchema(statement.tableName()).addTable(table);
		return null;
	}

	@Override
	public Void visitAlterTable(AlterTableStatement statement) {
		// TODO Auto-generated method stub
		Optional<Table> table = resolveSchema(statement.tableName()).findTable(statement.tableName());
        if (table != null) {
        	applyAlterAction(table.get(), statement.action());
        }
        
		return null;
	}

	@Override
	public Void visitDropTable(DropTableStatement statement) {
		// TODO Auto-generated method stub
		for (QualifiedName name : statement.tableNames()) {
			resolveSchema(name).removeTable(name);
        }
		return null;
	}

	@Override
	public Void visitCreateIndex(CreateIndexStatement statement) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitDropIndex(DropIndexStatement statement) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitCreateView(CreateViewStatement statement) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitDropView(DropViewStatement statement) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitCreateSchema(CreateSchemaStatement statement) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitDropSchema(DropSchemaStatement statement) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitCreateFunction(CreateFunctionStatement statement) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitDropFunction(DropFunctionStatement statement) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitCreateProcedure(CreateProcedureStatement statement) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitDropProcedure(DropProcedureStatement statement) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitCreateType(CreateTypeStatement statement) {
		// TODO Auto-generated method stub
		if (statement.definition() instanceof TypeDefinition.Enum _enum) {
			resolveSchema(statement.typeName()).addEnum(new za.co.sindi.sql.sql2j.codegen.metamodel.Enum(statement.typeName(), _enum.labels()));
		}
		return null;
	}

	@Override
	public Void visitDropType(DropTypeStatement statement) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitCreateTrigger(CreateTriggerStatement statement) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitDropTrigger(DropTriggerStatement statement) {
		// TODO Auto-generated method stub
		return null;
	}
}
