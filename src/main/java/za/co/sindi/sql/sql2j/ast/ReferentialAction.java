package za.co.sindi.sql.sql2j.ast;

/** Action taken on a foreign key's parent row change, per {@code ON DELETE}/{@code ON UPDATE}. */
public enum ReferentialAction {
    CASCADE, SET_NULL, SET_DEFAULT, RESTRICT, NO_ACTION
}
