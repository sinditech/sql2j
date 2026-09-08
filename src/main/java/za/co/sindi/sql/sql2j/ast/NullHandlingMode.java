package za.co.sindi.sql.sql2j.ast;

/** How a routine behaves when called with {@code NULL} argument(s). */
public enum NullHandlingMode {
    /** {@code STRICT} (PostgreSQL shorthand): returns {@code NULL} immediately if any argument is {@code NULL}. */
    STRICT,
    /** {@code CALLED ON NULL INPUT}: the routine body runs even if an argument is {@code NULL}. */
    CALLED_ON_NULL_INPUT,
    /** {@code RETURNS NULL ON NULL INPUT}: equivalent in meaning to {@code STRICT}, spelled the SQL-standard way. */
    RETURNS_NULL_ON_NULL_INPUT
}
