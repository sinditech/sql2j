/**
 * 
 */
package za.co.sindi.sql.sql2j;

import za.co.sindi.sql.sql2j.codegen.PostgreSQLTypeConverter;
import za.co.sindi.sql.sql2j.codegen.SQLTypeConverter;

/**
 * @author Buhake Sindi
 * @since 09 August 2026
 */
public enum SQLDialect {

//	DB2,
//	MARIADB,
//    MYSQL,
//    ORACLE,
    POSTGRESQL(SQLParserFactory.postgresql(), new PostgreSQLTypeConverter()),
//    SQL_SERVER,
//    SQLITE
    ;
	
	private final SQLParserFactory sqlParserFactory;
	private final SQLTypeConverter sqlTypeConverter;

	/**
	 * @param sqlParserFactory
	 * @param sqlTypeConverter
	 */
	private SQLDialect(SQLParserFactory sqlParserFactory, SQLTypeConverter sqlTypeConverter) {
		this.sqlParserFactory = sqlParserFactory;
		this.sqlTypeConverter = sqlTypeConverter;
	}

	/**
	 * @return the sqlParserFactory
	 */
	public SQLParserFactory getSqlParserFactory() {
		return sqlParserFactory;
	}

	/**
	 * @return the sqlTypeConverter
	 */
	public SQLTypeConverter getSqlTypeConverter() {
		return sqlTypeConverter;
	}
}
