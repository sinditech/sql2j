package za.co.sindi.sql.sql2j.ast;

import java.util.List;
import java.util.Optional;

/** {@code DROP FUNCTION [IF EXISTS] name [(param_type, ...)] [CASCADE|RESTRICT]}. */
public record DropFunctionStatement(
        QualifiedName functionName,
        boolean ifExists,
        Optional<List<DataType>> parameterTypes,
        CascadeOption cascadeOption
) implements DropStatement {}
