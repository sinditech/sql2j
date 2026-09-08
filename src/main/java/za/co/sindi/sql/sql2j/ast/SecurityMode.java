package za.co.sindi.sql.sql2j.ast;

/** Whether a routine executes with the privileges of its definer or its caller. */
public enum SecurityMode {
    DEFINER, INVOKER
}
