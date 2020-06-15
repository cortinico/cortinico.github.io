---
title: "Name Mangling in Kotlin"
categories: "Kotlin"

excerpt: "Let's discover two scenarios where name mangling is used in the Kotlin Compiler: inline classes and the internal modifier"

header:
    image: "/assets/images/posts/header-name-mangling.jpg"
    teaser: "/assets/images/posts/teaser-name-mangling.jpg"
    caption: "Baščaršija - Sarajevo, Bosnia and Herzegovina"
---

<blockquote>
    <h2>Mangle</h2>
    <i>Verb</i><br/>
    /ˈmæŋ.ɡəl/<br/><br/>
    <i>To destroy something by twisting it with force or tearing it into pieces so that its original form is completely changed.<br/>
    <small><a href="https://dictionary.cambridge.org/dictionary/english/mangle">Cambridge Dictionary</a></small></i>
</blockquote>

If you've played a bit around with Kotlin, chances are that you faced **name mangling** during your development.

Name mangling is a technique used by the Kotlin Compiler to alter the name of identifiers (e.g. function or variable names). This technique can be used to make  identifiers harder to access in the bytecode.

I discovered name mangling while preparing the release `v3.1.0` of [Chucker](https://github.com/ChuckerTeam/chucker). Before releasing a new version of a library, I generally inspect the API surface to make sure I'm not introducing any unintended change to the API with a tool like [japicmp](https://github.com/siom79/japicmp).

This time I noticed that I was removing a method and adding a new one, and that was not expected, as it will result in a breaking change 🤨.

This was due to name mangling. Let's see two scenarios where name mangling is used inside the Kotlin compiler: **inline classes** and the **internal modifier**.

# Inline Classes

[Inline Classes](https://kotlinlang.org/docs/reference/inline-classes.html) have been introduced as [experimental in Kotlin 1.3](https://kotlinlang.org/docs/reference/whatsnew13.html). They are a great tool to easily create **wrap types**, without introducing runtime overheads due to type wrapping/unwrapping. 

An example could be a having `Username`/`Password` inline classes to wrap `String` values.

So for Example, your login function:

```kotlin
fun login(username: String, password: String)
```

could be improved using inline classes in:

```kotlin
inline class Username(val value: String)
inline class Password(val value: String)

fun login(username: Username, password: Password)
```

Inline classes provide **type safety** at compile type, so you're sure you're not mixing up username and passwords of your users.

To overcome wrapping/unwrapping overhead, the Kotlin compiler is generating bytecode that uses the **underlying type** of the inline class.

This could lead to problematic scenarios. For example, if you happen to have a method overload with an inline class:

```kotlin
fun validate(password: String)
fun validate(password: Password)
```

The Kotlin compiler would have to generate two functions with the same signature.

To avoid such scenarios, the compiler is mangling the name of functions that are using inline classes by adding an hashcode to the function name.

So the previous `validate(Password)` function will be compile to a `validate-<hashcode>(String)` to avoid the signature clash with the `validate(String)` function. It can easily be seen using the _Show Kotlin Bytecode_ feature of IntelliJ:

```java
public final void validate(@NotNull String password) {
    ...
}

public final void validate_3mN7H_Y/* $FF was: validate-3mN7H-Y*/(@NotNull String password) {
    ...
}
```

It's interesting to note that the `-` char introduced by the mangling is an invalid character in Java. This is making the function that accepts an inline class **impossible to use from Java**.

This is actually by design, but it's worth noting when creating APIs that should be consumed by Java users.

# Internal Modifier

Kotlin gave us a new visibility modifier: `internal`. If you're not familiar with this modifier: it restricts the visibility of a declaration to the same [module](https://kotlinlang.org/docs/reference/visibility-modifiers.html#modules).

The `internal` modifier is handy when dealing with modularized projects, since it allows to easily contain the exposed API. Unfortunately, there is no support for the `internal` modifier neither in Java nor in the bytecode.

`internal` declarations in Kotlin are compiled to **`public` declarations** in Java. In other words: your `internal` function, classes, interfaces, variables etc. are accessible by Java classes **outside** of your module. 

As you can imagine, this is not great, especially if you're migrating a codebase from Java to Kotlin and you're making an extensive use of the `internal` keyword.

Luckily, the Kotlin compiler is using name mangling on `internal` identifiers to make them harder to call from Java. Names are rewritten appending the module name to them. So for example the following code: 

```kotlin
// inside the module named `library`
class UserDatabase {
    internal fun deleteUsers()
}
```

will compile to this equivalent Java code:

```java
public final class UserDatabase {
   public final void deleteUsers$library() {
   }
}
```

so you could potentially call `deleteUsers` in this way in Java:

```java
new UserInterface().deleteUsers$library();
```

Having the `$library` token inside the identifier, should warn developers that this is an internal method and should not be used.

Please note that this is also true when you're developing a Kotlin library, that could be consumed by Java projects. In other words: your `internal` methods will appear to the Java user of your library in their **IDE auto-complete**.

If you happen to use IntelliJ, there is a built-in code inspection that will raise a warning for usages of Kotlin `internal` declarations (that could potentially be suppressed with a `@SuppressWarnings("KotlinInternalInJava")`).

![kotlin internal in Java warning usage](/assets/images/posts/name-mangling-1.png)

## The Chucker Case

Chucker was recently [rewritten in Kotlin](/blog/introducing-chucker). We leveraged the `internal` keyword extensively to limit the visibility of our classes/methods.

One of our users on Github, [reported a build failure](https://github.com/ChuckerTeam/chucker/issues/134) with his Gradle build when using our library:

```
Execution failed for task ':app:mergeDebugJavaResource'.
> A failure occurred while executing com.android.build.gradle.internal.tasks.Workers$ActionFacade
   > More than one file was found with OS independent path 'META-INF/library_release.kotlin_module'
```

This failure is caused by a name clash with the `.kotlin_module` file. For every Kotlin module, the Kotlin compiler is generating `META-INF/<module name>.kotlin_module` file to store metadata from the compilation (more info on `kotlin_module` file [here](https://blog.jetbrains.com/kotlin/2015/06/improving-java-interop-top-level-functions-and-properties/)). 

The gradle module of Chucker is called `library`. As you can image, other libraries are also using the same module name. This is causing the `More than one file was found` error previously mentioned.

The [fix](https://github.com/ChuckerTeam/chucker/pull/146) was to pass the `-module-name` compiler flag, to change the name of our module at compile time:

```
compileOptions {
    kotlinOptions.freeCompilerArgs += ['-module-name', "com.github.ChuckerTeam.Chucker.library"]
}
```

This will make sure that our `.kotlin_module` is not clashing with others'.

The side effect of adding this compiler flag was also that signatures of our internal methods **changed**. So for example the method:

```kotlin
class RetentionManager {
    internal fun doMaintenance()
}
```

changed his signature from:

```java
final public void doMaintenance$library_release()
```

to 

```java
final public void doMaintenance$com_github_ChuckerTeam_Chucker_library()
```

as it was correctly spotted by japicmp: 

![japicmp report for RetentionManager](/assets/images/posts/name-mangling-2.png)

This was nothing major to worry about but rather something interesting to be aware of. I honestly don't expect users of our library to accidentally access internal methods (and if they do, it's on their own risk).

# Conclusions

Name mangling is often used by the Kotlin compiler to address Java interoperability with Kotlin. Specifically it's used to:

* Prevent methods that are using **inline classes** from being used from Java.
* Making **internal declarations** harder to accidentally being used from Java.

I hope you enjoyed this post and if you want to talk more about Kotlin, please reach me out as [@cortinico on Twitter<i class="fab fa-twitter"></i>](https://twitter.com/cortinico).

# References

* [Name mangling - Wikipedia](https://en.wikipedia.org/wiki/Name_mangling)
* [Inline Classes - Kotlinlang](https://kotlinlang.org/docs/reference/inline-classes.html)
* [Java to Kotlin Interop - Kotlinlang](https://kotlinlang.org/docs/reference/java-to-kotlin-interop.html)
