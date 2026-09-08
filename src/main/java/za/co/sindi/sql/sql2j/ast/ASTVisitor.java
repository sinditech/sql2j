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

	R visitCreateTable(CreateTableStatement statement);

    R visitAlterTable(AlterTableStatement statement);

    R visitDropTable(DropTableStatement statement);

    R visitCreateIndex(CreateIndexStatement statement);

    R visitDropIndex(DropIndexStatement statement);

    R visitCreateView(CreateViewStatement statement);

    R visitDropView(DropViewStatement statement);

    R visitCreateSchema(CreateSchemaStatement statement);

    R visitDropSchema(DropSchemaStatement statement);

    R visitCreateFunction(CreateFunctionStatement statement);

    R visitDropFunction(DropFunctionStatement statement);

    R visitCreateProcedure(CreateProcedureStatement statement);

    R visitDropProcedure(DropProcedureStatement statement);

    R visitCreateType(CreateTypeStatement statement);

    R visitDropType(DropTypeStatement statement);

    R visitCreateTrigger(CreateTriggerStatement statement);

    R visitDropTrigger(DropTriggerStatement statement);

//    R visitSelect(SelectStatement statement);
//
//    R visitUpdate(UpdateStatement statement);
//
//    R visitDelete(DeleteStatement statement);

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
