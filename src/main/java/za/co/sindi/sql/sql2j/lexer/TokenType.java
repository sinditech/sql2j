/**
 * 
 */
package za.co.sindi.sql.sql2j.lexer;

/**
 * @author Buhake Sindi
 * @since 28 June 2026
 */
public enum TokenType {
	KEYWORD,
    IDENTIFIER,
    QUOTED_IDENTIFIER,
//    DOLLAR_QUOTED_STRING,
    STRING_LITERAL,
    QUOTED_STRING,
    NUMBER_LITERAL,
    OPERATOR,
    COMMA,
    LPAREN,
    RPAREN,
    SEMICOLON,
    DOT,
    EOF,
//    UNKNOWN
}
