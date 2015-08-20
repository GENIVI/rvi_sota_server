# Scala styleguide

The document contains Scala stylistic guidelines, which should be fillowed in order to produce readable and ideomatic code, allowing effective colobaration. The following style guides was taking as basis:

1. [Typesafe Scala Style Guide](http://docs.scala-lang.org/style/)
2. [Databricks Scala Guide](https://github.com/databricks/scala-style-guide)
3. [Twitter's Effective Scala](http://twitter.github.io/effectivescala/)

## <a name='TOC'>Table of Contents</a>
1. [Syntactic Style](#syntactic)
    - [Identation](#identation)
    - [Naming Convention](#naming)
    - [Declarations](#declarations)
    - [Imports](#imports)
    - [Control Structures](#control_structures)
    - [Infix Methods](#infix)
    - [Documentation Style](#documentation)
2. [Language Features](#lang)
    - [override Modifier](#override_modifier)
    - [Destructuring Binds](#destruct_bind)
    - [Symbolic Methods (Operator Overloading)](#symbolic_methods)
    - [Type Inference](#type_inference)
    - [Return Statements](#return)
    - [Recursion and Tail Recursion](#recursion)
    - [Exception Handling](#exception)
    - [Options](#option)
    - [Monadic Chaining](#chaining)
    - [Concurrency](#concurrency)
    - [Private Fields](#concurrency-private-this)
    - [Default Parameter Values](#java-default-param-values)

## <a name='syntactic'>Syntactic Style</a> ##

### <a name='identation'>Identation</a> ###
  * General identation: 2 spaces
  * Limit lines to 100 characters
  * If a single expression is longer than 100 characters it should be spitted into multiple expressions by assigning intermediate results to values. In case it is not practical and the expression should be wrapped across several lines, each successive line should be indented two spaces from the first.
  * When calling a method which takes numerous arguments (in the range of five or more), it is often necessary to wrap the method invocation onto multiple lines. In such cases, put each argument on a line by itself, indented two spaces from the current indent level:
  
  ```scala
  foo(
    someVeryLongFieldName,
    andAnotherVeryLongFieldName,
    "this is a string",
    3.1415)
  ```

### <a name='naming'>Naming Convention</a>

- Classes, traits should follow Java class convention, i.e. CamelCase style with the first letter capitalized.
  ```scala
  class RedFox
  
  trait LazyDog
  ```

- Objects follow the class naming convention except when attempting to mimic a package or a function.

    ```scala
    object ast {
      sealed trait Expr
      case class Plus(e1: Expr, e2: Expr) extends Expr
      ...
    }

    object inc {
      def apply(x: Int): Int = x + 1
    }
    ```

- Scala packages should follow the Java package naming conventions
  ```scala
  package com.databricks.resourcemanager
  ```

- Constant names should be in upper camel case. That is, if the member is final, immutable and it belongs to a package object or an object, it may be considered a constant (similar to Java’s static final members):
  ```scala
  object Foo {
    val Bar = 10
  }
  ```
- Enums should be CamelCase with first letter capitalized.
- Names for methods should be in the camelCase style with the first letter lower-case.

### <a name='declarations'>Declarations</a> ###

All guides referenced above agreed on how the declarations should be styled. Please refer to the one from [Scala Style Guide](http://docs.scala-lang.org/style/declarations.html), as is most comprehensive of them.

### <a name='imports'>Imports</a> ###
  * Do NOT use wildcard imports, unless you are importing implicit methods.
  * Always import packages using absolute paths

### <a name='control_structures'>Control Structures</a> ###
Control structures should be styled as defined in [Scala Style Guide](http://docs.scala-lang.org/style/control-structures.html)

### <a name='infix'>Infix Methods</a>

Do NOT use infix notation for methods that aren't symbolic methods (i.e. operator overloading).
```scala
// Correct
list.map(func)
string.contains("foo")

// Wrong
list map (func)
string contains "foo"

// But overloaded operators should be invoked in infix style
arrayBuffer += elem
```

### <a name='documentation'>Documentation Style</a> ###
* Use Java docs style instead of Scala docs style.
  ```scala
  /**
    * Style mandated by "Scala Style Guide"
    */
 
  /**
   * Style to use
   */
  ```
* Do not use @author tags since it does not encourage Collective Code Ownership.

## <a name='lang'>Language Features</a> ##

### <a name='override_modifier'>override Modifier</a>
Always add override modifier for methods, both for overriding concrete methods and implementing abstract methods. The Scala compiler does not require `override` for implementing abstract methods.

### <a name='destruct_bind'>Destructuring Binds</a>

Destructuring bind (sometimes called tuple extraction) is a convenient way to assign two variables in one expression.
```scala
val (a, b) = (1, 2)
```

However, do NOT use them in constructors, especially when `a` and `b` need to be marked transient. The Scala compiler generates an extra Tuple2 field that will not be transient for the above example.
```scala
class MyClass {
  // This will NOT work because the compiler generates a non-transient Tuple2
  // that points to both a and b.
  @transient private val (a, b) = someFuncThatReturnsTuple2()
}
```

### <a name='symbolic_methods'>Symbolic Methods (Operator Overloading)</a>

Avoid symbolic method names, unless you are defining them for natural arithmetic operations (e.g. `+`, `-`, `*`, `/`) or as part as some DSL (`!` to send message in Akka, `\` to concatenate parts of a path or an uri). In second case they should be defined as aliases to the non-symbolic functions.

### <a name='type_inference'>Type Inference</a>

Scala type inference, especially left-side type inference and closure inference, can make code more concise. That said, there are a few cases where explicit typing should be used:

- __Public methods should be explicitly typed__, otherwise the compiler's inferred type can often surprise you.
- __Implicit methods should be explicitly typed__, otherwise it can crash the Scala compiler with incremental compilation.
- __Variables or closures with non-obvious types should be explicitly typed__. A good litmus test is that explicit types should be used if a code reviewer cannot determine the type in 3 seconds.

### <a name='return'>Return Statements</a>

__Do NOT use return statement__.

### <a name='recursion'>Recursion and Tail Recursion</a>

Do NOT use recursion, unless the problem can be naturally framed recursively (e.g. graph traversal, tree traversal).

For functions that are meant to be tail recursive, apply `@tailrec` annotation to make sure the compiler can check it is tail recursive (you will be surprised how often seemingly tail recursive code is actually not tail recursive due to the use of closures and functional transformations.)

### <a name='exception'>Exception Handling</a> ###

- Do NOT catch Throwable or Exception. Use `scala.util.control.NonFatal`:

  ```scala
  try {
    ...
  } catch {
    case NonFatal(e) =>
      // handle exception; note that NonFatal does not match InterruptedException
    case e: InterruptedException =>
      // handle InterruptedException
  }
  ```

### <a name='option'>Options</a>

- Use `Option` when the value can be empty. Compared with `null`, an `Option` explicitly states in the API contract that the value can be `None`.
- When constructing an `Option`, use `Option` rather than `Some` to guard against `null` values.
  ```scala
  def myMethod1(input: String): Option[String] = Option(transform(input))
  
  // This is not as robust because transform can return null, and then 
  // myMethod2 will return Some(null).
  def myMethod2(input: String): Option[String] = Some(transform(input))
  ```
- Do not use `None` to represent exceptions.
- Do not call `get` directly on an `Option`.

### <a name='chaining'>Monadic Chaining</a>

One of Scala's powerful features is monadic chaining. Almost everything (e.g. collections, Option, Future, Try) is a monad and operations on them can be chained together. This is an incredibly powerful concept, but chaining should be used sparingly. In particular:

- Do NOT chain (and/or nest) more than one `flatMap` operations.
- If you need to chain more than one `flatMap` - use for-comprehension.

### <a name='concurrency'>Concurrency</a> ###

Use Akka for concurrency.

### <a name='concurrency-private-this'>Private Fields</a>

Note that `private` fields are still accessible by other instances of the same class, so protecting it with `this.synchronized` (or just `synchronized`) is not technically sufficient. Make the field `private[this]` instead.
```scala
// The following is still unsafe.
class Foo {
  private var count: Int = 0
  def inc(): Unit = synchronized { count + 1 }
}

// The following is safe.
class Foo {
  private[this] var count: Int = 0
  def inc(): Unit = synchronized { count + 1 }
}
```

### <a name='java-default-param-values'>Default Parameter Values</a>

Do NOT use default parameter values. Overload the method instead.
```scala
// Breaks Java interoperability
def sample(ratio: Double, withReplacement: Boolean = false): RDD[T] = { ... }

// The following two work
def sample(ratio: Double, withReplacement: Boolean): RDD[T] = { ... }
def sample(ratio: Double): RDD[T] = sample(ratio, withReplacement = false)
```

