package za.co.sindi.sql.sql2j.ast;

import java.util.List;

/** {@code CREATE [OR REPLACE] PROCEDURE name(params) [characteristics] AS body}. Like {@link CreateFunctionStatement} but with no return type. */
public record CreateProcedureStatement(
        QualifiedName procedureName,
        boolean orReplace,
        List<RoutineParameter> parameters,
        List<RoutineCharacteristic> characteristics,
        RoutineBody body
) implements CreateStatement {}
