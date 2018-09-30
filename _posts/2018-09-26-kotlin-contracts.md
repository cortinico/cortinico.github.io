---
title: "Discovering Kotlin Contracts"
categories: "Kotlin"

excerpt: "Let's discover Kotlin Contracts, one of the new features of Kotlin 1.3 aimed to smooth the interactions between developers and the complier."

header:
    # overlay_filter: "0.6"
    image: "/assets/images/posts/header-kotlin-contracts.jpg"
    # show_overlay_excerpt: false
    teaser: "/assets/images/posts/header-kotlin-contracts.jpg"
    caption: "Hook Lighthouse - Ireland"
---

<!-- TODO Probably rewrite? -->

The next release of Kotlin, 1.3, is really near! Among the several features, I really enjoyed one that will allow library developers to write cleaner and better code: Kotlin Contracts. In this blogpost we'll see what are Kotlin Contracts and how they can be useful to XXX

# The Problem

Currently, the following Kotlin code is invalid:

```kotlin
@Test fun testMyTokenLenght() {
    val token : String? = getMyToken();

    assertNotNull(token)
    assertEquals(42, token.length)
}
```

<!-- TODO Change Specifically -->
Specifically, here we have a test to check that a `String` returned by a method is not `null` and has a specific length. This test will fail to compile on the `assertEquals` line with the following error:

Error: Only safe (?.) or non-null asserted (!!.) calls are allowed on a  nullable receiver of type String?
{: .notice--warning}

The error is related to the `token.length` expression. The problem is that the type of `token` is a `String?` and we need to explicitly check its nullability before accessing the `.length` property. To fix this error we could either wrap that line with an `if` check, use a safe call `(?.)` or a non-null asserted `(!!.)` calls as suggested by the compiler.

Actually, looking at the code we can be sure that `token` won't be nullable inside the `assertEquals` check. We are asserting that `token` shouldn't be nullable at the line above. 

<!-- TODO Too long question -->
Would it be cool if the Kotlin compiler could understand that and restrict the type of `token` from `String?` to `String` after the `assertNotNull` call? Unfortunately the complier of Kotlin 1.2.x is not smart enough to understand this type restriction (and you can't really provide any hint to the complier to instruct it).

Good news everyone! As of next week, that Kotlin code will compile. You can try it out on TryKotlin (TODO link) by switching the Kotlin release in the the drop-down menu on the bottom right to **1.3-RC1**. This is possible because of **Kotlin Contracts**, introduced in this last iteration of the language.

# Conference Driven Development

To better understand what's going to happen next week, we need to give some timeline of the 1.3.x releases:

The first EAP (early access preview) of Kotlin 1.3, the next milestone of the Kotlin language was announced on [July the 26th](https://blog.jetbrains.com/kotlin/2018/07/see-whats-coming-in-kotlin-1-3-m1/). Contracts were actually introduced in the second EAP, release on [August the 8th](https://blog.jetbrains.com/kotlin/2018/08/kotlin-1-3-m2/). As of [September the 20th](https://blog.jetbrains.com/kotlin/2018/09/kotlin-1-3-rc-is-here-migrate-your-coroutines/) Kotlin 1.3 is in the Release Candidate phase, so the stable release should be really near. 

Next week [KotlinConf](https://kotlinconf.com/) will take place in Amsterdam. I guess we can all bet on Kotlin 1.3.0 being one of the Keynote announcement. As soon as you'll bump that version number inside your project, you can start using Contracts. Also several functions inside the `stdlib` will benefit of Kotlin Contracts, let's see how.

# Kotlin Contracts

The problem in the Example (TODO number) is coming from the fact that you, developer, know something that the Kotlin compiler doesn't know (yet).

Although the Kotlin compiler turns out to be really smart in type inference or in [smart casting](https://kotlinlang.org/docs/reference/typecasts.html), it's not capable to understand that after an `assertNotNull` call the type of the receiver can be restricted.

Kotlin Contracts are exactly solving this kind of problem. With a contract you can actually provide **knowledge about a function behavior** to the compiler in order to help it performing a **more complete analysis** on your code, with the final goal to **write cleaner code**.

To better understand Kotlin contracts, we need to introduce the concept of **effect**.

## Effects

An effect represent a **piece of knowledge** related to a function behavior. A Kotlin contract is nothing more than a **collection of effects** attached to a function to enrich the compiler analysis. Whenever you call a function, all of his effects will be **fired**. 

<!-- TODO fired rewrite -->

The definition of effect here is a [bit vague](https://github.com/Kotlin/KEEP/blob/3490e847fe51aa6deb869654029a5a514638700e/proposals/kotlin-contracts.md#effects), and looks like is intentional to do not restrict the scope of contracts. 

However, in the current release of Kotlin, there are **4** supported effects (see [Effects.kt](https://github.com/JetBrains/kotlin/blob/7b66a4d295eb625af3b066476e8f7171a6276501/libraries/stdlib/src/kotlin/contracts/Effect.kt)):

* `Returns(value)` Represents that the function returned successfully (e.g. without throwing an exception). You can also optionally pass a `value` (of type `Any?`) to represents which value the function returned.
* `ReturnsNotNull` Represents that the function returned successfully a non nullable value.
* `ConditionalEffect(effect, expr)` Represents a composition of an Effect and a boolean expression, guaranteed to be true if the effects is fired (see the [paragraph below](#conditionaleffect)).
* `CallsInPlace(lambda, kind)` Represents a constraint on place and number of invocation of the passed `lambda` parameter (see the [paragraph below](#callsinplace)).

### ConditionalEffect

The first two effects (`Returns` and `ReturnsNotNull`) are usually composed with a **boolean expression** to form a `ConditionalEffect`. If we say a function has a contract with a `ConditionalEffect(e, b)` means that whenever the effect `e` will be fired, the boolean expression `b` is guaranteed to be true. From the logical point of view this can be summarized with a logical implication:

```
Effect => Boolean Expression
```

So for example, I can say that whenever a function **returns** his parameter is **not null** (that's exactly what we would love to achieve for the `assertNotNull` function).

Please note that in the current release of Kotlin Contracts there are several limitations on the `Returns(value)` and on the boolean expression

For boolean expression only null checks (`!= null`, `== null`), instance checks (`is`, `!is`) and logic operators (`&&`, `||`, `!`) are supported.
{: .notice--info}

For `Returns(value)` only `true`, `false` and `null` are supported.
{: .notice--info}

### CallsInPlace

The `CallsInPlace(lambda, kind)` effect allows you to provide constraints on when/where/and how often the provided **lambda will be called**.

Specifically this effect will imply that:

* `lambda` _will not be called after the call to owner-function is finished_. In other words, the `lambda` will be called locally and will not be deferred before the function returns.

* `lambda` will not be passed to another function that doesn't have a similar contract. 

* if the `kind` parameter is provided, the `lambda` will be invoked a specified amount of times. `kind` can be one of:
    - `AT_MOST_ONCE`
    - `EXACTLY_ONCE`
    - `AT_LEAST_ONCE`
    - `UNKNOWN` (the default).

To summarize, `CallsInPlace` turns to be handy when you're having a function with a lambda as a parameter, and you don't expect that lambda to be executed concurrently.

See the [examples](#examples) below to have a concrete example of this effect.

# Syntax

To use contracts in your code, you need to use the **ContractDsl** (defined in the [ContractBuilder.kt](https://github.com/JetBrains/kotlin/blob/7b66a4d295eb625af3b066476e8f7171a6276501/libraries/stdlib/src/kotlin/contracts/ContractBuilder.kt) file).

To define a contract, you should use the `contract` function from the stdlib. This function must be **the first statement** of your function:

```kotlin
fun Int?.isNullOrZero(): Boolean {
    contract { 
        // Contract effects.
    }
    // Body of your function.
}
```

The stdlib has several functions to help you express your effects (`return`, `returnNotNull` and `callsInPlace`). Finally you can use the infix `implies` function to compose one effect with a boolean expression.

```kotlin
fun Int?.isValid(): Boolean {
    contract { 
        returns(true) implies (this@isValid != null)
    }
    return this != null && this != 0
}
```

Here we are saying that a call to `isNullOrZero` that returns false, will have as consequence that the receiver is not null:

```kotlin
val aInt : Int? = getAnInt()
if (!aInt.isValid()){
    // Here the compiler knows that `aInt` is not nullable
    // thanks to the contract on `isValid`
} else {
    // Here the compiler has no extra information since
    // the `isValid` has only a `returns(true)` effect
}
```

Please note that contracts **are not evaluated** as expressions. You should consider them as annotations for the compiler and they are ignored at **runtime**. If you try to breakpoint inside the contract, you will notice that your debugger will never stop there ()

![kotlin contracts debugger](/assets/images/posts/kotlin-contract-1.png)

# Examples

Let's see some examples of usage of the Kotlin contracts. A great source of inspiration is definitely the stdlib. A lot of functions have already been enriched with several contracts.

## `assertTrue`, `assertFalse`, `assertNotNull`

Inside the `kotlin-test` package we can find several functions to write more idiomatic JUnit tests. Some of those have been enriched with contracts to make your testing code cleaner: `assertTrue`, `assertFalse` and `assertNotNull`.

```kotlin
fun assertTrue(actual: Boolean, message: String? = null) {
    contract { returns() implies actual }
    return asserter.assertTrue(message ?: "Expected value to be true.", actual)
}

fun assertFalse(actual: Boolean, message: String? = null) {
    contract { returns() implies (!actual) }
    return asserter.assertTrue(message ?: "Expected value to be false.", !actual)
}

fun <T : Any> assertNotNull(actual: T?, message: String? = null): T {
    contract { returns() implies (actual != null) }
    asserter.assertNotNull(message, actual)
    return actual!!
}
```

If you don't want to use the `kotlin-test` package, you can still define your own assert* wrappers that delegates to the JUnit functions:

```kotlin
fun assertNotNull(actual: Any?) {
    contract { returns() implies (actual != null) }
    org.junit.Assert.assertNotNull(actual)
}
```

## `run`

The `run` function is a simple and clear example to understand how the `callsInPlace` effect works. The `run` function simply accepts a lambda and executes it. In Kotlin 1.2.x the following code will not compile:

```kotlin
@Test fun testMyToken() {
    val token : String?
    run {
        token = getMyToken()
    }
    assertNotNull(token)
}
```

Specifically will fail to compile with the following error:

Error:(20, 12) Captured values initialization is forbidden due to possible reassignment
Error:(22, 16) Variable 'token' must be initialized
TODO Line number
{: .notice--warning}

The compiler is not able to assure that:
* The lambda passed to `run` is not executed more than once. This would cause the `val` to be reassigned.
* The lambda passed to `run` is ever executed more than once. If the lambda is not executed, the `token` var might be not initialized.

Starting from 1.3, `run` has a `callInPlace` contract with an `EXACTLY_ONCE` invocation kind. Thanks to this the compiler knows that the lambda you pass will be executed exactly once. 

```kotlin
public inline fun <T, R> T.run(block: T.() -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}
```

You can find more examples related to Kotlin contracts in the `samples.contract` package of the stdlib ([here on GitHub](https://github.com/JetBrains/kotlin/blob/8f209fa667de0af2b5ed8bc80c8868a1534300e9/libraries/stdlib/samples/test/samples/contracts/contracts.kt)).

# Limitations

* The biggest limitation of contracts is definitely that contracts are **not verified**. In other words: **You're responsible of writing correct contracts**. If you add a contract with an effect that doesn't reflect the real behavior of a function, you might experience crashes or unexpected behavior.

* `returns(value)` and `implies` boolean expression allowed values are really limited (see the [ConditionalEffect](#conditionaleffect) paragraph).

* Contracts can be applied only to **top level** functions (as inheritance is not supported yet).

* Contracts can be applied only to **block** functions (as it needs to be the first statement in the body of a function).

# Conclusions

<!-- More conclusions -->

Kotlin Contracts are a great tool to enrich the complier analysis and they can be really helpful to write cleaner and better code. However, contracts are not verified and can lead to misleading results if not written properly.

They are anyway setting the foundation for a (hopefully) long term relationship between Kotlin developers and the Kotlin compiler.

# References

* [Kotlin Contracts KEEP](https://github.com/Kotlin/KEEP/blob/3490e847fe51aa6deb869654029a5a514638700e/proposals/kotlin-contracts.md)
* [Kotlin Contracts KEEP #140](https://github.com/Kotlin/KEEP/pull/140)
* [Kotlin Contracts stdlib source code](https://github.com/JetBrains/kotlin/tree/7b66a4d295eb625af3b066476e8f7171a6276501/libraries/stdlib/src/kotlin/contracts)
* [meet Kotlin 1.3-M2 - JetBrains Blog](https://blog.jetbrains.com/kotlin/2018/08/kotlin-1-3-m2/)