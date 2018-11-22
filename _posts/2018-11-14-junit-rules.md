---
title: "Don't be lazy, use @Rules"
categories: "Kotlin"

excerpt: "TODO"

header:
    image: "/assets/images/posts/header-junit-rules.jpg"
    teaser: "/assets/images/posts/teaser-junit-rules.jpg"
    caption: "Golden Gate Bridge - SF"
---

Today I saw yet another JUnit test that was inheriting from a superclass...this is me just after:

![no go please no](/assets/images/posts/junit-rules-1.gif)

If you're writing JUnit tests on a daily basis, you probably experienced that moment when you realize that you're **duplicating some code** (maybe some setup code or a group of assertions) across multiple tests. You might think that, in order to avoid code duplication, you want to create a superclass to share all the common code.

That's probably **not** going to be a great idea...

In this blog post, we'll see how to create a JUnit `Rule` in Kotlin, and how to use annotations to make them easy to configure and have more elegant tests.

# Why inheritance in your tests is a bad idea

Your tests are one of the primary **source of documentation** of your codebase. You want them to be clear and **self explanatory**. Ideally, you should be able to print them on a paper a reader should be able to understand them.

Having a test that inherits from a superclass is going against this. You force a reader to open **another class** to understand what's the real behavior of a test. This _hidden logic_ can be really annoying and can lead to a lot of headaches when debugging a test.

```kotlin
class Test() : AbstractTest() {
    @Test fun testSomething() {
        assertEquals("what can", "go wrong");
    }
} // TODO Check my syntax
```
(TODO Example font) For example, this test might look obviously failing but is not. But inside the `AbstractTest` class, the developer could have re-defined the `assertEquals(String, String)` to check if the two parameters have the same length rather than the same content.

Moreover, you're adding a superclass with the intention of sharing code, but the problem is that you're not making it **reusable**.

Let's say that you have your group of tests, `ItLocaleTest.kt`, `DeLocaleTest.kt`, with a superclass (say `AbstractLocaleTest.kt`) that has the code to setup the locale for your testing environment. Then you have another group of tests say `LoggedUserTest.kt`, `AnoymousUserTest.kt` with another superclass (say `AbstractUserTest.kt`) that has the code to setup the authentication token for your user. 

What if tomorrow you want to write another test that has both initialization, like a logged in user with the FR locale? Unfortunately you can't because Kotlin/Java don't support **multiple inheritance** (your test can have only one superclass).

The JUnit framework offers us some tools:

* An Annotation to run a method **before all** the test in a file (`@BeforeClass` in JUnit4 or `@BeforeAll` in JUnit5)
* An Annotation to run a method **before every** test in a file (`@Before` in JUnit4 or `@BeforeEach` in JUnit5)
* An Annotation to run a method **after all** the test in a file (`@AfterClass` in JUnit4 or `@AfterAll` in JUnit5)
* An Annotation to run a method **after every** test in a file (`@After` in JUnit4 or `@AfterEach` in JUnit5)

But the best tool to reuse code are **Rules**. JUnit Rules are simple way to modify the behavior of all the tests in a class.

The JUnit defines them in this way:

> Rules can do everything that could be done previously with methods annotated with @Before, @After, @BeforeClass, or @AfterClass, but they are more powerful, and more easily shared between projects and classes.

# A simple JUnit @Rule

TODO JUnit Disclaimer

To create a Rule, you need to implement the `TestRule` interface. This interface has just one method: `apply` method. With this method you can specify _how_ your Rule should modify the test execution.

You can think at the `apply` conceptually as a `map()`. It takes a `Statement` as input and returns another `Statement` as output.

Let's see an example of an Rule that will not modify the test:

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

```
@get:Rule val timingRule = LogTimingRule()
```

The `@Rule` annotation will make sure your Rule is executed **Before every test** (to understand how @Before, @BeforeClass, @After, @AfterClass and @Rule interacts each other, see TODO - Maybe skip).

TODO Note on the @get:Rule.

# Annotations + @Rule = <3

Great! So now you know how to write Rules! 

What about if I want to customize my rule for every single test? For example, I might want to turn on the logging of the timing only for some specific test.

You might have noticed that there is one small detail that I left behind: the `apply` method has **two parameters**. The second parameter is a `Description`. This parameter gives us access to a lot of metadata for every test. A way to customize our Rule to be more flexible for every test is to use **annotations**, and the `Description` has exactly all the methods to give us this support.

Let's modify the `LogTimingRule` to have the logging disabled by default for every test, and to have it enabled only for tests annotated with `@LogTiming`. First we create the new annotation:

```kotlin
annotation class LogTiming
```

Then we can update the `LogTimingRule` in this way:

```
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

Here we get all the annotations from the `Description` field, we filter them getting only the `LogTiming` with the `filterIsInstance` method, and we set the `enabled` flag to true if the result is not empty (please note that also the `@Test` annotation will be inside the `.annotations` collection).

Now we can simply use our annotation on top of our test to enable logging!

```kotlin 
class MySampleTest {
    @get:Rule val rule = LogTimingRule()

    @Test
    @LogTiming
    fun testSomething() {
        assertEquals(2, 1 + 1)
    }
```

# A Real world example

Let's see a real world example of a JUnit Rule. This rule will **retry failed tests** a number of times provided inside an annotation on top of the test. The idea behind this Rule is to mitigate the impact of **flaky tests**.

TODO Flaky test definition

Flaky tests can be really annoying, especially when you have several tests and your test suite takes several minutes to re-run. Ideally you would love to avoid flakiness at all but is not always possible (e.g. on Android sometimes is really hard). With this Rule you can annotate the tests you know as being more flaky and they will re-run a defined amount of time if they fail.

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

Again we filter the annotations getting only the `RetryOnFailure` and we get the first result. If the result is missing, the `firstOrNull` method will return null and thanks to the elvis operator (`?:`) we'll default the field to 0.

Then we can loop through the `retryCount` till we get a success:

```
var failureCause: Throwable? = null
for (i in 0 until retryCount + 1) {
    try {
        statement.evaluate()
        return
    } catch (t: Throwable) {
        failureCause = t
    }
}
```

As soon as we get a success we return. If we get a failure, we store the `Throwable` as we want to print it later if the test fails:

```kotlin
println("Test ${description.methodName} - Giving up after ${retryCount + 1} attemps")
failureCause?.printStackTrace()
```

The complete code of the RetryRule is here:

```kotlin

```

# Conclusions


# References

* TODO
* TODO
* TODO