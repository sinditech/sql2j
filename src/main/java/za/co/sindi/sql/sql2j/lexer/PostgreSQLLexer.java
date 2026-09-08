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
public class PostgreSQLLexer extends SQLLexer {

	private static final Set<String> RESERVED_KEYWORDS = Set.of("ANALYSE", 
																"ANALYZE", 
																"AND", 
																"ANY", 
																"ASC",
																"ASYMMETRIC",
																"AUTHORIZATION",
																"BINARY",
																"BOTH",
																"CASE",
																"CAST",
																"COLLATE",
																"COLLATION", 
																"CONCURRENTLY", 
																"CONSTRAINT",
																"CROSS",
																"CURRENT_CATALOG",
																"CURRENT_DATE",
																"CURRENT_ROLE",
																"CURRENT_SCHEMA",
																"CURRENT_TIME",
																"CURRENT_TIMESTAMP",
																"CURRENT_USER",
																"DEFERRABLE", 
																"DESC",
																"DO",
																"END",
																"EXCEPT",
//																"EXCLUDE", 
																"FALSE",
																"FETCH",
																"FOREIGN", 
																"FREEZE", 
																"FULL",
																"ILIKE", 
																"INNER", 
																"INITIALLY",
																"ISNULL", 
																"JOIN",
																"LATERAL",
																"LEADING",
																"LEFT",
																"LIMIT",
																"LOCALTIME",
																"LOCALTIMESTAMP",
																"NATURAL",
																"NOTNULL",
																"OFFSET",
																"ONLY", 
																"OUTER",
																"OVERLAPS",
																"PLACING", 
																"PRIMARY",
																"REFERENCES",
																"RETURNING",
																"RIGHT",
																"SESSION_USER",
																"SIMILAR", 
																"SOME",
																"SYMMETRIC",
																"SYSTEM_USER",
																"TABLESAMPLE",
																"TRAILING",
																"TRUE",
																"USER",
																"USING",
																"VARIADIC", 
																"VERBOSE",
																"WHEN",
																"WINDOW");
	
	/**
	 * @param reader
	 */
	public PostgreSQLLexer(Reader reader) {
		super(reader);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected Token scanToken(int startLine, int startColumn) {
		// TODO Auto-generated method stub
		if (peek() == '\'') return new Token.StringLiteral(scanStringLiteral(), startLine, startColumn);
		
		if (peek() == '"') return new Token.QuotedIdentifier("\"", scanQuotedIdentifier(startLine, startColumn, '"'), "\"", startLine, startColumn);
		
		if (peek() == '$') return scanDollarQuotedString(startLine, startColumn);
		
		if (peek() == 'N' && peek(1) == '\'') {
			nextChar();
			return new Token.QuotedStringLiteral(QuoteStyle.NATIONAL, "N'", scanStringLiteral(), "'", startLine, startColumn);
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
			case "<>", "!=", "<=", ">=" -> { //Global operators
                nextChar();
                return new Token.Symbol(TokenType.OPERATOR, sb.toString(), startLine, startColumn);
            }
			
			case "||" -> {
                nextChar();
                if (peek() == '/') sb.append(nextChar());
                return new Token.Symbol(TokenType.OPERATOR, sb.toString(), startLine, startColumn);
            }
			
			case "<<", ">>", "|/" -> { //PostgreSQL specific operators
                nextChar();
                return new Token.Symbol(TokenType.OPERATOR, sb.toString(), startLine, startColumn);
            }
			
			default -> {
                if ("=<>+-*/%~^&!@|#".indexOf(c) >= 0) {
                	return new Token.Symbol(TokenType.OPERATOR, String.valueOf(c), startLine, startColumn);
                }
                
                throw new LexException("Unexpected character '" + c + "'", startLine, startColumn);
            }
		}
	}
	
	/**
     * Scans a PostgreSQL-style dollar-quoted string: {@code $$ ... $$} or a tagged
     * {@code $tag$ ... $tag$}. The tag (if any) may contain letters, digits, and
     * underscores, and must not itself contain {@code $}. Everything between the
     * opening and matching closing delimiter is taken verbatim — this is exactly
     * why dollar-quoting exists: the body never needs internal escaping, so this
     * scanner never needs to understand what's inside it.
     */
    private Token scanDollarQuotedString(int startLine, int startColumn) {
        nextChar(); // opening '$'
        StringBuilder tagBuilder = new StringBuilder();
        while (hasNext() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
            tagBuilder.append(nextChar());
        }
        if (!hasNext() || peek() != '$') {
            throw new LexException("Malformed dollar-quote delimiter (expected closing '$' after '$" + tagBuilder + "')", startLine, startColumn);
        }
        nextChar(); // closing '$' of the opening delimiter
        String tag = tagBuilder.toString();
        String delimiter = "$" + tag + "$";
        
        StringBuilder bodyBuilder = new StringBuilder();
        while (hasNext() && !reachedClosingTag(delimiter)) {
        	bodyBuilder.append(nextChar());
        }
        
        if (!hasNext() || !reachedClosingTag(delimiter)) throw new LexException("Unterminated dollar-quoted string (delimiter '" + delimiter + "')", startLine, startColumn);
        for (int i = 0; i < delimiter.length(); i++) nextChar();

        return new Token.QuotedStringLiteral("$$".equals(delimiter) ? QuoteStyle.DOLLAR_QUOTE : QuoteStyle.DOLLAR_TAGGED, delimiter, bodyBuilder.toString(), startLine, startColumn);
    }
    
    private boolean reachedClosingTag(final String tag) {
    	for (int i = 0; i < tag.length(); i++) {
    		if (peek(i) != tag.charAt(i)) return false;
    	}
    	
    	return true;
    }
}
