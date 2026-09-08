package za.co.sindi.sql.sql2j.ast;

/** PostgreSQL-style function volatility, affecting how aggressively the optimizer may cache/reorder calls. */
public enum VolatilityCategory {
    IMMUTABLE, STABLE, VOLATILE
}
