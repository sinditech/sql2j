/**
 * 
 */
package za.co.sindi.sql.sql2j;

import java.io.Reader;
import java.io.StringReader;

import za.co.sindi.sql.sql2j.lexer.PostgreSQLLexer;
import za.co.sindi.sql.sql2j.parser.PostgreSQLParser;
import za.co.sindi.sql.sql2j.parser.SQLParser;

/**
 * @author Buhake Sindi
 * @since 09 August 2026
 */
public interface SQLParserFactory {
	
	default SQLParser createParser(final String source) {
		return createParser(new StringReader(source));
	}

	public SQLParser createParser(final Reader reader);
	
	public static SQLParserFactory postgresql() {
		return new SQLParserFactory() {

			@Override
			public PostgreSQLParser createParser(Reader reader) {
				// TODO Auto-generated method stub
				return new PostgreSQLParser(new PostgreSQLLexer(reader));
			}
		};
	}
}
