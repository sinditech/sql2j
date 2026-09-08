package za.co.sindi.sql.sql2j.ast;

import java.util.Optional;

/** One attribute of a {@code CREATE TYPE name AS (...)} composite type. */
public record CompositeAttribute(String name, DataType dataType, Optional<String> collation) {}
