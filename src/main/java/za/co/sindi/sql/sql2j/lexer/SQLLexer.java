/**
 * 
 */
package za.co.sindi.sql.sql2j.lexer;

import java.io.Reader;
import java.util.Set;

/**
 * 
 * @author Buhake Sindi
 * @since 29 June 2026
 */
public abstract class SQLLexer extends Lexer {
	
	/**
	 * @param reader
	 */
	public SQLLexer(Reader reader) {
		super(reader);
		// TODO Auto-generated constructor stub
	}

	/** Reserved words recognized as {@link TokenType#KEYWORD}. */
    private static final Set<String> GLOBAL_RESERVED_KEYWORDS = Set.of("ALL",
//    		"AND",
    		"AS",
//    		"ASC",
//    		"BETWEEN",
//    		"BY",
//    		"CASE",
    		"CHECK",
    		"COLUMN",
    		"CREATE",
    		"DEFAULT",
//    		"DELETE",
//    		"DESC",
    		"DISTINCT",
//    		"DROP",
    		"ELSE",
//    		"END",
//    		"EXISTS",
//    		"FALSE",
    		"FOR",
    		"FROM",
//    		"FULL",
    		"GRANT",
    		"GROUP",
    		"HAVING",
    		"IN",
//    		"INNER",
    		"INSERT",
    		"INTERSECT",
    		"INTO",
    		"IS",
//    		"JOIN",
//    		"LEFT",
    		"LIKE",
    		"NOT",
    		"NULL",
    		"ON",
    		"OR",
    		"ORDER",
//    		"OUTER",
//    		"PRIMARY",
//    		"REFERENCES",
//    		"RIGHT",
    		"SELECT",
    		"SET",
    		"TABLE",
    		"THEN",
    		"TO",
    		"TRUE",
    		"UNION",
    		"UNIQUE",
//    		"UPDATE",
    		"VALUES",
//    		"WHEN",
    		"WHERE",
    		"WITH"
    );

	/**
	 * @return the globalReservedKeywords
	 */
	protected static Set<String> getGlobalReservedKeywords() {
		return GLOBAL_RESERVED_KEYWORDS;
	}
	
	protected void skipCommentsAndWhitespaces() {
		while (true) {
			char c = peek();
			if (Character.isWhitespace(c)) nextChar();
			else if (c == '-' && peek(1) == '-') {
				while(peek() != '\n') nextChar();
			} else if (c == '/' && peek(1) != '*') {
				nextChar();
				nextChar();
				while(c != NULL && c != '*' && peek(1) != '/') nextChar();
				nextChar();
				nextChar();
			} else break;
		}
	}
	
	protected String scanIdentifierToken(int startLine, int startColumn) {
		StringBuilder sb = new StringBuilder();
		while (hasNext() && isIdentifierPart(peek())) sb.append(nextChar());
		
		return sb.toString();
	}
	
	protected String scanQuotedIdentifier(int startLine, int startColumn, char endCharacter) {
		nextChar();
		
		StringBuilder sb = new StringBuilder();
		while (hasNext() && peek() != endCharacter) sb.append(nextChar());
		if (peek() == endCharacter) nextChar();
		return sb.toString();
	}
	
	protected String scanNumber(int startLine, int startColumn) {
		char c = peek();
		if (c == '0' && Character.toUpperCase(peek(1)) == 'B') {
			return new StringBuilder().append(nextChar()).append(nextChar()).append(scanBinaryNumber()).toString();
		}
		
		if (c == '0' && Character.toUpperCase(peek(1)) == 'X') {
			return new StringBuilder().append(nextChar()).append(nextChar()).append(scanHexadecimalNumber()).toString();
		}
			
		StringBuilder sb = new StringBuilder();
        int pos = 0;
        while (hasNext() && Character.isDigit(peek())) {
        	sb.append(nextChar());
        	pos++;
        }
        
        if (pos > 1 && peek() == '.' && Character.isDigit(peek(1))) {
        	sb.append(nextChar());
        	pos++;
        	while (hasNext() && Character.isDigit(peek())) {
            	sb.append(nextChar());
            	pos++;
            }
        }
        
        if (pos > 1 && (peek() == 'e' || peek() == 'E')) {
        	sb.append(nextChar());
        	pos++;
        	if (hasNext() && (peek() == '+' || peek() == '-')) {
            	sb.append(nextChar());
            	pos++;
            }
        	
        	while (hasNext() && Character.isDigit(peek())) {
            	sb.append(nextChar());
            	pos++;
            }
        }
        
        return sb.toString();
	}
	
	protected String scanBinaryNumber() {
		StringBuilder sb = new StringBuilder();
		while (hasNext() && isBinaryDigit(peek())) sb.append(nextChar());
	
		return sb.toString();
	}
	
	protected String scanHexadecimalNumber() {
		StringBuilder sb = new StringBuilder();
		while (hasNext() && isHexDigit(peek())) sb.append(nextChar());
	
		return sb.toString();
	}
	
	protected String scanStringLiteral() {
		nextChar();
		
		StringBuilder sb = new StringBuilder();
		while (hasNext() && peek() != '\'') sb.append(nextChar());
		if (peek() == '\'' && peek(1) == '\'') {
			sb.append(nextChar());
			nextChar();
			while (hasNext() && peek() != '\'') sb.append(nextChar());
		}
		if (peek() == '\'') nextChar();
		
		return sb.toString();
	}
	
	private boolean isIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }
    
    private static boolean isBinaryDigit(char c) {
        return (c == '0' || c == '1');
    }
    
    private static boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }
}
