package za.co.sindi.sql.sql2j.ast;

import java.util.List;
import java.util.Optional;

/** {@code DROP PROCEDURE [IF EXISTS] name [(param_type, ...)] [CASCADE|RESTRICT]}. */
public record DropProcedureStatement(
        QualifiedName procedureName,
        boolean ifExists,
        Optional<List<DataType>> parameterTypes,
        CascadeOption cascadeOption
) implements DropStatement {}
