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

(TODO MEME)

If you're writing JUnit tests on a daily basis, you probably experienced that moment when you realize that you're **duplicating some code** (maybe some setup code or a group of assertions) across multiple tests. You might decide to create a superclass to share all the common code in order to avoid code duplication.

That's probably going to be **a bad idea**...

In this blog post, we'll see how to create a JUnit `Rule` in Kotlin, and how to use `@Annotations` to beautify it (TODO).

# Why inheritance in your tests is a bad idea

Your tests are one of the primary **source of documentation** of your codebase. You want them to be clear and **self explanatory**. Ideally you should be able to print them on a paper a reader should be able to understand them.

Having a test that inherits from a superclass is going against this. You force a reader to open **another class** to understand what's the real behavior of a test. This _hidden logic_ can be really annoying and can lead to a lot of headaches when debugging a test.

```kotlin
TODO class MyCursedTest() : SuperTest() {

	@Test fun testSomething() {
		assertEquals(what can go wrong, everything);
	}
}
```

Moreover, you're adding a superclass with the intention of sharing code, but the problem is that you're not making it **reusable**. Let's say that you have your group of tests, `Test1.kt`, `Test2.kt` with a superclass that has the code to TODO (`TODOTest.kt`). Then you have another group of tests `TestA.kt`, `TestB.kt` with another superclass that has the code to TODO (`TODOTest.kt`). What if tomorrow you want to write another test that has both initialization. Unfortunately you can't because Kotlin/Java don't support **multiple inheritance** (you can have only one superclass).

The JUnit framework offers us some tools:

* An Annotation to run a method **before all** the test in a file (`@BeforeClass` in JUnit4 or `@BeforeAll` in JUnit5)
* An Annotation to run a method **before every** test in a file (`@Before` in JUnit4 or `@BeforeEach` in JUnit5)
* An Annotation to run a method **after all** the test in a file (`@AfterClass` in JUnit4 or `@AfterAll` in JUnit5)
* An Annotation to run a method **after every** test in a file (`@After` in JUnit4 or `@AfterEach` in JUnit5)

But the best tool to reuse code are **Rules**. JUnit Rules are simple way to modify the behavior of all the tests in a class.

The JUnit defines them in this way:

> Rules can do everything that could be done previously with methods annotated with @Before, @After, @BeforeClass, or @AfterClass, but they are more powerful, and more easily shared between projects and classes.

# A simple JUnit @Rule

To create a Rule, you need to implement the `TestRule` interface. This interface has just one method: `apply` method. With this method you can specify _how_ your Rule should modify the test execution.

You can think at the `apply` conceptually as a `map()`. It takes a `Statement` as input and returns another `Statement` as output.

Let's see an example of a Rule that will TODO:

```kotlin
BLA BLA BLA BLA
```

Here you can see that we modify the test to do TODO

Using your Rule will be just one line of code:

```
@get:Rule TODO
```

The `@Rule` annotation will make sure your Rule is executed **Before every test** (to understand how @Before, @BeforeClass, @After, @AfterClass and @Rule interacts each other, see TODO). 

# Annotations + @Rule = <3

Great! So now you know how to write Rules! 

What about if I want to customize my rule for every single test? For example, I might want this rule to TODO.

You might have noticed that there is one small detail that I left behind: the `apply` method has **two parameters**. The second parameter is a `Description`. This 



.
.
.

# A simple Android Rule (TODO)

# Conclusions
