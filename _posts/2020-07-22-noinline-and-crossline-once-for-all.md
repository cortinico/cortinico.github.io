---
title: "Kotlin's Noinline & Crossline, once for all"
categories: "Kotlin"

excerpt: "Explaining the Kotlin's noinline and crossinline keywords, with a simple example, once for all."

header:
    image: "./assets/images/posts/noinline-and-crossline-once-for-all/header.jpg"
    teaser: "./assets/images/posts/noinline-and-crossline-once-for-all/teaser.jpg"
    caption: "Keikyū Line Haneda Station - Tokyo, Japan"
---

Kotlin, as most of the programming languages, has several **reserved keywords**. 

You can find all of them listed on this page of the official documentation: [keyword-reference](https://kotlinlang.org/docs/reference/keyword-reference.html).

<figure>
<a target="_blank" href="https://kotlinlang.org/docs/reference/keyword-reference.html">
  <img src="/assets/images/posts/noinline-and-crossline-once-for-all/keyword-reference-screenshot.png" alt="keyword reference screenshot"></a>
</figure>

I invite you to spend some time going through the list. Do you know all the keywords?

If you wrote some Kotlin code, you probably encountered most of them. Some keywords in that list though are **less commonly used**, therefore they could be harder to remember. One example: the `noinline` and the `crossinline` keywords.
 
The keyword reference gives the following definitions:
- `noinline` _turns off inlining of a lambda passed to an inline function_ 
- `crossinline` _forbids non-local returns in a lambda passed to an inline function_

The latter can sound a bit complicated for an inexperienced developer. Given that those keywords are not frequently used in daily development, remembering the exact semantic could be harder. If you speak with other Kotlin developers, chances are that they never used the `crossinline` keyword, and they probably don't **know when to use it**.

The [official documentation](https://kotlinlang.org/docs/reference/inline-functions.html) can help understand those keywords, but in this case, I believe an example is worth a thousand words.

In this blog-post, we will learn the semantic of the `noinline` and `crossinline` keyword, with a simple example using the [Kotlin Playground](http://play.kotlinlang.org/) (with snippets that you can run and edit in your browser to follow along the article).

# inline

To explain those keywords, we have to first explain the `inline` keyword, and the **Inline Functions** feature of Kotlin.

Let's start with an example: a function to do something **if you're running on Debug**:

```kotlin
fun doOnDebug(block: () -> Unit) {
    // On Android you can replace with a BuildConfig.DEBUG
    if (Env.DEBUG) {
        block()
    }
}

fun main() {
    doOnDebug {
        println("I'm on debug ¯\\_(ツ)_/¯")
    }
}

object Env {
    val DEBUG = true
}
```

The function is trivial: it takes a lambda as a parameter and executes it if you execute on a Debug environment (we are simulating it using the `Boolean DEBUG` property).
 
Using the **Show Kotlin Bytecode** feature of IntelliJ, we can see how the equivalent bytecode code will look like:

<figure>
  <img src="/assets/images/posts/noinline-and-crossline-once-for-all/bytecode-fun.png" alt="decompiled bytecode of a regular function with lambda parameters">
  <figcaption>Decompiled bytecode of a regular function with lambda parameters</figcaption>
</figure>

{% capture notice-info %}
Are you wondering what that `(Function0)null.INSTANCE` means? 🤨 You're not alone. 

There is a bug ([IDEABKL-7385](https://youtrack.jetbrains.com/issue/IDEABKL-7385)) in the Kotlin Bytecode decompiler of IntelliJ that causes the lambda class to be lost (and replaced with a `null`). 

If you use [JD-GUI](http://java-decompiler.github.io/) to analyze the bytecode instead, you will see that the parameters is actually `Foo$main$1.INSTANCE` ([decompiled class with JD GUI](/assets/images/posts/noinline-and-crossline-once-for-all/jdgui1.png)) and the resulting lambda is also decompiled correctly with the corresponding `.INSTANCE` field ([decompiled lambda class with JD GUI](/assets/images/posts/noinline-and-crossline-once-for-all/jdgui2.png)). 
{% endcapture %}

<div class="notice--info">{{ notice-info | markdownify }}</div>

As we can see from the image, the `doOnDebug` function is compiled to a function that takes a `Function0` as a single parameter. `Function0` is an interface declared in the [Kotlin Standard Library](https://github.com/JetBrains/kotlin/blob/d8f701ee61d052eb1ec43927b8686a13f0fbb5aa/libraries/stdlib/jvm/runtime/kotlin/jvm/functions/Functions.kt#L10-L14) for the JVM. It's a functional interface, used to pass a function that takes zero parameters from Java to Kotlin.

This means that for every `doOnDebug{}` invocation we **create an instance** of a `Function0` causing memory allocation. This creates **runtime overhead** that can be problematic, especially if you use a lot of high-order functions with lambdas like this one.

Kotlin offers the Inline Functions feature exactly to **mitigate** this overhead. If we add the `inline` keyword in front of the function definition:

```kotlin
inline fun doOnDebug(block: () -> Unit)
```

The resulted bytecode will look like this:

<figure>
  <img src="/assets/images/posts/noinline-and-crossline-once-for-all/bytecode-inline.png" alt="decompiled bytecode of an inline function">
  <figcaption>Decompiled bytecode of an inline function</figcaption>
</figure>

As we can see from the bytecode, the `doOnDebug` code is **inlined** in the `main()` function body. The **lambda parameter** is affected as well as its body is also copied.

> In other words, inline will _copy-paste_ the function body, and the lambda parameter at every call site.

This means that we can avoid creating an instance of a `Function0` at all, resulting in saved memory allocation.

To recap, the `inline` keywords allows you to reduce the runtime overhead of functions taking lambda as parameters by:
* **Inlining their body** at every call site.
* **Inlining every lambda parameter** at the call site.

Please note that inlining functions can increase the size of your generated code, so make sure you do it only for small functions.
{: .notice--warning}

The full example is available in this playground (you can edit the code and run it as well):

{% raw %}
<iframe class="plkotlin" src="https://pl.kotl.in/R6aL3MYCh?theme=darcula&to=12&from=1"></iframe>
{% endraw %}

With `noinline` and `crossinline` you can then **customize** the `inline` behavior, let's see how.  

# noinline

Let's now extend our example with some logging on the `main()` function:

```kotlin
fun main() {
    println("START main()")
    doOnDebug {
        println("I'm on debug ¯\\_(ツ)_/¯")
    }
    println("END main()")
}
```

And let's add some logging to `doOnDebug` as well.
For `doOnDebug`, since it's a util function, we also want to specify which **logger** to use.
 
For the sake of simplicity, a logger will be just a simple function:

```kotlin
logger: (String) -> Unit
```

So we can extend the `doOnDebug` with invocations of this logger.

Moreover, we also want to **flush** it at the end of the function. 
Let's assume we invoke a `flush` function:

```kotlin
inline fun doOnDebug(
    logger: (String) -> Unit,
    block: () -> Unit
) {
    if (Env.DEBUG) {
        logger("[LOG] Running doOnDebug...")
        block()
        logger("[LOG] Flushing the log...")
        flush(logger)
    }
}

fun flush(logger: (String) -> Unit) { 
    // Flush the logger here
}
```

As it is right now, this code won't compile. It will fail with the following error message:

`Illegal usage of inline-parameter 'logger' in 'public inline fun doOnDebug(logger: (String) -> Unit = ..., block: () -> Unit): Unit defined in root package in file File.kt'. Add 'noinline' modifier to the parameter declaration`
{: .notice--danger}

The error message suggests we add the `noinline` keyword to the `logger` parameter. Adding this will make the code compile:

```
inline fun doOnDebug(
    noinline logger: (String) -> Unit,
```

Why is this needed?

As previously mentioned, the `inline` keyword affects the function body and **all the lambda parameters**. 
In our example, `logger` is inlined as well. 

Inlining a lambda **limits what you can do** with it. You can only invoke it. If you wish to store it in a variable or **pass them to another function** (as we do for the `flush` function) you need to tell the compiler to avoid inlining it.

If we try to run, the console output now will be:

```
START main()
[LOG] Running doOnDebug...
I'm on debug ¯\_(ツ)_/¯
[LOG] Flushing the log...
END main()
```

To recap, the `noinline` keyword is a mechanism to **prevent the inlining** of a specific lambda parameter of an inline function. It's useful if you wish to pass the lambda around or store it in a variable.   

The full example until here is available in this playground:

{% raw %}
<iframe class="plkotlin" src="https://pl.kotl.in/6KTWLikqk?theme=darcula&to=19&from=1"></iframe>
{% endraw %}

# crossinline

Let's go back to the call site, and complicate our function a bit more.

First, we add a `times: Int = 1` parameters, to allow repeating the lambda multiple times:

```kotlin
inline fun doOnDebug(
    noinline logger: (String) -> Unit,
    times : Int = 1,
    block: () -> Unit
) {
    if (Env.DEBUG) {
        logger("[LOG] Running doOnDebug...")
        repeat(times) {
            logger("[LOG] Iteration #$it...")
            block()
        }
        logger("[LOG] Flushing the log...")
        flush(logger)
    }
}
```

Then, our code contains a [katakana](https://en.wiktionary.org/wiki/%E3%83%84) symbol: ツ. Let's say we want to play defensive and avoid printing if the terminal doesn't support UTF-8.

Let's update the `doOnDebug` call site like this:

```kotlin
doOnDebug {
    if(!Env.UTF8_SUPPORT) {
        return
    }
    println("I'm on debug ¯\\_(ツ)_/¯")
}
```

As long as `Env.UTF8_SUPPORT` is `true`, everything goes smoothly. Here is what is printed on the console:

```
START main()
[LOG] Running doOnDebug...
[LOG] Iteration #0...
I'm on debug ¯\_(ツ)_/¯
[LOG] Flushing the log...
END main()
```

But if you try to change `Env.UTF8_SUPPORT` to `false`, this will happen:

```
START main()
[LOG] Running doOnDebug...
[LOG] Iteration #0...
```

Seems like the execution terminated at the `return`, but the flushing of the logger and `END main()` line are missing 🧐.

## Time to (de)-bug

To fully understand what is going on, let's introduce a bug 🐛. 

Instead of `Env.UTF8_SUPPORT` returning always either `true` or `false`, let's call `Random`. 
We can introduce some flakyness by letting `Env.UTF8_SUPPORT` return `true` only 80% of the times:

```kotlin
val UTF8_SUPPORT : Boolean get() = Random.nextInt(5) != 0
```

Now let's try to call `doOnDebug` with `repeat = 10` and see what's the output on the console. 
We expect to see on the average ~9 shrug on the screen:

```
Run main()
[LOG] Running doOnDebug
[LOG] Iteration 0
¯\_(ツ)_/¯
[LOG] Iteration 1
¯\_(ツ)_/¯
[LOG] Iteration 2
```

What happens instead is that in my run, after two iterations, `UTF8_SUPPORT` is `false` and the whole execution is halted.

What is happening here?

Given that the lambda parameter of `doOnReturn` is inlined, also the `return` is inlined at the call site. This means that in the resulting code, that `return` is causing a return of the **outer function**, the `main()`. 

This is called a **non-local return** in Kotlin.

To prevent this behavior we can mark the parameter `block` as `crossinline`. This keyword **prevents non-local returns** in the specified lambda parameter.

If we try to apply it to our function:

```kotlin
inline fun doOnDebug(
    noinline logger: (String) -> Unit,
    times: Int = 1,
    crossinline block: () -> Unit
```

We will see that our code will not compile anymore with the following error message:

`'return' is not allowed here`
{: .notice--danger}

The IDE autocompletion is also suggesting you to use a [labeled return](https://kotlinlang.org/docs/reference/returns.html#return-at-labels): `return@doOnDebug`. That will make sure your return will apply only to the `doOnDebug` function and not to the `main()` function.


<figure>
  <img src="/assets/images/posts/noinline-and-crossline-once-for-all/ide-labeled-return.png" alt="IDE Labelled Return">
  <figcaption>IntelliJ suggesting you to use a labeled return</figcaption>
</figure>

The full example of this article is available in this playground:

{% raw %}
<iframe class="plkotlin" src="https://pl.kotl.in/sFIBUhG3p?theme=darcula&to=28&from=3"></iframe>
{% endraw %}

I hope that with some examples, the `inline`, `noinline`, and `crossinline` keywords are clearer now.

Happy inlining 🚇

# References

- [Inline Functions](https://kotlinlang.org/docs/reference/inline-functions.html)
- [Keyword Reference](https://kotlinlang.org/docs/reference/keyword-reference.html)
- [Returns and Jumps](https://kotlinlang.org/docs/reference/returns.html)