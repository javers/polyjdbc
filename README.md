# PolyJDBC

[![Build Status](https://travis-ci.org/polyjdbc/polyjdbc.png?branch=master)](https://travis-ci.org/polyjdbc/polyjdbc)

PolyJDBC is a polyglot, lightweight wrapper around standard JDBC drivers with
schema inspection/creation capabilities.

See project wiki for changelog.

## Why?

When developing [SmartParam](http://smartparam.org) JDBC repository i realized
that i was writing polyglot JDBC wrapper instead of focusing on core JDBC repository responsibilities.
PolyJDBC came to life by extracting low-level wrapping code to separate project.

Before starting work on SmartParam JDBC repo i was looking for a good JDBC wrapper
that would do more than suppress SQLExceptions. Specifically i was looking for
transaction-aware wrapper that would also make it easy to perform DDL operations.

## Target

PolyJDBC primary target are libraries that need light and cross-platform JDBC
persistence. Hibernate is great, but it leaves little place for end-user customization,
which is important when offering a library. It is also quite heavy. PolyJDBC size is
only 75kB, no dependencies except for slf4j logging API.

## Features

* polyglot (or better poly-dialect? multiple DB dialect support, including id generation stategies)
* transaction-oriented
* resources (Statements, ResultSets) are managed inside transaction scope
* intiutive transaction commit/rollback support
* DDL operation DSL (CREATE TABLE/INDEX/SEQUENCE with constraints, DROP \*)
* SQL query DSL (INSERT, SELECT, UPDATE, DELETE)
* lightweight
* schema inspection (table/sequence exists?)
* schema alteration

## Supported database engines

* H2
* PostgreSQL
* MySQL
* Oracle (without limit/offset in SELECT)

Thanks to [testng](http://testng.org/) magic PolyJDBC runs integration tests
on each database. This way i can be sure that all features all supported across
all engines.

## How to get it?

```xml
<dependency>
    <groupId>org.polyjdbc</groupId>
    <artifactId>polyjdbc</artifactId>
    <version>0.3.0</version>
</dependency>
```

## Enough, show me the code

### Querying

Simple usage of low-level query runner:

```java
Dialect dialect = DialectRegistry.H2.getDialect();
PolyJDBC polyjdbc = new PolyJDBC(dataSource, dialect);

QueryRunner queryRunner = null;

try {
    queryRunner = polyjdbc.queryRunner();
    SelectQuery query = QueryFactory.selectAll().from("test").where("year = :year")
        .withArgument("year", 2013).limit(10);
    List<Test> tests = queryRunner.selectList(query, new TestMapper());
}
finally {
    polyjdbc.close(queryRunner);
}
```

**QueryRunner** is created per transaction and should always be closed.
 **try-finally** is there to make sure that resources are really freed. If you want to
perform operations without runner create/close boilerplate use reusable **SimpleQueryRunner**:

```java
Dialect dialect = DialectRegistry.H2.getDialect();
PolyJDBC polyjdbc = new PolyJDBC(dataSource, dialect);

SelectQuery query = QueryFactory.selectAll().from("test").where("name = :name")
        .withArgument("name", "test");

Test test = polyjdbc.simpleQueryRunner().queryUnique(query, new TestMapper());
```

**SimpleQueryRunner** performs each query in new transaction. If you need to perform
custom (or multiple) operations use reusable **TransactionRunner**:

```java
Dialect dialect = DialectRegistry.H2.getDialect();
PolyJDBC polyjdbc = new PolyJDBC(dataSource, dialect);

TransactionRunner transactionRunner = polyjdbc.transactionRunner();

Test test = transactionRunner.run(new TransactionWrapper<Test>() {
    @Override
    public Test perform(QueryRunner queryRunner) {
        SelectQuery query = QueryFactory.selectAll().from("test").where("name = :name")
            .withArgument("name", "test");
        return queryRunner.queryUnique(query, new TestMapper());
    }
});

transactionRunner.run(new VoidTransactionWrapper() {
    @Override
    public void performVoid(QueryRunner queryRunner) {
        DeleteQuery query = QueryFactory.delete().from("test").where("year < :year")
            .withArgument("year", 2012);
        queryRunner.delete(query);
    }
});
```

### Schema management

PolyJDBC comes with tools for schema creating and deletion. More options for
schema inspection are planned, although there is no concrete release date.

To check if relation exists:

```java
Dialect dialect = DialectRegistry.H2.getDialect();
PolyJDBC polyjdbc = new PolyJDBC(dataSource, dialect);

SchemaInspector schemaInspector = null;
try {
    schemaInspector = polyjdbc.schemaInspector();
    boolean relationExists = schemaInspector.relationExists("testRelation");
} finally {
    polyjdbc.close(schemaManager);
}
```

To create new schema (group of relations):

```java
Dialect dialect = DialectRegistry.H2.getDialect();
PolyJDBC polyjdbc = new PolyJDBC(dataSource, dialect);

SchemaManager schemaManager = null;
try {
    schemaManager = polyjdbc.schemaManager();

    Schema schema = new Schema(configuration.getDialect());
    schema.addRelation("test_one")
        .withAttribute().longAttr("id").withAdditionalModifiers("AUTO_INCREMENT").notNull().and()
        .withAttribute().string("name").withMaxLength(200).notNull().unique().and()
        .withAttribute().integer("age").notNull().and()
        .primaryKey("pk_test_one").using("id").and()
        .build();
    schema.addSequence("seq_test_one").build();
    schema.addRelation("test_two")
        .withAttribute().longAttr("id").withAdditionalModifiers("AUTO_INCREMENT").notNull().and()
        .withAttribute().longAttr("fk_test_one").notNull().and()
        .foreignKey("fk_test_one_id").references("test_one", "id").on("fk_test_one").and()
        .build();
    schema.addSequence("seq_test_two").build();

    schemaManager.create(schema);
} finally {
    polyjdbc.close(schemaManager);
}
```

You don't need to define `Schema` object. Single `Relation`, `Sequence` or `Index` can be
created using `SchemaManager` as well.

## License

PolyJDBC is published under [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0).
