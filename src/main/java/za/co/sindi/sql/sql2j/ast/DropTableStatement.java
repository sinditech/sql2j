package za.co.sindi.sql.sql2j.ast;

import java.util.List;

/** {@code DROP TABLE [IF EXISTS] name [, name...] [CASCADE|RESTRICT]}. */
public record DropTableStatement(
        List<QualifiedName> tableNames,
        boolean ifExists,
        CascadeOption cascadeOption
) implements DropStatement {}
