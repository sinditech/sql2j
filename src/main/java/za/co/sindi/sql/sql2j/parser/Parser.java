/**
 * 
 */
package za.co.sindi.sql.sql2j.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import za.co.sindi.sql.sql2j.ast.Statement;
import za.co.sindi.sql.sql2j.lexer.Lexer;
import za.co.sindi.sql.sql2j.lexer.Token;
import za.co.sindi.sql.sql2j.lexer.TokenType;

/**
 * @author Buhake Sindi
 * @since 03 August 2026
 */
public abstract class Parser {

	private final Lexer lexer;
    private final List<Token> lookaheadBuffer = new ArrayList<>();
	
	/**
	 * @param lexer
	 */
	protected Parser(Lexer lexer) {
		super();
		this.lexer = Objects.requireNonNull(lexer, "A SQL Lexer is required.");
	}

	protected Token peek() {
        return peek(0);
    }

    protected Token peek(int distance) {
        while (lookaheadBuffer.size() <= distance) {
            lookaheadBuffer.add(lexer.hasNext()
                    ? lexer.next()
                    : new Token.EOF(-1, -1)); // new Token(TokenType.EOF, null, -1, -1)
        }
        return lookaheadBuffer.get(distance);
    }
	
	protected boolean hasNext() {
		return peek().type() != TokenType.EOF;
	}
	
	protected Token next() {
		Token token = peek();
		if (!lookaheadBuffer.isEmpty()) lookaheadBuffer.remove(0);
		return token;
	}
	
	protected boolean check(TokenType type) {
        return peek().type() == type;
    }

    protected Token expect(TokenType type, String message) {
        if (!check(type)) {
            throw error(message);
        }
        return next();
    }

    protected boolean match(TokenType type) {
        if (check(type)) {
            next();
            return true;
        }
        return false;
    }
	
	protected boolean checkKeyword(String keyword) {
        Token token = peek();
        return token.type() == TokenType.KEYWORD && token.value().equalsIgnoreCase(keyword);
    }

    protected void expectKeyword(String keyword) {
        if (!matchKeyword(keyword)) {
            throw error("Expected keyword " + keyword);
        }
    }
	
	protected boolean matchKeyword(String keyword) {
        if (checkKeyword(keyword)) {
            next();
            return true;
        }
        return false;
    }
	
	protected boolean checkIdentifier(String keyword) {
        Token token = peek();
        return token.type() == TokenType.IDENTIFIER && token.value().equalsIgnoreCase(keyword);
    }
	
	protected void expectIdentifier(String identifier) {
        if (!matchIdentifier(identifier)) {
            throw error("Expected identifier " + identifier);
        }
    }
	
	protected boolean matchIdentifier(String identifier) {
        if (check(TokenType.IDENTIFIER) && peek().value().equals(identifier)) {
            next();
            return true;
        }
        return false;
    }
	
    protected boolean matchOperator(String operatorText) {
        if (check(TokenType.OPERATOR) && peek().value().equals(operatorText)) {
            next();
            return true;
        }
        return false;
    }
	
	protected ParseException error(String message) {
        Token token = peek();
        return new ParseException(message + " -- found " + token, token.line(), token.column());
    }
	
	protected void skipStraySemicolons() {
        while (check(TokenType.SEMICOLON)) {
            next();
        }
    }
	
	public List<Statement> parse() {
		List<Statement> statements = new ArrayList<>();
		skipStraySemicolons();
		
		while (hasNext()) {
			statements.add(parseStatement());
			skipStraySemicolons();
		}
		return Collections.unmodifiableList(statements);
	}
	
	protected abstract Statement parseStatement();
}
