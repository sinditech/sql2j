/**
 * 
 */
package za.co.sindi.sql.sql2j.lexer;

/**
 * @author Buhake Sindi
 * @since 09 August 2026
 */
public interface Token {
	
	public TokenType type();
	
	public String value();
	
	public int line();
	
	public int column();
	
	public record EOF(int line, int column) implements Token {

		@Override
		public TokenType type() {
			// TODO Auto-generated method stub
			return TokenType.EOF;
		}

		@Override
		public String value() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return "EOF@%d:%d".formatted(line, column);
		}
	}
	
	public record Keyword(String value, int line, int column) implements Token {

		@Override
		public TokenType type() {
			// TODO Auto-generated method stub
			return TokenType.KEYWORD;
		}
		
		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return "KEYWORD('%s')@%d:%d".formatted(value, line, column);
		}
	}
	
	public record Identifier(String value, int line, int column) implements Token {

		@Override
		public TokenType type() {
			// TODO Auto-generated method stub
			return TokenType.IDENTIFIER;
		}
		
		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return "IDENTIFIER('%s')@%d:%d".formatted(value, line, column);
		}
	}
	
	public record QuotedIdentifier(String startDelimiter, String value, String endDelimiter, int line, int column) implements Token {

		@Override
		public TokenType type() {
			// TODO Auto-generated method stub
			return TokenType.QUOTED_IDENTIFIER;
		}
		
		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return "QUOTED_IDENTIFIER(%s%s%s)@%d:%d".formatted(startDelimiter, value, endDelimiter, line, column);
		}
	}
	
	public record StringLiteral(String value, int line, int column) implements Token {

		@Override
		public TokenType type() {
			// TODO Auto-generated method stub
			return TokenType.STRING_LITERAL;
		}
		
		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return "STRING_LITERAL('%s')@%d:%d".formatted(value, line, column);
		}
	}
	
	public record QuotedStringLiteral(QuoteStyle style, String startDelimiter, String value, String endDelimiter, int line, int column) implements Token {

		public QuotedStringLiteral(QuoteStyle style, String tag, String value, int line, int column) {
			this(style, tag, value, tag, line, column);
		}
		
		@Override
		public TokenType type() {
			// TODO Auto-generated method stub
			return TokenType.QUOTED_STRING;
		}
		
		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return "QUOTED_STRING(%s%s%s)@%d:%d".formatted(startDelimiter, value, endDelimiter, line, column);
		}
	}
	
	public record NumericLiteral(String value, Number numericValue, int line, int column) implements Token {

		@Override
		public TokenType type() {
			// TODO Auto-generated method stub
			return TokenType.NUMBER_LITERAL;
		}
		
		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return "NUMERIC_LITERAL('%s')@%d:%d".formatted(value, line, column);
		}
	}
	
	public record Symbol(TokenType type, String value, int line, int column) implements Token {

		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return "%s('%s')@%d:%d".formatted(type, value, line, column);
		}
	}
}
