/**
 * 
 */
package za.co.sindi.sql.sql2j.lexer;

/**
 * @author Buhake Sindi
 * @since 06 August 2026
 */
public class LexException extends RuntimeException {

	private final int line;
    private final int column;
    
	/**
	 * @param message
	 * @param line
	 * @param column
	 */
	public LexException(String message, int line, int column) {
		super("%s (at line %d, column %d)".formatted(message, line, column));
		this.line = line;
		this.column = column;
	}

	/**
	 * @return the line
	 */
	public int getLine() {
		return line;
	}

	/**
	 * @return the column
	 */
	public int getColumn() {
		return column;
	}
}
