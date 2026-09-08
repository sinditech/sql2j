/**
 * 
 */
package za.co.sindi.sql.sql2j.codegen;

/**
 * @author Buhake Sindi
 * @since 19 August 2026
 */
public record GeneratedEntity(String packageName, String className, String code) {

	public String fileName() {
		return className + ".java";
	}
}
