package za.co.sindi.sql.sql2j.ast;

import java.util.Optional;

/**
 * One parameter in a {@code CREATE FUNCTION}/{@code CREATE PROCEDURE} parameter
 * list. {@code name} is optional because SQL permits unnamed, type-only
 * parameters (e.g. {@code CREATE FUNCTION f(int, text) RETURNS ...}).
 */
public record RoutineParameter(ParameterMode mode, Optional<String> name, DataType dataType, Optional<Expression> defaultValue) {}
