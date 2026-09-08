/**
 * 
 */
package za.co.sindi.sql.sql2j.lexer;

import java.io.Reader;
import java.util.Set;

import za.co.sindi.sql.sql2j.SQLDialect;
import za.co.sindi.sql.sql2j.utils.SQLNumberConverter;

/**
 * @author Buhake Sindi
 * @since 11 August 2026
 */
public class MySQLLexer extends SQLLexer {

	private static final Set<String> RESERVED_KEYWORDS = Set.of("ACCESSIBLE", 
																"ADD",
																"ALTER",
																"ANALYZE", 
																"AND", 
																"ASC",
																"ASENSITIVE",
																"BEFORE",
																"BETWEEN",
																"BIGINT",
																"BINARY",
																"BLOB",
																"BOTH",
																"BY",
																"CALL",
																"CASCADE",
																"CASE",
																"CHANGE",
																"CHAR",
																"CHARACTER",
																"COLLATE",
																"CONDITION", 
																"CONSTRAINT",
																"CONTINUE",
																"CONVERT",
																"CROSS",
																"CUBE",
																"CUME_DIST",
																"CURRENT",
																"CURRENT_DATE",
																"CURRENT_TIME",
																"CURRENT_TIMESTAMP",
																"CURRENT_USER",
																"DATABASE",
																"DATABASES",
																"DAY_HOUR", "DAY_MICROSECOND", "DAY_MINUTE", "DAY_SECOND",
																"DEC",
																"DECIMAL",
																"DECLARE",
																"DELAYED",
																"DENSE_RANK",
																"DESC",
																"DESCRIBE",
																"DETERMINISTIC",
																"DISTINCTROW",
																"DIV",
//																"DO",
																"DOUBLE",
																"DUAL",
																"EACH",
																"ELSE",
																"ELSEIF",
																"EMPTY",
																"ENCLOSED",
//																"END",
																"ESCAPED",
																"EXCEPT",
																"EXISTS",
																"EXIT",
																"EXPLAIN",
																"EXTERNAL",
																"FALSE",
																"FETCH",
																"FIRST_VALUE",
																"FLOAT", "FLOAT4", "FLOAT8",
																"FORCE",
																"FOREIGN", 
																"FULLTEXT",
																"FUNCTION",
																"GENERAATED",
																"GET",
																"GROUPINP",
																"GROUPS",
																"HIGH_PRIORITY",
																"HOUR_MICROSECOND",
																"HOUR_MINUTE",
																"HOUR_SECOND",
																"IF",
																"IGNORE",
																"INDEX", 
																"INFILE",
																"INNER",
																"INOUT",
																"INSENSITIVE",
																"INSERT",
																"INT", "INT1", "INT2", "INT3", "INT4", "INT8", "INTEGER",
																"INTERVAL",
																"IO_AFTER_GTIDS", "IO_BEFORE_GTIDS",
																"IS",
																"ITERATE",
																"JOIN",
																"JSON_TABLE",
																"KEY",
																"KEYS",
																"KILL",
																"LAG",
																"LAST_VALUE",
																"LATERAL",
																"LEAD",
																"LEADING",
																"LEAVE",
																"LEFT",
																"LIMIT",
																"LINEAR",
																"LINES",
																"LOAD",
																"LOCALTIME",
																"LOCALTIMESTAMP",
																"LOCK",
																"LONG",
																"LONGBLOB",
																"LONGTEXT",
																"LOOP",
																"LOW_PRIORITY",
																"MASTER_BIND",
																"MASTER_SSL_VERIFY_SERVER_CERT",
																"MATCHES",
																"MAXVALUE",
																"MEDIUMBLOB", "MEDIUMINT", "MEDIUMTEXT",
																"MIDDLEINT",
																"MINUTE_MICROSECOND", "MINUTE_SECOND",
																"MOD",
																"MODIFIES",
																"NATURAL",
																"NO_WRITE_TO_BINLOG",
																"NTH_VALUE",
																"NTILE",
																"NUMERIC",
																"OF",
																"OPTIMIZE", 
																"OPTIMIZER_COSTS",
																"OPTION",
																"OPTIONALLY",
																"OUT",
																"OUTER",
																"OUTFILE",
																"OVER",
																"PARTITION", 
																"PERCENT_RANK",
																"PRECISION",
																"PRIMARY",
																"PROCEDURE",
																"PURGE",
																"RANGE",
																"RANK", 
																"READ",
																"READS",
																"READ_WRITE",
																"REAL",
																"RECURSIVE",
																"REFERENCES",
																"REGEXP",
																"RELEASE",
																"RENAME",
																"REPEAT",
																"REPLACE",
																"REQUIRE",
																"RESIGNAL",
																"RESTRICT",
																"RETURN",
																"REVOKE",
																"RIGHT",
																"RLIKE",
																"ROW", "ROWS", "ROW_NUMBER",
																"SCHEMA", "SCHEMAS",
																"SECOND_MICROSECOND",
																"SENSITIVE",
																"SEPARATOR",
																"SHOW",
																"SIGNAL",
																"SMALLINT",
																"SPATIAL", 
																"SPECIFIC",
																"SQL", "SQLEXCEPTION", "SQLSTATE", 
																"SQLWARNING",
																"SQL_BIG_RESULT",
																"SQL_CALC_FOUND_ROWS",
																"SQL_SMALL_RESULT", 
																"SSL",
																"STARTING",
																"STORED",
																"STRAIGHT_JOIN",
																"SYSTEM",
																"TERMINATED",
																"TINYBLOB", "TINYINT", "TINYTEXT",
																"TRAILING",
																"TRIGGER",
																"TRUE",
																"UNDO", 
																"UNLOCK",
																"UNSIGNED",
																"UPDATE",
																"USAGE",
																"USE",
																"USING",
																"UTC_DATE", "UTC_TIME", "UTC_TIMESTAMP",
																"VARBINARY", 
																"VARCHAR", "VARCHARACTER",
																"VARYING",
																"VIRTUAL",
																"WHEN",
																"WHILE",
																"WINDOW",
																"WRITE",
																"XOR",
																"YEAR_MONTH",
																"ZEROFILL");
	
	/**
	 * @param reader
	 */
	public MySQLLexer(Reader reader) {
		super(reader);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected Token scanToken(int startLine, int startColumn) {
		// TODO Auto-generated method stub
		if (peek() == '\'') return new Token.StringLiteral(scanStringLiteral(), startLine, startColumn);
		
		if (peek() == '"') return new Token.QuotedIdentifier("\"", scanQuotedIdentifier(startLine, startColumn, '"'), "\"", startLine, startColumn);
		
		if (peek() == 'N' && peek(1) == '\'') {
			nextChar();
			return new Token.QuotedStringLiteral(QuoteStyle.NATIONAL, "N'", scanStringLiteral(), "'", startLine, startColumn);
		}
		
		if (peek() == 'X' && peek(1) == '\'') {
			nextChar();
			String value = scanStringLiteral();
			return new Token.NumericLiteral("X'" + value + "'", SQLNumberConverter.convertStringToNumber("0x" + value), startLine, startColumn);
		}
		
		if (isIdentifierStart(peek())) {
			String value = scanIdentifierToken(startLine, startColumn);
			if (RESERVED_KEYWORDS.contains(value.toUpperCase()) || getGlobalReservedKeywords().contains(value.toUpperCase())) {
				return new Token.Keyword(value.toUpperCase(), startLine, startColumn);
			}
			
			return new Token.Identifier(value, startLine, startColumn);
		}
		
		if (Character.isDigit(peek())) {
			String value = scanNumber(startLine, startColumn);
			return new Token.NumericLiteral(value, SQLNumberConverter.convertStringToNumber(value), startLine, startColumn);
		}
		
		return scanSymbols(startLine, startColumn);
	}

	@Override
	public SQLDialect getDialect() {
		// TODO Auto-generated method stub
		return SQLDialect.POSTGRESQL;
	}
	
	
	private boolean isIdentifierStart(char c) {
		return Character.isLetter(c) || c == '_';
	}
	
	private Token scanSymbols(int startLine, int startColumn) {
		char c = nextChar();
        return switch (c) {
            case '(' -> new Token.Symbol(TokenType.LPAREN, "(", startLine, startColumn);
            case ')' -> new Token.Symbol(TokenType.RPAREN, ")", startLine, startColumn);
            case ',' -> new Token.Symbol(TokenType.COMMA, ",", startLine, startColumn);
            case ';' -> new Token.Symbol(TokenType.SEMICOLON, ";", startLine, startColumn);
            case '.' -> new Token.Symbol(TokenType.DOT, ".", startLine, startColumn);
            default -> scanOperator(c, startLine, startColumn);
        };
	}
	
	private Token scanOperator(char c, int startLine, int startColumn) {
		StringBuilder sb = new StringBuilder();
		sb.append(c);
		if (peek() != NULL) sb.append(peek());
		
		switch (sb.toString()) {
			case "<>", "!=", ">=", "||" -> { //Global operators
                nextChar();
                return new Token.Symbol(TokenType.OPERATOR, sb.toString(), startLine, startColumn);
            }
			
			case "+=", "-=", "*=", "/=", "%=", "&=", "&&", ":=" -> { 
                nextChar();
                return new Token.Symbol(TokenType.OPERATOR, sb.toString(), startLine, startColumn);
            }
			
			case "<=" -> {
				nextChar();
				if (peek() == '>') sb.append(nextChar());
				return new Token.Symbol(TokenType.OPERATOR, sb.toString(), startLine, startColumn);
			}
			
			case "<<", ">>", "\\|" -> { //MySQL specific operators
                nextChar();
                return new Token.Symbol(TokenType.OPERATOR, sb.toString(), startLine, startColumn);
            }
			
			default -> {
				if (Set.of("^-", "|*").contains(sb.toString()) && peek(1) == '=') {
					nextChar();
					sb.append(nextChar());
					return new Token.Symbol(TokenType.OPERATOR, sb.toString(), startLine, startColumn);
				}
				
                if ("=<>+-*/%~^&!@|#".indexOf(c) >= 0) {
                	return new Token.Symbol(TokenType.OPERATOR, String.valueOf(c), startLine, startColumn);
                }
                
                throw new LexException("Unexpected character '" + c + "'", startLine, startColumn);
            }
		}
	}
}
