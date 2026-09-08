/**
 * 
 */
package za.co.sindi.sql.sql2j.lexer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Stream;

import za.co.sindi.sql.sql2j.SQLDialect;

/**
 * Turns raw SQL DDL text into a lazily-produced sequence of {@link Token}s.
 *
 * <p>Design: this class is the classic Gang-of-Four <b>Iterator</b> pattern
 * applied to lexing. It never materializes the whole token list unless the
 * caller asks it to (via {@link #tokenizeAll(String)}); instead {@link #next()}
 * scans exactly one token from the underlying character buffer on demand.
 * That makes it possible to pipe a lexer directly into a {@link Stream}
 * ({@link #stream()}) or into the parser, so a multi-megabyte DDL script
 * never needs to live in memory as a token list before parsing starts.
 *
 * <p>Instances are single-use and <b>not</b> thread-safe, exactly like a
 * normal {@link Iterator}.
 * 
 * @author Buhake Sindi
 * @since 28 June 2026
 */
public abstract class Lexer implements Iterator<Token> {
	
	protected static final char NULL = '\0';
	
	private final Reader reader;
    private int line = 1;
    private int column = 1;
    
	/**
	 * @param reader
	 */
	protected Lexer(Reader reader) {
		super();
		this.reader = Objects.requireNonNull(reader, "A reader is required.") instanceof BufferedReader br ? br : new BufferedReader(reader);
	}

	@Override
	public boolean hasNext() {
		// TODO Auto-generated method stub
		return peek() != NULL;
	}
	
	@Override
	public Token next() {
		// TODO Auto-generated method stub
		if (!hasNext()) {
            throw new NoSuchElementException("No more tokens");
        }
		
		skipCommentsAndWhitespaces();
		int startLine = line;
        int startColumn = column;
        
        char c = peek();
        if (c == NULL) return new Token.EOF(startLine, startColumn); //new Token(TokenType.EOF, null, startLine, startColumn);
        return scanToken(startLine, startColumn);
	}
	
	public List<Token> tokenize() {
		List<Token> tokens = new ArrayList<>();
		while (hasNext()) tokens.add(next());
		return Collections.unmodifiableList(tokens);
	}
	
	protected char peek() {
		try {
			reader.mark(1);
			int c = reader.read();
			reader.reset();
			return c == -1 ? NULL : (char) c;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return NULL;
		}
	}
	
	protected char peek(int offset) {
        try {
			char[] buf = new char[offset + 1];
			int n = 0;
			
			reader.mark(offset + 1);
			while (n < buf.length) {
			    int c = reader.read();
			    if (c == -1) break;
			    buf[n++] = (char) c;
			}

			if (n > 0) {
				reader.reset();
			}
			
			return (char)((n <= offset) ? NULL : buf[offset]);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return NULL;
		}
    }
	
	protected char nextChar() {
		try {
			char c = (char)reader.read();
			if (c == '\r') column = 1;
			else if (c == '\n') line++;
			else column++;
			
			return c;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return NULL;
		}
	}
	

	protected abstract void skipCommentsAndWhitespaces();
	protected abstract Token scanToken(int startLine, int startColumn);
	public abstract SQLDialect getDialect();
}
