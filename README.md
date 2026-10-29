# SQL to Java (SQL2J)
**SQL2J** is a lightweight Java library that parses SQL DDL scripts and generates Java code — currently focused on **JPA entity classes**.

It reads `CREATE TABLE`, `ALTER TABLE`, `CREATE TYPE` (enums and composite types), and related statements, builds an in-memory schema model, and produces clean, annotated JPA entities ready to use in your project.

The library is designed with a **pluggable dialect architecture**, so support for multiple relational databases can be added without changing the core generation pipeline.

- **Repository**: [https://github.com/sinditech/sql2j](https://github.com/sinditech/sql2j)
- **Group ID**: `za.co.sindi.sql`
- **Artifact ID**: `sql2j`
- **Current version**: `0.0.1-SNAPSHOT`
- **License**: [Apache License 2.0](LICENSE)
- **Author**: Buhake Sindi (Sindi Technologies (Pty) Ltd)

---

## Features

- **SQL DDL parsing** with a full Abstract Syntax Tree (AST)
- **Dialect-based architecture** — each RDBMS provides its own parser factory and type converter
- **JPA entity generation** with:
  - `@Entity`, `@Table`, `@Id`, `@Column`, `@GeneratedValue`
  - Relationship mapping (`@ManyToOne` / `@OneToOne`) from foreign keys
  - Configurable fetch type (`LAZY` / `EAGER`)
  - Composite primary key support via `@IdClass` (default) or `@EmbeddedId`
  - Optional `equals()` / `hashCode()` generation
  - Support for both **Jakarta Persistence** (`jakarta.persistence`) and classic `javax.persistence`
- **Custom database types**:
  - Named enum types → Java `enum` + `AttributeConverter`
  - Composite / record types → Java value classes + converters
- **Type mapping** tailored per dialect (serial/auto-increment types, UUID, JSON, timestamps with/without time zone, etc.)
- Clean, modern Java (records, pattern matching, streams)

---

## Supported Dialects

| Dialect        | Status          | Notes |
|----------------|-----------------|-------|
| **PostgreSQL** | Fully implemented | Parser + type converter available |
| **MySQL**      | Planned         | Will be added via the same dialect SPI |
| MariaDB        | Planned         | |
| Oracle         | Planned         | |
| SQL Server     | Planned         | |
| SQLite         | Planned         | |
| DB2            | Planned         | |

Dialects are selected through the `SQLDialect` enum. Each dialect supplies:

- A `SQLParserFactory` (responsible for creating a dialect-aware parser)
- An `SQLTypeConverter` (maps SQL data types to Java types)

Adding a new RDBMS only requires implementing these two components and registering them in `SQLDialect`.

---

## Requirements

| Requirement       | Version / Notes                          |
|-------------------|------------------------------------------|
| Java              | **25** (project is compiled with `--release 25`) |
| Build tool        | Maven 3.x                                |
| Runtime dependency| `za.co.sindi.commons:sindi-commons` (SNAPSHOT) |

> **Note**: Because the library currently targets Java 25 and uses SNAPSHOT dependencies from Central Portal Snapshots, you will need access to the snapshot repository and a compatible JDK.

---

## Installation

### Maven

Add the following to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>central-portal-snapshots</id>
        <name>Central Portal Snapshots</name>
        <url>https://central.sonatype.com/repository/maven-snapshots/</url>
        <releases>
            <enabled>false</enabled>
        </releases>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>za.co.sindi.sql</groupId>
        <artifactId>sql2j</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </dependency>
</dependencies>
```

### Building from source

```bash
git clone https://github.com/sinditech/sql2j.git
cd sql2j
mvn clean install
```

---

## Quick Start

```java
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import za.co.sindi.sql.sql2j.SQLDialect;
import za.co.sindi.sql.sql2j.ast.Statement;
import za.co.sindi.sql.sql2j.codegen.GeneratedEntity;
import za.co.sindi.sql.sql2j.codegen.JPAEntityCodeGenerator;
import za.co.sindi.sql.sql2j.codegen.JPAEntityGeneratorConfig;
import za.co.sindi.sql.sql2j.codegen.JPAEntityGeneratorConfig.FetchType;
import za.co.sindi.sql.sql2j.codegen.CompositeKeyStrategy;
import za.co.sindi.sql.sql2j.codegen.SchemaBuilder;
import za.co.sindi.sql.sql2j.codegen.metamodel.Schema;
import za.co.sindi.sql.sql2j.codegen.metamodel.Schemas;
import za.co.sindi.sql.sql2j.parser.Parser;

public class GenerateEntities {

    public static void main(String[] args) throws Exception {
        String sqlFile = "create-tables.sql";

        // Choose the dialect that matches your SQL script
        SQLDialect dialect = SQLDialect.POSTGRESQL;
        // Future example: SQLDialect dialect = SQLDialect.MYSQL;

        // 1. Parse the SQL DDL
        Parser parser = dialect.getSqlParserFactory().createParser(new FileReader(sqlFile));
        List<Statement> statements = parser.parse();

        // 2. Build the in-memory schema model
        SchemaBuilder.build(statements);

        // 3. Configure the JPA generator
        JPAEntityGeneratorConfig config = JPAEntityGeneratorConfig.builder()
                .packageName("com.example.shop.entity")
                .toOneFetchType(FetchType.LAZY)
                .useJakartaEENamespace(true)          // jakarta.persistence (default)
                .compositeKeyStrategy(CompositeKeyStrategy.ID_CLASS)
                .generateEqualsAndHashCode(true)
                .build();

        // 4. Generate entities for every schema
        Path outDir = Path.of("generated-entities");
        Files.createDirectories(outDir);

        for (String schemaName : Schemas.names()) {
            Schema schema = Schemas.getOrCreate(schemaName);
            List<GeneratedEntity> entities =
                    new JPAEntityCodeGenerator(config, dialect.getSqlTypeConverter())
                            .generateCode(schema);

            for (GeneratedEntity entity : entities) {
                Path file = outDir.resolve(entity.fileName());
                Files.writeString(file, entity.code());
                System.out.println("Wrote " + file);
            }
        }
    }
}
```

---

## Configuration Options

`JPAEntityGeneratorConfig` is built via a fluent builder:

| Option                     | Type                          | Default              | Description |
|----------------------------|-------------------------------|----------------------|-------------|
| `packageName`              | `String`                      | *required*           | Target Java package for generated classes |
| `useJakartaEENamespace`    | `boolean`                     | `true`               | `true` → `jakarta.persistence`, `false` → `javax.persistence` |
| `toOneFetchType`           | `FetchType` (`LAZY` / `EAGER`)| `EAGER`              | Fetch type applied to `@ManyToOne` / `@OneToOne` |
| `compositeKeyStrategy`     | `CompositeKeyStrategy`        | `ID_CLASS`           | How multi-column primary keys are represented (`ID_CLASS` or `EMBEDDED_ID`) |
| `generateEqualsAndHashCode`| `boolean`                     | `true`               | Whether to generate `equals()` and `hashCode()` |

Example:

```java
JPAEntityGeneratorConfig config = JPAEntityGeneratorConfig.builder()
        .packageName("com.mycompany.domain")
        .useJakartaEENamespace(true)
        .toOneFetchType(FetchType.LAZY)
        .compositeKeyStrategy(CompositeKeyStrategy.EMBEDDED_ID)
        .generateEqualsAndHashCode(true)
        .build();
```

---

## How It Works

1. **Lexer + Parser**  
   The SQL script is tokenized and parsed into a rich AST (`za.co.sindi.sql.sql2j.ast.*`).  
   The concrete parser is obtained from the chosen `SQLDialect`.

2. **SchemaBuilder**  
   An `ASTVisitor` walks the statements and builds a dialect-agnostic metamodel (`Schema`, `Table`, `Column`, `Enum`, `Record`).

3. **JPAEntityCodeGenerator**  
   For each table it:
   - Maps columns to Java fields using the dialect’s `SQLTypeConverter`
   - Collapses foreign-key columns into relationship fields
   - Generates companion classes for composite keys, enums, and composite types
   - Emits complete, compilable Java source

4. **Output**  
   Each generated artifact is returned as a `GeneratedEntity` record containing the file name and the source code string.

---

## Supported SQL Constructs

The schema builder and code generator process the following constructs (exact syntax support depends on the chosen dialect’s parser):

- `CREATE TABLE` (columns, constraints, inline references)
- `ALTER TABLE` (add/drop/rename column, change type, set/drop default, set/drop NOT NULL, add/drop constraint, rename table)
- `DROP TABLE`
- `CREATE TYPE` (enum and composite/record types)
- Column constraints: `NOT NULL`, `NULL`, `PRIMARY KEY`, `UNIQUE`, `DEFAULT`, `REFERENCES`, `CHECK`, auto-increment / serial types

Other statements (views, indexes, functions, triggers, etc.) are parsed into the AST where supported, but are currently ignored by the code generator.

---

## Type Mapping

Type mapping is **dialect-specific**. Each dialect implements `SQLTypeConverter` and decides how SQL types are translated to Java types.

Typical mappings across dialects include:

| SQL Concept                          | Common Java Mapping            |
|--------------------------------------|--------------------------------|
| 64-bit integer / bigserial / bigint  | `Long` / `long`                |
| 32-bit integer / serial / int        | `Integer` / `int`              |
| 16-bit integer / smallint            | `Short` / `short`              |
| Decimal / numeric                    | `BigDecimal`                   |
| Floating-point                       | `Float` / `Double`             |
| Boolean                              | `Boolean` / `boolean`          |
| Character / varchar / text           | `String`                       |
| Date                                 | `LocalDate`                    |
| Time                                 | `LocalTime`                    |
| Timestamp (without time zone)        | `LocalDateTime`                |
| Timestamp with time zone             | `OffsetDateTime`               |
| UUID                                 | `UUID`                         |
| Binary / blob / bytea                | `byte[]`                       |
| JSON / JSONB                         | `String` (or custom converter) |
| Named enum type                      | Generated Java `enum` + converter |
| Composite / record type              | Generated Java `record` + converter |
| Unknown type                         | `String` (flagged)             |

Nullable columns use wrapper types; non-nullable columns prefer primitives where applicable.

---

## Project Structure

```
za.co.sindi.sql.sql2j
├── SQLDialect.java              # Registry of available dialects
├── SQLParserFactory.java
├── ast/                         # Full DDL AST (dialect-agnostic)
├── lexer/
├── parser/                      # Dialect-specific parsers
├── codegen/
│   ├── JPAEntityCodeGenerator.java
│   ├── JPAEntityGeneratorConfig.java
│   ├── SchemaBuilder.java
│   ├── SQLTypeConverter.java          # Abstract base
│   ├── PostgreSQLTypeConverter.java   # PostgreSQL implementation
│   ├── GeneratedEntity.java
│   ├── CompositeKeyStrategy.java
│   └── metamodel/               # Schema, Table, Column, Enum, Record, Schemas
├── util/
└── utils/
```

---

## Extending the Library

### Adding a new RDBMS dialect

1. Implement a dialect-specific parser (and lexer if needed).
2. Create a `SQLParserFactory` that returns instances of that parser.
3. Implement `SQLTypeConverter` with the appropriate SQL → Java mappings.
4. Register the new dialect in the `SQLDialect` enum:

```java
public enum SQLDialect {
    POSTGRESQL(...),
    MYSQL(SQLParserFactory.mysql(), new MySQLTypeConverter()),
    // ...
}
```

### Custom code generation

You can reuse the parsed metamodel (`Schemas`, `Schema`, `Table`) and write your own generator (DTOs, records, Spring Data interfaces, OpenAPI models, etc.).

### Additional statement handling

Extend `SchemaBuilder` (it already implements `ASTVisitor`) to process views, indexes, or other constructs that your use case requires.

---

## Limitations & Roadmap

- PostgreSQL is the first fully implemented dialect; additional RDBMS support (starting with MySQL) is planned.
- The library is at an early stage (`0.0.1-SNAPSHOT`).
- Generated code assumes standard JPA; advanced features (inheritance strategies, secondary tables, etc.) are not yet generated.
- Requires Java 25.

Contributions and feedback are welcome via GitHub issues and pull requests.

---

## License

```
Copyright 2026 Sindi Technologies (Pty) Ltd

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```

---

## Author

**Buhake Sindi**  
Sindi Technologies (Pty) Ltd  
[https://www.sindi.co.za](https://www.sindi.co.za)
```
