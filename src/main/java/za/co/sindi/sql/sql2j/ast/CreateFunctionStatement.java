package za.co.sindi.sql.sql2j.ast;

import java.util.List;

/** {@code CREATE [OR ALTER|REPLACE] FUNCTION name(params) RETURNS type [characteristics] AS body}. */
public record CreateFunctionStatement(
        QualifiedName functionName,
        FunctionModifier modifier,
        List<RoutineParameter> parameters,
        RoutineReturnType returnType,
        List<RoutineCharacteristic> characteristics,
        RoutineBody body
) implements CreateStatement {}
