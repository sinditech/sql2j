/**
 * 
 */
package za.co.sindi.sql.sql2j.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import za.co.sindi.sql.sql2j.ast.AlterAction;
import za.co.sindi.sql.sql2j.ast.AlterStatement;
import za.co.sindi.sql.sql2j.ast.AlterTableStatement;
import za.co.sindi.sql.sql2j.ast.ColumnConstraint;
import za.co.sindi.sql.sql2j.ast.ColumnDefinition;
import za.co.sindi.sql.sql2j.ast.CompositeAttribute;
import za.co.sindi.sql.sql2j.ast.CreateFunctionStatement;
import za.co.sindi.sql.sql2j.ast.CreateIndexStatement;
import za.co.sindi.sql.sql2j.ast.CreateProcedureStatement;
import za.co.sindi.sql.sql2j.ast.CreateSchemaStatement;
import za.co.sindi.sql.sql2j.ast.CreateStatement;
import za.co.sindi.sql.sql2j.ast.CreateTableStatement;
import za.co.sindi.sql.sql2j.ast.CreateTriggerStatement;
import za.co.sindi.sql.sql2j.ast.CreateTypeStatement;
import za.co.sindi.sql.sql2j.ast.CreateViewStatement;
import za.co.sindi.sql.sql2j.ast.DMLStatement;
import za.co.sindi.sql.sql2j.ast.DQLStatement;
import za.co.sindi.sql.sql2j.ast.DataType;
import za.co.sindi.sql.sql2j.ast.DropStatement;
import za.co.sindi.sql.sql2j.ast.Expression;
import za.co.sindi.sql.sql2j.ast.FunctionModifier;
import za.co.sindi.sql.sql2j.ast.NullHandlingMode;
import za.co.sindi.sql.sql2j.ast.ParameterMode;
import za.co.sindi.sql.sql2j.ast.QualifiedName;
import za.co.sindi.sql.sql2j.ast.ReferentialAction;
import za.co.sindi.sql.sql2j.ast.RoutineBody;
import za.co.sindi.sql.sql2j.ast.RoutineCharacteristic;
import za.co.sindi.sql.sql2j.ast.RoutineParameter;
import za.co.sindi.sql.sql2j.ast.RoutineReturnType;
import za.co.sindi.sql.sql2j.ast.SecurityMode;
import za.co.sindi.sql.sql2j.ast.TableConstraint;
import za.co.sindi.sql.sql2j.ast.TriggerAction;
import za.co.sindi.sql.sql2j.ast.TriggerEvent;
import za.co.sindi.sql.sql2j.ast.TriggerLevel;
import za.co.sindi.sql.sql2j.ast.TriggerOrder;
import za.co.sindi.sql.sql2j.ast.TriggerOrderPosition;
import za.co.sindi.sql.sql2j.ast.TriggerReference;
import za.co.sindi.sql.sql2j.ast.TriggerTiming;
import za.co.sindi.sql.sql2j.ast.TriggerTransition;
import za.co.sindi.sql.sql2j.ast.TruncateStatement;
import za.co.sindi.sql.sql2j.ast.TruncateTableStatement;
import za.co.sindi.sql.sql2j.ast.TypeDefinition;
import za.co.sindi.sql.sql2j.ast.TypeOption;
import za.co.sindi.sql.sql2j.ast.VolatilityCategory;
import za.co.sindi.sql.sql2j.lexer.Lexer;
import za.co.sindi.sql.sql2j.lexer.Token;
import za.co.sindi.sql.sql2j.lexer.Token.QuotedStringLiteral;
import za.co.sindi.sql.sql2j.lexer.TokenType;
import za.co.sindi.sql.sql2j.utils.SQLNumberConverter;

/**
 * @author Buhake Sindi
 * @since 12 August 2026
 */
public class PostgreSQLParser extends SQLParser {
	
	/** Comparison / equality operator lexemes recognized by the expression parser. */
    private static final Set<String> COMPARISON_OPERATORS = Set.of("=", "<>", "!=", "<", "<=", ">", ">=");
    private static final Set<String> ADDITIVE_OPERATORS = Set.of("+", "-", "||");
    private static final Set<String> MULTIPLICATIVE_OPERATORS = Set.of("*", "/", "%");

	/**
	 * @param lexer
	 */
	public PostgreSQLParser(Lexer lexer) {
		super(lexer);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected CreateStatement parseCreateStatement() {
		// TODO Auto-generated method stub
		boolean orReplace = false;
		boolean orAlter = false;
		if (matchKeyword("OR")) {
			orReplace = matchKeyword("REPLACE") || matchIdentifier("REPLACE");
			orAlter = matchKeyword("ALTER");
		}

        if (matchKeyword("TABLE")) return parseCreateTable();
        if (matchIdentifier("FUNCTION")) return parseCreateFunction(orReplace ? FunctionModifier.REPLACE : orAlter ? FunctionModifier.ALTER : null);
        if (matchKeyword("UNIQUE")) return parseCreateIndex(true);
        if (matchIdentifier("INDEX")) return parseCreateIndex(false);
        if (matchIdentifier("VIEW")) return parseCreateView(orReplace);
        if (matchIdentifier("SCHEMA")) return parseCreateSchema();
        if (matchIdentifier("PROCEDURE")) return parseCreateProcedure(orReplace);
        if (matchIdentifier("TYPE")) return parseCreateType();
        
        boolean constraintTrigger = false;
        if (matchKeyword("CONSTRAINT") && checkIdentifier("TRIGGER")) {
            constraintTrigger = true;
        }
        if (matchIdentifier("TRIGGER")) return parseCreateTrigger(orReplace, constraintTrigger);
        throw error("Expected TABLE, [UNIQUE] INDEX, VIEW, SCHEMA, FUNCTION, TYPE, TRIGGER or PROCEDURE after CREATE");
	}

	@Override
	protected AlterStatement parseAlterStatement() {
		// TODO Auto-generated method stub
		expectKeyword("TABLE");
        QualifiedName tableName = parseQualifiedName();
        AlterAction action = parseAlterAction();
        return new AlterTableStatement(tableName, action);
	}

	@Override
	protected DropStatement parseDropStatement() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected TruncateStatement parseTruncateStatement() {
		// TODO Auto-generated method stub
		return new TruncateTableStatement(parseQualifiedName());
	}
	
	@Override
	protected DQLStatement parseSelectStatement() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected DMLStatement parseInsertStatement() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected DMLStatement parseUpdateStatement() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected DMLStatement parseDeleteStatement() {
		// TODO Auto-generated method stub
		return null;
	}

	private CreateTableStatement parseCreateTable() {
		boolean ifNotExists = matchIdentifier("IF") && matchKeyword("NOT") && matchIdentifier("EXISTS");
        QualifiedName tableName = parseQualifiedName();
        expect(TokenType.LPAREN, "Expected '(' to start the column/constraint list");

        List<ColumnDefinition> columns = new ArrayList<>();
        List<TableConstraint> constraints = new ArrayList<>();
        do {
            if (isTableConstraintStart()) {
                constraints.add(parseTableConstraint());
            } else {
                columns.add(parseColumnDefinition());
            }
        } while (match(TokenType.COMMA));

        expect(TokenType.RPAREN, "Expected ')' to close the column/constraint list");
        return new CreateTableStatement(tableName, ifNotExists, columns, constraints, List.of(), List.of());
	}
    
    private boolean isTableConstraintStart() {
        return checkKeyword("CONSTRAINT") || checkKeyword("PRIMARY")
                || checkKeyword("FOREIGN") || checkKeyword("UNIQUE") || checkKeyword("CHECK");
    }
    
    private TableConstraint parseTableConstraint() {
        Optional<String> name = matchKeyword("CONSTRAINT")
                ? Optional.of(parseIdentifierName())
                : Optional.empty();

//        if (matchKeywords("PRIMARY", "KEY")) {
        if (matchKeyword("PRIMARY") && matchIdentifier("KEY")) return new TableConstraint.PrimaryKey(name, parseIdentifierList());
        if (matchKeyword("UNIQUE")) return new TableConstraint.Unique(name, parseIdentifierList());
        
//        if (matchKeywords("FOREIGN", "KEY")) {
        if (matchKeyword("FOREIGN") && matchIdentifier("KEY")) {
        	List<String> columns = parseIdentifierList();
            expectKeyword("REFERENCES");
            QualifiedName refTable = parseQualifiedName();
            List<String> refColumns = check(TokenType.LPAREN) ? parseIdentifierList() : List.of();
            ReferentialActions actions = parseReferentialActions();
            return new TableConstraint.ForeignKey(name, columns, refTable, refColumns, actions.onDelete(), actions.onUpdate());
        }
        
        if (matchKeyword("CHECK")) {
            expect(TokenType.LPAREN, "Expected '(' after CHECK");
            Expression expression = parseExpression();
            expect(TokenType.RPAREN, "Expected ')' to close CHECK expression");
            return new TableConstraint.Check(name, expression);
        }
        
        throw error("Expected PRIMARY KEY, UNIQUE, FOREIGN KEY or CHECK");
    }

    private List<String> parseIdentifierList() {
        expect(TokenType.LPAREN, "Expected '('");
        List<String> names = new ArrayList<>();
        do {
            names.add(parseIdentifierName());
        } while (match(TokenType.COMMA));
        expect(TokenType.RPAREN, "Expected ')'");
        return names;
    }
    
    private record ReferentialActions(Optional<ReferentialAction> onDelete, Optional<ReferentialAction> onUpdate) {}

    private ReferentialActions parseReferentialActions() {
        Optional<ReferentialAction> onDelete = Optional.empty();
        Optional<ReferentialAction> onUpdate = Optional.empty();
        while (matchKeyword("ON")) {
            if (matchIdentifier("DELETE")) {
                onDelete = Optional.of(parseReferentialAction());
            } else if (matchKeyword("UPDATE")) {
                onUpdate = Optional.of(parseReferentialAction());
            } else {
                throw error("Expected DELETE or UPDATE after ON");
            }
        }
        return new ReferentialActions(onDelete, onUpdate);
    }

    private ReferentialAction parseReferentialAction() {
        if (matchIdentifier("CASCADE")) return ReferentialAction.CASCADE;
        if (matchKeyword("SET")) {
        	if (matchKeyword("NULL")) return ReferentialAction.SET_NULL;
            if (matchKeyword("DEFAULT")) return ReferentialAction.SET_DEFAULT;
        }
        if (matchIdentifier("RESTRICT")) return ReferentialAction.RESTRICT;
        if (matchIdentifier("NO") && matchIdentifier("ACTION")) return ReferentialAction.NO_ACTION;
        throw error("Expected CASCADE, SET NULL, SET DEFAULT, RESTRICT or NO ACTION");
    }
    
	// ================================================================
    //  Expression parsing (Pratt / precedence-climbing)
    //
    //  Precedence, lowest to highest:
    //    OR
    //    AND
    //    NOT (prefix)
    //    comparisons  =  <>  !=  <  <=  >  >=  BETWEEN  IN  LIKE  IS [NOT] NULL
    //    + -  ||        (additive)
    //    * / %          (multiplicative)
    //    unary + -
    //    primary: literals, identifiers, function calls, ( expr )
    // ================================================================

    private Expression parseExpression() {
        return parseOr();
    }

    private Expression parseOr() {
        Expression left = parseAnd();
        while (matchKeyword("OR")) {
            left = new Expression.Binary(left, "OR", parseAnd());
        }
        return left;
    }

    private Expression parseAnd() {
        Expression left = parseNot();
        while (matchKeyword("AND")) {
            left = new Expression.Binary(left, "AND", parseNot());
        }
        return left;
    }

    private Expression parseNot() {
        if (matchKeyword("NOT")) {
            return new Expression.Unary("NOT", parseNot());
        }
        return parseComparison();
    }

    private Expression parseComparison() {
        Expression left = parseAdditive();
        while (true) {
            if (check(TokenType.OPERATOR) && COMPARISON_OPERATORS.contains(peek().value())) {
                String operator = next().value();
                left = new Expression.Binary(left, operator, parseAdditive());
            } else if (matchKeyword("NOT")) {
            	if (matchKeyword("BETWEEN")) {
                    left = parseBetweenTail(left, true);
                } else if (matchKeyword("IN")) {
                	left = new Expression.In(left, parseExpressionList(), true);
                } else if (matchKeyword("LIKE")) {
                    left = new Expression.Like(left, parseAdditive(), true);
                }
            } else if (matchKeyword("IS")) {
            	if (matchKeyword("NOT") && matchKeyword("NULL")) {
                    left = new Expression.IsNull(left, true);
                } else if (matchKeyword("NULL")) {
                    left = new Expression.IsNull(left, false);
                }
            } else if (matchKeyword("BETWEEN")) {
                left = parseBetweenTail(left, false);
            } else if (matchKeyword("IN")) {
                left = new Expression.In(left, parseExpressionList(), false);
            } else if (matchKeyword("LIKE")) {
                left = new Expression.Like(left, parseAdditive(), false);
            } else {
                break;
            }
        }
        return left;
    }

    private Expression parseBetweenTail(Expression target, boolean negated) {
        Expression lower = parseAdditive();
        expectKeyword("AND");
        Expression upper = parseAdditive();
        return new Expression.Between(target, lower, upper, negated);
    }

    private List<Expression> parseExpressionList() {
        expect(TokenType.LPAREN, "Expected '(' to start an expression list");
        List<Expression> expressions = new ArrayList<>();
        if (!check(TokenType.RPAREN)) {
            do {
                expressions.add(parseExpression());
            } while (match(TokenType.COMMA));
        }
        expect(TokenType.RPAREN, "Expected ')' to close an expression list");
        return expressions;
    }

    private Expression parseAdditive() {
        Expression left = parseMultiplicative();
        while (check(TokenType.OPERATOR) && ADDITIVE_OPERATORS.contains(peek().value())) {
            String operator = next().value();
            left = new Expression.Binary(left, operator, parseMultiplicative());
        }
        return left;
    }

    private Expression parseMultiplicative() {
        Expression left = parseUnary();
        while (check(TokenType.OPERATOR) && MULTIPLICATIVE_OPERATORS.contains(peek().value())) {
            String operator = next().value();
            left = new Expression.Binary(left, operator, parseUnary());
        }
        return left;
    }

    private Expression parseUnary() {
        if (check(TokenType.OPERATOR) && (peek().value().equals("-") || peek().value().equals("+"))) {
            String operator = next().value();
            return new Expression.Unary(operator, parseUnary());
        }
        return parsePrimary();
    }

    private Expression parsePrimary() {
        Token token = peek();

        if (token.type() == TokenType.NUMBER_LITERAL) {
            next();
            return new Expression.Literal(SQLNumberConverter.convertStringToNumber(token.value()), Expression.LiteralKind.NUMBER);
        }
        if (token.type() == TokenType.STRING_LITERAL) {
            next();
            return new Expression.Literal(token.value(), Expression.LiteralKind.STRING);
        }
        if ("TRUE".equals(token.value()) || "FALSE".equals(token.value())) {
            next();
            return new Expression.Literal(Boolean.valueOf(token.value()), Expression.LiteralKind.BOOLEAN);
        }
        if ("NULL".equals(token.value())) {
            next();
            return Expression.Literal.ofNull();
        }
        if (token.type() == TokenType.LPAREN) {
            next();
            Expression inner = parseExpression();
            expect(TokenType.RPAREN, "Expected ')' to close parenthesized expression");
            return new Expression.Parenthesized(inner);
        }
        if (token.type() == TokenType.IDENTIFIER || token.type() == TokenType.QUOTED_IDENTIFIER
                || token.type() == TokenType.KEYWORD) {
            next();
            if (check(TokenType.LPAREN)) {
                next();
                List<Expression> args = new ArrayList<>();
                if (!check(TokenType.RPAREN)) {
                    do {
                        args.add(parseExpression());
                    } while (match(TokenType.COMMA));
                }
                expect(TokenType.RPAREN, "Expected ')' to close function call arguments");
                return new Expression.FunctionCall(token.value(), args);
            }
            return new Expression.ColumnReference(token.value());
        }
        throw error("Unexpected token in expression: " + token);
    }
    
    private ColumnDefinition parseColumnDefinition() {
        String name = parseIdentifierName();
        DataType type = parseDataType();
        List<ColumnConstraint> constraints = new ArrayList<>();
        while (true) {
            ColumnConstraint constraint = parseColumnConstraint();
            if (constraint == null) {
                break;
            }
            constraints.add(constraint);
        }
        return new ColumnDefinition(name, type, constraints);
    }

    private ColumnConstraint parseColumnConstraint() {
        if (matchKeyword("NOT") && matchKeyword("NULL")) {
            return new ColumnConstraint.NotNull();
        }
        if (matchKeyword("NULL")) {
            return new ColumnConstraint.Nullable();
        }
        if (matchKeyword("PRIMARY") && (matchKeyword("KEY") || matchIdentifier("KEY"))) {
            return new ColumnConstraint.PrimaryKey();
        }
        if (matchKeyword("UNIQUE")) {
            return new ColumnConstraint.Unique();
        }
        if (matchKeyword("DEFAULT")) {
            return new ColumnConstraint.DefaultValue(parseExpression());
        }
        if (matchKeyword("CHECK")) {
            expect(TokenType.LPAREN, "Expected '(' after CHECK");
            Expression expression = parseExpression();
            expect(TokenType.RPAREN, "Expected ')' to close CHECK expression");
            return new ColumnConstraint.Check(Optional.empty(), expression);
        }
        if (matchKeyword("REFERENCES")) {
            return parseReferencesClause();
        }
        if (matchKeyword("COLLATE")) {
            return new ColumnConstraint.Collate(parseIdentifierName());
        }
        if ((matchKeyword("GENERATED") && matchKeyword("ALWAYS") && matchKeyword("AS") && matchKeyword("IDENTITY"))
                || (matchKeyword("GENERATED") && matchKeyword("BY") && matchKeyword("DEFAULT") && matchKeyword("AS") && matchKeyword("IDENTITY"))
                || matchKeyword("AUTO_INCREMENT") || matchKeyword("AUTOINCREMENT") || matchKeyword("IDENTITY")) {
            return new ColumnConstraint.AutoIncrement();
        }
        return null; // no more column constraints here
    }
    
    private DataType parseDataType() {
//        Token nameToken = next();
//        if (nameToken.type() != TokenType.IDENTIFIER && nameToken.type() != TokenType.KEYWORD) {
//            throw error("Expected a data type name");
//        }
//        String typeName = nameToken.value().toUpperCase();
    	Token nameToken = peek();
        if (nameToken.type() != TokenType.IDENTIFIER && nameToken.type() != TokenType.KEYWORD) {
            throw error("Expected a data type name");
        }
        QualifiedName typeName = parseQualifiedName(); //nameToken.value().toUpperCase();

        List<Integer> parameters = new ArrayList<>();
        if (match(TokenType.LPAREN)) {
            do {
                Token number = expect(TokenType.NUMBER_LITERAL, "Expected a numeric type parameter");
                parameters.add(SQLNumberConverter.convertStringToNumber(number.value()).intValue());
            } while (match(TokenType.COMMA));
            expect(TokenType.RPAREN, "Expected ')' to close type parameters");
        }

        List<String> modifiers = new ArrayList<>();
        if (matchKeyword("WITH")) {
            modifiers.add("WITH");
            expectKeyword("TIME");
            expectKeyword("ZONE");
            modifiers.add("TIME");
            modifiers.add("ZONE");
        } else if (matchKeyword("WITHOUT")) {
            modifiers.add("WITHOUT");
            expectKeyword("TIME");
            expectKeyword("ZONE");
            modifiers.add("TIME");
            modifiers.add("ZONE");
        }

        return new DataType(typeName, parameters, modifiers);
    }
    
    private ColumnConstraint.References parseReferencesClause() {
        QualifiedName refTable = parseQualifiedName();
        List<String> refColumns = check(TokenType.LPAREN) ? parseIdentifierList() : List.of();
        ReferentialActions actions = parseReferentialActions();
        return new ColumnConstraint.References(refTable, refColumns, actions.onDelete(), actions.onUpdate());
    }
    
    // ----------------------------------------------------- CREATE/DROP FUNCTION

    private CreateFunctionStatement parseCreateFunction(FunctionModifier modifier) {
        QualifiedName name = parseQualifiedName();
        List<RoutineParameter> parameters = parseRoutineParameters();
        expectIdentifier("RETURNS");
        RoutineReturnType returnType = parseReturnType();
        List<RoutineCharacteristic> characteristics = new ArrayList<>(parseRoutineCharacteristics());
        RoutineBody body = parseRoutineBody();
        characteristics.addAll(parseRoutineCharacteristics()); // PostgreSQL allows e.g. LANGUAGE after the body too
        return new CreateFunctionStatement(name, modifier, parameters, returnType, characteristics, body);
    }

    // ---------------------------------------------------- CREATE/DROP PROCEDURE

    private CreateProcedureStatement parseCreateProcedure(boolean orReplace) {
        QualifiedName name = parseQualifiedName();
        List<RoutineParameter> parameters = parseRoutineParameters();
        List<RoutineCharacteristic> characteristics = new ArrayList<>(parseRoutineCharacteristics());
        RoutineBody body = parseRoutineBody();
        characteristics.addAll(parseRoutineCharacteristics());
        return new CreateProcedureStatement(name, orReplace, parameters, characteristics, body);
    }

    // --------------------------------------------------- shared routine grammar

    private List<RoutineParameter> parseRoutineParameters() {
        expect(TokenType.LPAREN, "Expected '(' to start the parameter list");
        List<RoutineParameter> parameters = new ArrayList<>();
        if (!check(TokenType.RPAREN)) {
            do {
                parameters.add(parseRoutineParameter());
            } while (match(TokenType.COMMA));
        }
        expect(TokenType.RPAREN, "Expected ')' to close the parameter list");
        return parameters;
    }

    private RoutineParameter parseRoutineParameter() {
        ParameterMode mode = ParameterMode.IN;
        if (matchKeyword("INOUT")) {
            mode = ParameterMode.INOUT;
        } else if (matchKeyword("OUT")) {
            mode = ParameterMode.OUT;
        } else {
            matchKeyword("IN"); // explicit IN is the default anyway
        }

        // SQL permits unnamed, type-only parameters (e.g. "int" with no name), so a
        // parameter is only treated as "name type" when TWO identifier-like tokens
        // appear back to back; otherwise the single token in front is just the type.
        Optional<String> name = Optional.empty();
        if ((check(TokenType.IDENTIFIER) || check(TokenType.QUOTED_IDENTIFIER))
                && (peek(1).type() == TokenType.IDENTIFIER || peek(1).type() == TokenType.KEYWORD)) {
            name = Optional.of(parseIdentifierName());
        }
        DataType type = parseDataType();

        Optional<Expression> defaultValue = Optional.empty();
        if (matchKeyword("DEFAULT")) {
            defaultValue = Optional.of(parseExpression());
        } else if (check(TokenType.OPERATOR) && peek().value().equals("=")) {
            next();
            defaultValue = Optional.of(parseExpression());
        }
        return new RoutineParameter(mode, name, type, defaultValue);
    }

    private RoutineReturnType parseReturnType() {
        if (matchKeyword("SETOF")) {
            return new RoutineReturnType.SetOf(parseDataType());
        }
        if (matchKeyword("TABLE")) {
            expect(TokenType.LPAREN, "Expected '(' to start RETURNS TABLE columns");
            List<ColumnDefinition> columns = new ArrayList<>();
            do {
                columns.add(parseColumnDefinition());
            } while (match(TokenType.COMMA));
            expect(TokenType.RPAREN, "Expected ')' to close RETURNS TABLE columns");
            return new RoutineReturnType.Table(columns);
        }
        return new RoutineReturnType.Scalar(parseDataType());
    }

    private List<RoutineCharacteristic> parseRoutineCharacteristics() {
        List<RoutineCharacteristic> characteristics = new ArrayList<>();
        while (true) {
            if (matchIdentifier("LANGUAGE")) {
                characteristics.add(new RoutineCharacteristic.Language(parseLanguageName()));
            } else if (matchKeyword("NOT") && matchIdentifier("DETERMINISTIC")) {
                characteristics.add(new RoutineCharacteristic.Deterministic(false));
            } else if (matchIdentifier("DETERMINISTIC")) {
                characteristics.add(new RoutineCharacteristic.Deterministic(true));
            } else if (matchIdentifier("IMMUTABLE")) {
                characteristics.add(new RoutineCharacteristic.Volatility(VolatilityCategory.IMMUTABLE));
            } else if (matchIdentifier("STABLE")) {
                characteristics.add(new RoutineCharacteristic.Volatility(VolatilityCategory.STABLE));
            } else if (matchIdentifier("VOLATILE")) {
                characteristics.add(new RoutineCharacteristic.Volatility(VolatilityCategory.VOLATILE));
            } else if (matchIdentifier("STRICT")) {
                characteristics.add(new RoutineCharacteristic.NullHandling(NullHandlingMode.STRICT));
            } else if (matchIdentifier("CALLED") && matchKeyword("ON") && matchKeyword("NULL") && matchKeyword("INPUT")) {
                characteristics.add(new RoutineCharacteristic.NullHandling(NullHandlingMode.CALLED_ON_NULL_INPUT));
            } else if (matchIdentifier("RETURNS") && matchKeyword("NULL") && matchKeyword("ON") && matchKeyword("NULL") && matchIdentifier("INPUT")) {
                characteristics.add(new RoutineCharacteristic.NullHandling(NullHandlingMode.RETURNS_NULL_ON_NULL_INPUT));
            } else if (matchIdentifier("SECURITY") && matchIdentifier("DEFINER")) {
                characteristics.add(new RoutineCharacteristic.Security(SecurityMode.DEFINER));
            } else if (matchIdentifier("SECURITY") && matchIdentifier("INVOKER")) {
                characteristics.add(new RoutineCharacteristic.Security(SecurityMode.INVOKER));
            } else {
                break;
            }
        }
        return characteristics;
    }

    private String parseLanguageName() {
        Token token = next();
        return switch (token.type()) {
            case IDENTIFIER, KEYWORD, STRING_LITERAL -> token.value();
            default -> throw error("Expected a language name after LANGUAGE");
        };
    }

    /**
     * Parses whichever body form the routine uses: a PostgreSQL dollar-quoted
     * string, a plain quoted string, a {@code BEGIN ... END} block, or a bare
     * {@code RETURN <expr>}. {@code AS} is optional before the body because
     * MySQL-style {@code CREATE FUNCTION} bodies go straight from the
     * characteristics to {@code BEGIN} with no {@code AS} in between.
     */
    private RoutineBody parseRoutineBody() {
        matchKeyword("AS");

        if (check(TokenType.QUOTED_STRING)) {
            QuotedStringLiteral token = (QuotedStringLiteral) next();
            return new RoutineBody.DollarQuoted(token.startDelimiter(), (String) token.value());
        }
        if (check(TokenType.STRING_LITERAL)) {
            Token token = next();
            return new RoutineBody.QuotedString((String) token.value());
        }
        if (checkKeyword("BEGIN")) {
            return parseBeginEndBody();
        }
        if (matchKeyword("RETURN")) {
            return new RoutineBody.ReturnExpression(parseExpression());
        }
        throw error("Expected a routine body: $$...$$, a quoted string, BEGIN...END, or RETURN <expr>");
    }

    /**
     * Reads a {@code BEGIN ... END} block via best-effort balanced-token scanning.
     * A depth counter increments on nested {@code BEGIN} and decrements on a bare
     * {@code END}; an {@code END} immediately followed by {@code IF}/{@code CASE}/
     * {@code LOOP}/{@code WHILE}/{@code FOR} is recognized as that construct's own
     * terminator and left alone. See {@link RoutineBody.BeginEndBlock} for the one
     * known ambiguity this heuristic can't resolve (a bare {@code CASE ... END}
     * <em>expression</em> inside the block).
     */
    private RoutineBody.BeginEndBlock parseBeginEndBody() {
        expectIdentifier("BEGIN");
        RawCapture capture = new RawCapture();
        capture.appendRaw("BEGIN");

        int depth = 1;
        while (depth > 0) {
            if (!hasNext()) {
                throw error("Unterminated BEGIN ... END block in routine body");
            }
            if (checkKeyword("BEGIN")) {
                depth++;
                capture.append(next());
            } else if (checkKeyword("END")) {
                capture.append(next());
                if (checkKeyword("IF") || checkKeyword("CASE") || checkKeyword("LOOP")
                        || checkKeyword("WHILE") || checkKeyword("FOR")) {
                    capture.append(next());
                } else {
                    depth--;
                }
            } else {
                capture.append(next());
            }
        }
        return new RoutineBody.BeginEndBlock(capture.text());
    }
    
    // ------------------------------------------------------------ CREATE INDEX

    private CreateIndexStatement parseCreateIndex(boolean unique) {
    	boolean ifNotExists = matchKeyword("IF") &&  matchKeyword("NOT") &&  matchKeyword("EXISTS");
        String indexName = parseIdentifierName();
        expectKeyword("ON");
        QualifiedName tableName = parseQualifiedName();
        List<String> columns = parseIndexColumnList();
        return new CreateIndexStatement(indexName, unique, ifNotExists, tableName, columns);
    }

    private List<String> parseIndexColumnList() {
        expect(TokenType.LPAREN, "Expected '(' to start the index column list");
        List<String> columns = new ArrayList<>();
        do {
            columns.add(parseIdentifierName());
            // tolerate (and discard) an ASC/DESC sort direction, e.g. CREATE INDEX ix ON t (name DESC)
            if (checkIdentifier("ASC") || checkIdentifier("DESC")) {
                next();
            }
        } while (match(TokenType.COMMA));
        expect(TokenType.RPAREN, "Expected ')' to close the index column list");
        return columns;
    }
    
    // ------------------------------------------------------------- CREATE VIEW

    private CreateViewStatement parseCreateView(boolean orReplace) {
        QualifiedName viewName = parseQualifiedName();
        List<String> columnNames = check(TokenType.LPAREN) ? parseIdentifierList() : List.of();
        expectKeyword("AS");
        String query = captureRawUntilStatementEnd();
        return new CreateViewStatement(viewName, orReplace, columnNames, query);
    }

    /**
     * Best-effort reconstruction of the raw SQL text of a trailing clause
     * (the {@code SELECT ...} body of a view) that this DDL-focused parser
     * intentionally does not build a full AST for.
     */
    private String captureRawUntilStatementEnd() {
        StringBuilder sb = new StringBuilder();
        TokenType previousType = null;
        while (!check(TokenType.SEMICOLON) && hasNext()) {
            Token token = next();
            if (!sb.isEmpty() && needsSpaceBefore(token.type(), previousType)) {
                sb.append(' ');
            }
            sb.append(tokenDisplayText(token));
            previousType = token.type();
        }
        return sb.toString();
    }

    private boolean needsSpaceBefore(TokenType current, TokenType previous) {
        if (current == TokenType.COMMA || current == TokenType.RPAREN || current == TokenType.DOT) {
            return false;
        }
        return previous != TokenType.DOT;
    }

    private String tokenDisplayText(Token token) {
        return switch (token.type()) {
            case STRING_LITERAL -> "'" + token.value().replace("'", "''") + "'";
            case QUOTED_IDENTIFIER -> "\"" + token.value() + "\"";
            case QUOTED_STRING -> ((QuotedStringLiteral)token).startDelimiter() + token.value() + ((QuotedStringLiteral)token).endDelimiter();
            default -> token.value();
        };
    }

    // ----------------------------------------------------------- CREATE SCHEMA

    private CreateSchemaStatement parseCreateSchema() {
        boolean ifNotExists = matchKeyword("IF") &&  matchKeyword("NOT") &&  matchKeyword("EXISTS");
        return new CreateSchemaStatement(parseIdentifierName(), ifNotExists);
    }
    
    /** Accumulates a sequence of tokens back into readable text, sharing one spacing policy across every raw-capture use site. */
    private final class RawCapture {
        private final StringBuilder sb = new StringBuilder();
        private TokenType previousType = null;

        void append(Token token) {
            if (!sb.isEmpty() && needsSpaceBefore(token.type(), previousType)) {
                sb.append(' ');
            }
            sb.append(tokenDisplayText(token));
            previousType = token.type();
        }

        void appendRaw(String literalText) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(literalText);
            previousType = null;
        }

        String text() {
            return sb.toString();
        }
    }
    
    // -------------------------------------------------------------- CREATE TYPE

    /**
     * Parses whichever of PostgreSQL's five {@code CREATE TYPE} shapes is present:
     * <ul>
     *   <li>{@code CREATE TYPE name;} — a shell type (no body at all)</li>
     *   <li>{@code CREATE TYPE name AS (attr type, ...);} — composite</li>
     *   <li>{@code CREATE TYPE name AS ENUM ('label', ...);} — enum</li>
     *   <li>{@code CREATE TYPE name AS RANGE (SUBTYPE = type, ...);} — range</li>
     *   <li>{@code CREATE TYPE name (INPUT = fn, OUTPUT = fn, ...);} — base type</li>
     * </ul>
     */
    private CreateTypeStatement parseCreateType() {
        QualifiedName name = parseQualifiedName();

        if (matchKeyword("AS")) {
            if (matchIdentifier("ENUM")) {
                return new CreateTypeStatement(name, new TypeDefinition.Enum(parseStringLiteralList()));
            }
            if (matchIdentifier("RANGE")) {
                return new CreateTypeStatement(name, new TypeDefinition.Range(parseTypeOptionList()));
            }
            return new CreateTypeStatement(name, new TypeDefinition.Composite(parseCompositeAttributeList()));
        }
        if (check(TokenType.LPAREN)) {
            return new CreateTypeStatement(name, new TypeDefinition.Base(parseTypeOptionList()));
        }
        if (check(TokenType.SEMICOLON) || !hasNext()) {
            return new CreateTypeStatement(name, new TypeDefinition.Shell());
        }
        throw error("Expected AS, '(', or ';' after CREATE TYPE " + name);
    }

    private List<String> parseStringLiteralList() {
        expect(TokenType.LPAREN, "Expected '(' to start the ENUM label list");
        List<String> labels = new ArrayList<>();
        if (!check(TokenType.RPAREN)) {
            do {
                Token token = expect(TokenType.STRING_LITERAL, "Expected a string literal ENUM label");
                labels.add((String) token.value());
            } while (match(TokenType.COMMA));
        }
        expect(TokenType.RPAREN, "Expected ')' to close the ENUM label list");
        return labels;
    }

    private List<CompositeAttribute> parseCompositeAttributeList() {
        expect(TokenType.LPAREN, "Expected '(' to start the composite type attribute list");
        List<CompositeAttribute> attributes = new ArrayList<>();
        if (!check(TokenType.RPAREN)) {
            do {
                String attrName = parseIdentifierName();
                DataType type = parseDataType();
                Optional<String> collation = matchKeyword("COLLATE") ? Optional.of(parseIdentifierName()) : Optional.empty();
                attributes.add(new CompositeAttribute(attrName, type, collation));
            } while (match(TokenType.COMMA));
        }
        expect(TokenType.RPAREN, "Expected ')' to close the composite type attribute list");
        return attributes;
    }

    /** Shared by {@code AS RANGE (...)} and base-type {@code (...)}: a parenthesized {@code option = value} list. */
    private List<TypeOption> parseTypeOptionList() {
        expect(TokenType.LPAREN, "Expected '(' to start the type option list");
        List<TypeOption> options = new ArrayList<>();
        if (!check(TokenType.RPAREN)) {
            do {
                options.add(parseTypeOption());
            } while (match(TokenType.COMMA));
        }
        expect(TokenType.RPAREN, "Expected ')' to close the type option list");
        return options;
    }

    private TypeOption parseTypeOption() {
        String name = parseTypeOptionName();
        if (matchOperator("=")) {
            return new TypeOption(name, Optional.of(captureTypeOptionValue()));
        }
        return new TypeOption(name, Optional.empty()); // a bare flag, e.g. PASSEDBYVALUE
    }

    private String parseTypeOptionName() {
        Token token = next();
        if (token.type() == TokenType.IDENTIFIER || token.type() == TokenType.KEYWORD) {
            return token.value();
        }
        throw error("Expected a type option name");
    }

    /**
     * Captures the raw text of whatever follows a type option's {@code =}, stopping at the
     * next {@code ,} or {@code )}. Deliberately not parsed into a typed sub-grammar — see
     * {@link TypeOption} for why (the option list mixes identifiers, type references,
     * numbers, bare keywords, and quoted strings under one syntax with no way to tell
     * which a given option expects without hard-coding PostgreSQL's option docs).
     */
    private String captureTypeOptionValue() {
        RawCapture capture = new RawCapture();
        while (!check(TokenType.COMMA) && !check(TokenType.RPAREN) && hasNext()) {
            capture.append(next());
        }
        if (capture.text().isEmpty()) {
            throw error("Expected a value after '=' in a type option");
        }
        return capture.text();
    }
    
    // ---------------------------------------------------------- CREATE TRIGGER

    /**
     * Parses {@code CREATE TRIGGER} into the unified {@link CreateTriggerStatement}
     * shape (see its Javadoc for which fields come from which dialect). Concretely
     * enforces MySQL vs PostgreSQL differences via {@link #requireDialectFeature}:
     * MySQL allows exactly one event, no {@code REFERENCING}/{@code WHEN}, and an
     * inline body; PostgreSQL allows multiple {@code OR}'d events, both clauses,
     * and always calls {@code EXECUTE FUNCTION} rather than embedding a body.
     */
    private CreateTriggerStatement parseCreateTrigger(boolean orReplace, boolean constraintTrigger) {
        QualifiedName triggerName = parseQualifiedName();
        TriggerTiming timing = parseTriggerTiming();

        List<TriggerEvent> events = new ArrayList<>();
        events.add(parseTriggerEvent());
        while (matchKeyword("OR")) {
            events.add(parseTriggerEvent());
        }

        expectKeyword("ON");
        QualifiedName tableName = parseQualifiedName();

        List<TriggerReference> referencing = new ArrayList<>();
        if (matchKeyword("REFERENCING")) {
            do {
                referencing.add(parseTriggerReference());
            } while (checkKeyword("OLD") || checkKeyword("NEW"));
        }

        Optional<TriggerLevel> level = Optional.empty();
        if (matchKeyword("FOR")) {
            matchKeyword("EACH");
            if (matchKeyword("ROW")) {
                level = Optional.of(TriggerLevel.ROW);
            } else {
                expectKeyword("STATEMENT");
                level = Optional.of(TriggerLevel.STATEMENT);
            }
        }

        Optional<TriggerOrder> order = Optional.empty();
        if (checkIdentifier("FOLLOWS") || checkIdentifier("PRECEDES")) {
            TriggerOrderPosition position = checkIdentifier("FOLLOWS") ? TriggerOrderPosition.FOLLOWS : TriggerOrderPosition.PRECEDES;
            next();
            order = Optional.of(new TriggerOrder(position, parseIdentifierName()));
        }

        Optional<Expression> when = Optional.empty();
        if (matchKeyword("WHEN")) {
            expect(TokenType.LPAREN, "Expected '(' after WHEN");
            when = Optional.of(parseExpression());
            expect(TokenType.RPAREN, "Expected ')' to close WHEN condition");
        }

        TriggerAction action = parseTriggerAction();
        return new CreateTriggerStatement(triggerName, orReplace, constraintTrigger, timing, events, tableName,
                referencing, level, when, action, order);
    }

    private TriggerTiming parseTriggerTiming() {
        if (matchIdentifier("BEFORE")) return TriggerTiming.BEFORE;
        if (matchIdentifier("AFTER")) return TriggerTiming.AFTER;
        if (matchIdentifier("INSTEAD") && matchIdentifier("OF")) return TriggerTiming.INSTEAD_OF;
        throw error("Expected BEFORE, AFTER or INSTEAD OF");
    }

    private TriggerEvent parseTriggerEvent() {
        if (matchKeyword("INSERT")) return new TriggerEvent.Insert();
        if (matchKeyword("DELETE")) return new TriggerEvent.Delete();
        if (matchKeyword("TRUNCATE")) {
            return new TriggerEvent.Truncate();
        }
        if (matchKeyword("UPDATE")) {
            List<String> columns = new ArrayList<>();
            if (matchKeyword("OF")) {
                do {
                    columns.add(parseIdentifierName());
                } while (match(TokenType.COMMA));
            }
            return new TriggerEvent.Update(columns);
        }
        throw error("Expected INSERT, UPDATE, DELETE or TRUNCATE");
    }

    private TriggerReference parseTriggerReference() {
        TriggerTransition transition;
        if (matchKeyword("OLD")) {
            transition = TriggerTransition.OLD;
        } else {
            expectKeyword("NEW");
            transition = TriggerTransition.NEW;
        }
        boolean isTable = matchKeyword("TABLE");
        matchKeyword("ROW"); // Oracle-style explicit ROW marker; no-op beyond consuming it
        expectKeyword("AS");
        return new TriggerReference(transition, isTable, parseIdentifierName());
    }

    private TriggerAction parseTriggerAction() {
        if (matchKeyword("EXECUTE")) {
            boolean legacyProcedureSyntax = !matchKeyword("FUNCTION");
            if (legacyProcedureSyntax) {
                expectKeyword("PROCEDURE");
            }
            QualifiedName functionName = parseQualifiedName();
            expect(TokenType.LPAREN, "Expected '(' after trigger function name");
            List<Expression> args = new ArrayList<>();
            if (!check(TokenType.RPAREN)) {
                do {
                    args.add(parseExpression());
                } while (match(TokenType.COMMA));
            }
            expect(TokenType.RPAREN, "Expected ')' to close trigger function arguments");
            return new TriggerAction.ExecuteFunction(functionName, args, legacyProcedureSyntax);
        }
        return new TriggerAction.Body(parseTriggerBody());
    }

    /**
     * Like {@link #parseRoutineBody()}, but also tolerates a single bare statement with
     * no {@code AS}/{@code BEGIN...END}/dollar-quoting wrapper at all — common for
     * one-line MySQL trigger bodies (e.g. {@code SET NEW.x = NOW();}).
     */
    private RoutineBody parseTriggerBody() {
        if (check(TokenType.QUOTED_STRING) || check(TokenType.STRING_LITERAL)
                || checkKeyword("BEGIN") || checkKeyword("AS") || checkKeyword("RETURN")) {
            return parseRoutineBody();
        }
        RawCapture capture = new RawCapture();
        while (!check(TokenType.SEMICOLON) && hasNext()) {
            capture.append(next());
        }
        return new RoutineBody.SingleStatement(capture.text());
    }
    
    private AlterAction parseAlterAction() {
        if (matchKeyword("ADD")) {
            if (matchKeyword("COLUMN") || !isTableConstraintStart()) {
                matchKeyword("COLUMN"); // no-op if already consumed above; safe if absent
                boolean ifNotExists = matchIdentifier("IF") && matchKeyword("NOT") && matchIdentifier("EXISTS"); //matchKeywords("IF", "NOT", "EXISTS");
                return new AlterAction.AddColumn(parseColumnDefinition(), ifNotExists);
            }
            return new AlterAction.AddConstraint(parseTableConstraint());
        }
        if (matchKeyword("DROP")) {
            if (matchKeyword("CONSTRAINT")) {
                boolean ifExists = matchIdentifier("IF") && matchIdentifier("EXISTS"); //matchKeywords("IF", "EXISTS");
                return new AlterAction.DropConstraint(parseIdentifierName(), ifExists);
            }
            matchKeyword("COLUMN"); // optional in several dialects
            boolean ifExists = matchIdentifier("IF") && matchIdentifier("EXISTS"); //matchKeywords("IF", "EXISTS");
            return new AlterAction.DropColumn(parseIdentifierName(), ifExists);
        }
        if (matchKeyword("RENAME")) {
            if (matchKeyword("TO")) {
                return new AlterAction.RenameTable(parseQualifiedName());
            }
            matchKeyword("COLUMN"); // optional
            String from = parseIdentifierName();
            expectKeyword("TO");
            return new AlterAction.RenameColumn(from, parseIdentifierName());
        }
        if (matchKeyword("ALTER")) {
            matchKeyword("COLUMN"); // optional
            String column = parseIdentifierName();
            if (matchKeyword("SET") && matchKeyword("DEFAULT")) {
                return new AlterAction.AlterColumnSetDefault(column, parseExpression());
            }
            if (matchIdentifier("DROP") && matchKeyword("DEFAULT")) {
                return new AlterAction.AlterColumnDropDefault(column);
            }
            if (matchKeyword("SET") && matchKeyword("NOT") && matchKeyword("NULL")) {
                return new AlterAction.AlterColumnSetNotNull(column);
            }
            if (matchIdentifier("DROP") && matchKeyword("NOT") && matchKeyword("NULL")) {
                return new AlterAction.AlterColumnDropNotNull(column);
            }
            if ((matchKeyword("SET") && matchIdentifier("TYPE")) || matchIdentifier("TYPE")) {
                return new AlterAction.AlterColumnType(column, parseDataType());
            }
            throw error("Expected SET/DROP DEFAULT, SET/DROP NOT NULL, or TYPE after ALTER COLUMN " + column);
        }
        throw error("Expected ADD, DROP, RENAME or ALTER after ALTER TABLE " + "<table>");
    }
}
