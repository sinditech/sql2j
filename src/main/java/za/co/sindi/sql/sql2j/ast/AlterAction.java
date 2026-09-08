package za.co.sindi.sql.sql2j.ast;

/** One clause of an {@code ALTER TABLE} statement, e.g. {@code ADD COLUMN ...}. */
public sealed interface AlterAction {

    record AddColumn(ColumnDefinition column, boolean ifNotExists) implements AlterAction {}

    record DropColumn(String columnName, boolean ifExists) implements AlterAction {}

    record RenameColumn(String from, String to) implements AlterAction {}

    record AlterColumnType(String columnName, DataType newType) implements AlterAction {}

    record AlterColumnSetDefault(String columnName, Expression value) implements AlterAction {}

    record AlterColumnDropDefault(String columnName) implements AlterAction {}

    record AlterColumnSetNotNull(String columnName) implements AlterAction {}

    record AlterColumnDropNotNull(String columnName) implements AlterAction {}

    record RenameTable(QualifiedName newName) implements AlterAction {}

    record AddConstraint(TableConstraint constraint) implements AlterAction {}

    record DropConstraint(String constraintName, boolean ifExists) implements AlterAction {}
}
