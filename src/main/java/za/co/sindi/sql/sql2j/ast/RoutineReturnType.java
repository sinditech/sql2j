package za.co.sindi.sql.sql2j.ast;

import java.util.List;

/** What a {@code CREATE FUNCTION} returns: a single value, a set of rows of a scalar type, or a row set with named columns. */
public sealed interface RoutineReturnType {

    /** {@code RETURNS int}. */
    record Scalar(DataType dataType) implements RoutineReturnType {}

    /** {@code RETURNS SETOF int} (PostgreSQL). */
    record SetOf(DataType dataType) implements RoutineReturnType {}

    /** {@code RETURNS TABLE (col1 type1, col2 type2, ...)} (PostgreSQL table-valued function). */
    record Table(List<ColumnDefinition> columns) implements RoutineReturnType {}
}
