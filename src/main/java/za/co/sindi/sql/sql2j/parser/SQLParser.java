/**
 * 
 */
package za.co.sindi.sql.sql2j.parser;

import za.co.sindi.sql.sql2j.ast.AlterStatement;
import za.co.sindi.sql.sql2j.ast.CreateStatement;
import za.co.sindi.sql.sql2j.ast.DMLStatement;
import za.co.sindi.sql.sql2j.ast.DQLStatement;
import za.co.sindi.sql.sql2j.ast.DropStatement;
import za.co.sindi.sql.sql2j.ast.QualifiedName;
import za.co.sindi.sql.sql2j.ast.Statement;
import za.co.sindi.sql.sql2j.ast.TruncateStatement;
import za.co.sindi.sql.sql2j.lexer.Lexer;
import za.co.sindi.sql.sql2j.lexer.Token;
import za.co.sindi.sql.sql2j.lexer.TokenType;

/**
 * @author Buhake Sindi
 * @since 03 August 2026
 */
public abstract class SQLParser extends Parser {

	/**
	 * @param lexer
	 */
	protected SQLParser(Lexer lexer) {
		super(lexer);
	}
	
	protected String parseIdentifierName() {
        Token token = next();
        if (token.type() == TokenType.IDENTIFIER || token.type() == TokenType.QUOTED_IDENTIFIER) {
            return token.value();
        }
        throw error("Expected an identifier but found " + token);
    }
	
	protected QualifiedName parseQualifiedName() {
        String first = parseIdentifierName();
        if (match(TokenType.DOT)) {
        	String second = parseIdentifierName();
        	if (match(TokenType.DOT)) 
        		return QualifiedName.of(first, second, parseIdentifierName());
        	
        	return QualifiedName.of(first, second);
        }
        
        return QualifiedName.of(first);
    }
	
	protected Statement parseStatement() {
		//DDL
		if (matchKeyword("CREATE")) return parseCreateStatement();
        if (matchKeyword("ALTER")) return parseAlterStatement();
        if (matchKeyword("DROP")) return parseDropStatement();
        if (matchKeyword("TRUNCATE")) return parseTruncateStatement();
        
        //DQL
        if (matchKeyword("SELECT")) return parseSelectStatement();
        
        //DML
        if (matchKeyword("INSERT")) return parseInsertStatement();
        if (matchKeyword("UPDATE")) return parseUpdateStatement();
        if (matchKeyword("DELETE")) return parseDeleteStatement();
        
        throw error("Expected a DDL statement starting with CREATE, ALTER, DROP, TRUNCATE, SELECT, INSERT, UPDATE or DELETE");
	}
	
	//DDL
	protected abstract CreateStatement parseCreateStatement();
	protected abstract AlterStatement parseAlterStatement();
	protected abstract DropStatement parseDropStatement();
	protected abstract TruncateStatement parseTruncateStatement();
	
	//DQL
	protected abstract DQLStatement parseSelectStatement();
	
	//DML
	protected abstract DMLStatement parseInsertStatement();
	protected abstract DMLStatement parseUpdateStatement();
	protected abstract DMLStatement parseDeleteStatement();
}
