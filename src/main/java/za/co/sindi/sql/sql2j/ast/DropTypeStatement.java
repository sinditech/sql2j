package za.co.sindi.sql.sql2j.ast;

import java.util.List;

/** {@code DROP TYPE [IF EXISTS] name [, name...] [CASCADE|RESTRICT]}. */
public record DropTypeStatement(
        List<QualifiedName> typeNames,
        boolean ifExists,
        CascadeOption cascadeOption
) implements DropStatement {}
