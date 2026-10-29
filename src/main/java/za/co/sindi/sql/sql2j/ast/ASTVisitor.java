package za.co.sindi.sql.sql2j.ast;

/**
 * The classic GoF <b>Visitor</b> pattern, adapted to modern Java.
 * <p>
 * Rather than requiring every AST record to implement {@code accept(Visitor)}
 * (which would mean putting parser/print-related plumbing inside otherwise
 * plain data records), dispatch is centralized in {@link #dispatch}, which
 * uses a pattern-matching {@code switch} (JEP 441) over the sealed
 * {@link Statement} hierarchy. The compiler still guarantees exhaustiveness
 * &mdash; the switch does not compile if a {@code Statement} subtype is
 * missing a case &mdash; so this keeps every classic Visitor benefit
 * (add new operations without touching the AST classes; compile-time
 * completeness checking) while removing the double-dispatch boilerplate.
 *
 * @param <R> the result type produced by visiting a statement
 */
public interface ASTVisitor<R> {

	default R visitCreateTable(CreateTableStatement statement) { return null; }

    default R visitAlterTable(AlterTableStatement statement) { return null; }

    default R visitDropTable(DropTableStatement statement) { return null; }

    default R visitCreateIndex(CreateIndexStatement statement) { return null; }

    default R visitDropIndex(DropIndexStatement statement) { return null; }

    default R visitCreateView(CreateViewStatement statement) { return null; }

    default R visitDropView(DropViewStatement statement) { return null; }

    default R visitCreateSchema(CreateSchemaStatement statement) { return null; }

    default R visitDropSchema(DropSchemaStatement statement) { return null; }

    default R visitCreateFunction(CreateFunctionStatement statement) { return null; }

    default R visitDropFunction(DropFunctionStatement statement) { return null; }

    default R visitCreateProcedure(CreateProcedureStatement statement) { return null; }

    default R visitDropProcedure(DropProcedureStatement statement) { return null; }

    default R visitCreateType(CreateTypeStatement statement) { return null; }

    default R visitDropType(DropTypeStatement statement) { return null; }

    default R visitCreateTrigger(CreateTriggerStatement statement) { return null; }

    default R visitDropTrigger(DropTriggerStatement statement) { return null; }

//    default R visitSelect(SelectStatement statement) { return null; }
//
//    default R visitUpdate(UpdateStatement statement) { return null; }
//
//    default R visitDelete(DeleteStatement statement) { return null; }

    /** Dispatches {@code statement} to the matching {@code visitXxx} method. */
    static <R> R dispatch(Statement statement, ASTVisitor<R> visitor) {
        return switch (statement) {
        	case CreateTableStatement s -> visitor.visitCreateTable(s);
            case AlterTableStatement s -> visitor.visitAlterTable(s);
            case DropTableStatement s -> visitor.visitDropTable(s);
            case CreateIndexStatement s -> visitor.visitCreateIndex(s);
            case DropIndexStatement s -> visitor.visitDropIndex(s);
            case CreateViewStatement s -> visitor.visitCreateView(s);
            case DropViewStatement s -> visitor.visitDropView(s);
            case CreateSchemaStatement s -> visitor.visitCreateSchema(s);
            case DropSchemaStatement s -> visitor.visitDropSchema(s);
            case CreateFunctionStatement s -> visitor.visitCreateFunction(s);
            case DropFunctionStatement s -> visitor.visitDropFunction(s);
            case CreateProcedureStatement s -> visitor.visitCreateProcedure(s);
            case DropProcedureStatement s -> visitor.visitDropProcedure(s);
            case CreateTypeStatement s -> visitor.visitCreateType(s);
            case DropTypeStatement s -> visitor.visitDropType(s);
            case CreateTriggerStatement s -> visitor.visitCreateTrigger(s);
            case DropTriggerStatement s -> visitor.visitDropTrigger(s);
//            case SelectStatement s -> visitor.visitSelect(s);
//            case UpdateStatement s -> visitor.visitUpdate(s);
//            case DeleteStatement s -> visitor.visitDelete(s);
            default -> throw new IllegalStateException("Unable to dispatch statement: " + statement);
        };
    }
}
