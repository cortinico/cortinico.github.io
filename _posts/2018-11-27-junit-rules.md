---
title: "Don't be lazy, use @Rules"
categories: "Kotlin"

excerpt: "Code reuse is fundamental, especially in tests. In this blogposts we'll see how to write a JUnit Rule in order to share your code between tests."

header:
    image: "/assets/images/posts/header-junit-rules.jpg"
    teaser: "/assets/images/posts/teaser-junit-rules.jpg"
    caption: "Golden Gate Bridge - San Francisco, California"
---

Today I saw yet another JUnit test that was extending a superclass...this is me just after:

![no go please no](/assets/images/posts/junit-rules-1.gif)

If you're writing JUnit tests on a daily basis, you probably experienced that moment when you realize that you're **duplicating some code** (maybe some set up code or a group of assertions) across multiple tests. You might think that, in order to avoid code duplication, you can create a superclass to share all the common code.

That's probably **not** going to be a great idea...

In this blog post, we'll see how to create a JUnit `Rule` in Kotlin, and how to use annotations to make them easy to configure and have more elegant tests.

# Why inheritance in your tests is a bad idea

Your tests are one of the primary **sources of documentation** of your codebase. You want them to be clear and **self-explanatory**. Ideally, you should be able to print them on a paper a reader should be able to understand them.

Having a test that inherits from a superclass is going against this. You force a reader to open **another class** to understand what's the real behavior of a test. This _hidden logic_ can be really annoying and can lead to a lot of headaches when debugging a test.

```kotlin
class Test : AbstractTest() {
    @Test fun testSomething() {
        assertEquals("what can", "go wrong")
    }
}
```
For example, this test might look obviously failing but could be not. Inside the `AbstractTest` class, the developer could have re-defined the `assertEquals(String, String)` to check if the two parameters have the same length rather than the same content.

Moreover, you're adding a superclass with the intention of sharing code, but the problem is that you're not making it **reusable**.

Let's say that you have your group of tests, `ItLocaleTest.kt`, `DeLocaleTest.kt`, with a superclass (say `AbstractLocaleTest.kt`) that has the code to set up the locale for your testing environment. Then you have another group of tests say `LoggedUserTest.kt`, `AnoymousUserTest.kt` with another superclass (say `AbstractUserTest.kt`) that has the code to set up the authentication token for your user. 

What if tomorrow you want to write a test that has both initializations, like a logged in user with the FR locale? Unfortunately, you can't because Kotlin/Java don't support **multiple inheritance** (your test can have only one superclass).

So in this case you probably have to use [composition over inheritance](https://en.wikipedia.org/wiki/Composition_over_inheritance). You need to extract your initialization logic in some helper class that will be used by all the tests that needs it. In this way you can **compose** multiple helper classes and combine all the initializations you need and you're not stuck with just one single superclass.

The JUnit framework offers us some tools to provide initialization code:

* An Annotation to run a method **before all** the test in a file (`@BeforeClass` in JUnit4 or `@BeforeAll` in JUnit5)
* An Annotation to run a method **before every** test in a file (`@Before` in JUnit4 or `@BeforeEach` in JUnit5)
* An Annotation to run a method **after all** the test in a file (`@AfterClass` in JUnit4 or `@AfterAll` in JUnit5)
* An Annotation to run a method **after every** test in a file (`@After` in JUnit4 or `@AfterEach` in JUnit5)

But the best tool to reuse code are **Rules**. JUnit Rules are a simple way to modify the behavior of all the tests in a class.

The JUnit defines them in this way:

> Rules can do everything that could be done previously with methods annotated with @Before, @After, @BeforeClass, or @AfterClass, but they are more powerful, and more easily shared between projects and classes.

Multiple rules can be **combined** together with a [RuleChain](https://junit.org/junit4/javadoc/4.12/org/junit/rules/RuleChain.html) allowing us to create initialization code that can be easily combined, reused, and distributed across projects and teams.

# A simple JUnit @Rule

Please note that those examples apply only for JUnit4 as JUnit5 requires a `minSdkVersion` of `26` or above for Instrumentation tests on Android (which is not the case for several apps). Rules have been replaced by the [Extension API](https://junit.org/junit5/docs/current/user-guide/#extensions) in JUnit5.
{: .notice--warning}

To create a Rule, you need to implement the `TestRule` interface. This interface has just one method: `apply`. With this method, you can specify _how_ your Rule should modify the test execution.

You can see the `apply` conceptually as a `map()`. It takes a `Statement` as input and returns another `Statement` as output.

Let's see an example of a Rule that will do nothing:

```kotlin
class EmptyRule : TestRule {
    override fun apply(s: Statement, d: Description): Statement {
        return s
    }
}
```

While something more interesting might be a Rule that prints the execution time of every test:

```kotlin
class LogTimingRule : TestRule {
    override fun apply(s: Statement, d: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                // Do something before the test.
                val startTime = System.currentTimeMillis()
                try {
                    // Execute the test.
                    s.evaluate()
                } finally {
                    // Do something after the test.
                    val endTime = System.currentTimeMillis()
                    println("${d.methodName} took ${endTime - startTime} ms)")
                }
            }
        }
    }
}
```

Here you can see that we modify the test to return a new `Statement` that will record the start time, execute the old statement, and print the elapsed time on the console.

Using this Rule will be just one line of code:

```kotlin
@get:Rule val timingRule = LogTimingRule()
```

The `@Rule` annotation will make sure your Rule is executed **Before every test**.

# Annotations + @Rule = <3

Great! So now we know how to write Rules.

What about if you want to **customize** your rule for every single test? For example, I might want to turn on the logging of the timing only for some specific test.

You might have noticed that there is one small detail that I left behind: the `apply` method has **two parameters**. 

The second parameter is a `Description`. This parameter gives us access to some metadata for every test. A way to customize our Rule to be more flexible for every test is to use **annotations**, and the `Description` class has exactly all the methods to give us this support.

Let's modify the `LogTimingRule` to have the logging disabled by default for every test, and to have it **enabled only for tests annotated** with the `@LogTiming` annotation. 

First, we create the new annotation:

```kotlin
annotation class LogTiming
```

Then we can update the `LogTimingRule` in this way:

```kotlin
class LogTimingRule : TestRule {
    override fun apply(statement: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                var enabled = description
                        .annotations
                        .filterIsInstance<LogTiming>()
                        .isNotEmpty()

                val startTime = System.currentTimeMillis()
                try {
                    statement.evaluate()
                } finally {
                    if (enabled) {
                        val endTime = System.currentTimeMillis()
                        println("${description.methodName} took ${endTime - startTime} ms)")
                    }
                }
            }
        }
    }
}
```

The `apply` function is pretty similar to before. We just added an `enabled` flag to check if we should print the time or not. The interesting part is obviously this:

```kotlin
var enabled = description
        .annotations
        .filterIsInstance<LogTiming>()
        .isNotEmpty()
```

Here we get all the annotations from the `description` field, we filter them getting only the `LogTiming` with the `filterIsInstance` method, and we set the `enabled` flag to true if the result is not empty (please note that also the `@Test` annotation will be inside the `.annotations` collection).

Now we can simply use our annotation on top of our test to enable logging only for the tests we are interested in!

```kotlin 
class MySampleTest {
    @get:Rule val rule = LogTimingRule()

    @Test
    @LogTiming
    fun testSomething() {
        assertEquals(2, 1 + 1)
    }
}
```

# A Real-world example

Let's see a real-world example of a JUnit Rule. This rule will **retry failed tests** a number of times provided inside an annotation on top of the test. The idea behind this Rule is to mitigate the impact of **flaky tests**.

Flaky tests are tests that can either pass or fail on the same code, given the same configuration/status.
{: .notice--info}

Flaky tests can be really annoying, especially when you have several tests and your test suite takes several minutes to re-run. Ideally, you would love to avoid flakiness at all but is not always possible (e.g. on Android sometimes is really hard). With this Rule, you can annotate the tests you know as being flakier and they will re-run a defined amount of time if they fail.

Let's start as before, with an annotation. This time we also want to pass a parameter, the `retryCount`:

```kotlin
annotation class RetryOnFailure(val retryCount: Int)
```

This time we need an integer value inside the Rule to count how many times we need to retry the test:

```kotlin
val retryCount = description
    .annotations
    .filterIsInstance<RetryOnFailure>()
    .firstOrNull()
    ?.retryCount ?: 0
```

Again we filter the annotations getting only the `RetryOnFailure` and we get the first result. If the result is missing, the `firstOrNull` method will return null and thanks to the elvis operator (`?:`) we'll default the value to 0.

Then we can try to run the test `retryCount + 1` times till we get a success:

```kotlin
repeat(retryCount + 1) { _ ->
    runCatching { statement.evaluate() }
            .onSuccess { return }
            .onFailure { failureCause = it }
}
```

As soon as we get a success we return. If we get a failure, we store the `Throwable` as we want to print it later if the test fails:

```kotlin
println("Test ${description.methodName} - Giving up after ${retryCount + 1} attemps")
failureCause?.printStackTrace()
```

The complete code of the RetryRule is here:

```kotlin
class RetryRule : TestRule {
    override fun apply(statement: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                val retryCount = description
                        .annotations
                        .filterIsInstance<RetryOnFailure>()
                        .firstOrNull()
                        ?.retryCount ?: 0

                var failureCause: Throwable? = null

                repeat(retryCount + 1) { _ ->
                    runCatching { statement.evaluate() }
                            .onSuccess { return }
                            .onFailure { failureCause = it }
                }

                println("Test ${description.methodName} - Giving up after ${retryCount + 1} attemps")
                failureCause?.printStackTrace()
            }
        }
    }
}
```

# Use the source, Luke!

I've collected the code of those rules on a Maven package. You can use them just by adding this line to your gradle file:

```groovy 
dependencies {
    // For JUnit Tests
    testImplementation 'com.ncorti:rules4android:1.0.0'

    // For Instrumentation Tests
    androidTestImplementation 'com.ncorti:rules4android:1.0.0'
}
```

And you can find the source code on GitHub: [cortinico/rules4android](https://github.com/cortinico/rules4android).

# On method execution order

You're probably wondering how your `@Rule` interacts with all the other annotations provided by JUnit: `@Before`, `@After`, `@BeforeClass`, `@AfterClass` and `@ClassRule`. The better way to discover it is just to try:

```kotlin
class OrderTest {

    companion object {
        @get:ClassRule @JvmStatic val printRule = PrintRule("@ClassRule")
        @BeforeClass @JvmStatic fun beforeClass() = println(" @BeforeClass")
        @AfterClass @JvmStatic fun afterClass() = println(" @AfterClass")
    }

    @get:Rule val rule = PrintRule("  @Rule")

    @Before fun before() = println("   @Before")

    @After fun after() = println("   @After")

    @Test fun testSometing() = println("    @Test testSomething")

    @Test fun testSomethingElse() = println("    @Test testSomethingElse")
}
```

So I assume to have a [`PrintRule`](https://gist.github.com/cortinico/7a65aa2b05a72ae00624007a0ea4616d) that prints a line before and after the execution of the `Statement`. The output on the console is:

```java
@ClassRule before statement
 @BeforeClass
  @Rule before statement
   @Before
    @Test testSomething
   @After
  @Rule after statement
  @Rule before statement
   @Before
    @Test testSomethingElse
   @After
  @Rule after statement
 @AfterClass
@ClassRule after statement
```

So we can obviously see that:

* `Class` annotations are executed only **once** per test file (as you would expect).
* `@Rule` annotations are wrapping the `@After` and `@Before` executions.
* `@ClassRule` annotations are wrapping the `@AfterClass` and `@BeforeClass` executions.

Make sure to understand the execution order of JUnit methods, in order to don't get mad with debugging. Finally, don't forget that you can use a `RuleChain` to combine multiple rules and to define their order.

# Appendix: On @get:Rule

You're probably also wondering why do we need to use `@get:Rule` if you're using Kotlin and not just `@Rule` as you would do in Java.

```kotlin
@get:Rule val timingRule = LogTimingRule()
```

JUnit needs to have access to your rule, so it needs to be **public**. If you remove the `@get:` from the annotation, the test runner will fail with:

org.junit.internal.runners.rules.ValidationError: The @Rule 'timingRule' must be public.
{: .notice--warning}

This might look weird as the `timingRule` is actually public. But what is happening is that by default the `@Rule` annotation is applied to the **property target**, that is ignored by the JUnit runner. Kotlin allows you to specify the [target of your annotations](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-target/index.html) so in this case we need to specify the target to be **the getter**.

Alternatively, you can instruct the compiler to do not generate a property with the `@JvmField` annotation:

```kotlin
@Rule @JvmField val timingRule = LogTimingRule()
```

In this way, getters and setters for `timingRule` won't be created and it will be exposed as a field.

# Conclusions

Do you want to reuse your testing code? Create a JUnit Rule! 

Inheritance here is not generally a good idea. You might have the _illusion_ you're reusing code, but you'll probably end up in problems really soon. In your testing code, prefer composition over inheritance.

**Don't be lazy** and start writing your Rules today! 💪

If you want to talk more about testing, you can reach me out as [@cortinico on Twitter<i class="fab fa-twitter"></i>](https://twitter.com/cortinico).

# References

* [JUnit4 Wiki - Rules](https://github.com/junit-team/junit4/wiki/rules)
* [JUnit4 Wiki - Test Fixtures](https://github.com/junit-team/junit4/wiki/Test-fixtures)
* [JUnit API - Rule](https://junit.org/junit4/javadoc/4.12/org/junit/Rule.html)
