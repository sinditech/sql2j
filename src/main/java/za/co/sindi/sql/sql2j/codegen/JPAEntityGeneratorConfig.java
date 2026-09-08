/**
 * 
 */
package za.co.sindi.sql.sql2j.codegen;

/**
 * @author Buhake Sindi
 * @since 18 August 2026
 */
public record JPAEntityGeneratorConfig(String packageName, boolean useJakartaEENamespace, FetchType toOneFetchType, boolean useIdClassForCompositeKeys, boolean generateEqualsAndHashCode) {

	public String persistenceNamespace() {
		return useJakartaEENamespace ? "jakarta.persistence"  : "javax.persistence";
	}
	
	public static enum FetchType {

	    /** Defines that data can be lazily fetched. */
	    LAZY,

	    /** Defines that data must be eagerly fetched. */
	    EAGER
	}
	
	public static Builder builder() {
		return new Builder();
	}
	
	public static final class Builder {
		private String packageName; 
		private boolean useJakartaEENamespace = true; 
		private FetchType toOneFetchType = FetchType.EAGER;
		private boolean useIdClassForCompositeKeys = true;
		private boolean generateEqualsAndHashCode = true;
		
		private Builder() {}
		
		/**
		 * @param packageName the packageName to set
		 */
		public Builder packageName(String packageName) {
			this.packageName = packageName;
			return this;
		}

		/**
		 * @param useJakartaEENamespace the useJakartaEENamespace to set
		 */
		public Builder useJakartaEENamespace(boolean useJakartaEENamespace) {
			this.useJakartaEENamespace = useJakartaEENamespace;
			return this;
		}

		/**
		 * @param toOneFetchType the toOneFetchType to set
		 */
		public Builder toOneFetchType(FetchType toOneFetchType) {
			this.toOneFetchType = toOneFetchType;
			return this;
		}

		/**
		 * @param useIdClassForCompositeKeys the useIdClassForCompositeKeys to set
		 */
		public Builder useIdClassForCompositeKeys(boolean useIdClassForCompositeKeys) {
			this.useIdClassForCompositeKeys = useIdClassForCompositeKeys;
			return this;
		}

		/**
		 * @param generateEqualsAndHashCode the generateEqualsAndHashCode to set
		 */
		public Builder generateEqualsAndHashCode(boolean generateEqualsAndHashCode) {
			this.generateEqualsAndHashCode = generateEqualsAndHashCode;
			return this;
		}

		public JPAEntityGeneratorConfig build() {
			return new JPAEntityGeneratorConfig(packageName, useJakartaEENamespace, toOneFetchType, useIdClassForCompositeKeys, generateEqualsAndHashCode);
		}
	}
}
