---
layout: page
title: "Database Style Guide"
category: dev
date: 2015-08-26 10:40:49
---

### Table names are UpperCamelCase Singular.

Table names should be the same as the Scala domain object that represents them (if it exists). By using the same casing rules as Scala, the domain object and SQL table names can match exactly.

For more arguments on the singular/plural naming, see http://stackoverflow.com/questions/338156/table-naming-dilemma-singular-vs-plural-names

### Column names are lowerCamelCase

Lower camel case column names should match scala property names.

### Surrogate primary keys are called 'id'.

For example:

```sql
    -- Good
    CREATE TABLE Person (
      id int PRIMARY KEY,
      ...
    );
```

rather than:

```sql
    -- BAD
    CREATE TABLE Person (
      personId int PRIMARY KEY, -- BAD: should be 'id'
      ...
    );
```
