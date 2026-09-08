package za.co.sindi.sql.sql2j.ast;

/** {@code ALTER TABLE name <action>}. */
public record AlterTableStatement(QualifiedName tableName, AlterAction action) implements AlterStatement {}
