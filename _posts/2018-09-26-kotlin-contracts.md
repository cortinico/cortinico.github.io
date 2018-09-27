---
title: "Discovering Kotlin Contracts"
categories: "Kotlin"

excerpt: "TODO"

header:
    overlay_filter: "0.5"
    overlay_image: "/assets/images/post-discovering-kotlin-contracts.jpg"
    show_overlay_excerpt: false
    teaser: "/assets/images/post-discovering-kotlin-contracts.jpg"
    caption: "Computer History Museum - SunnyVale"

# header:
# image: "/assets/images/post-discovering-kotlin-contracts.jpg"
---

<!-- TODO Probably rewrite? -->

The next release of Kotlin, 1.3, is really near. Among the several features, I really enjoyed one that will XXX to more clean code and better: Kotlin Contracts. In this blogpost we'll see what are Kotlin Contracts and how they can be useful to XXX

# The Problem

Currently, the following Kotlin code is invalid:

```kotlin
fun testMyTokenLenght() {
    val token : String? = getMyToken();

    assertNotNull(token)
    assertEquals(42, token.length)
}
```

Specifically, here we have a test to check that a `String` returned by a method is not `null` and has a specific length. This test will fail to compile on the `assertEquals` line with the following error:

Error: Only safe (?.) or non-null asserted (!!.) calls are allowed on a  nullable receiver of type String?
{: .notice--warning}

The error is related to the `token.length` expression. The problem is that the type of `token` is a `String?` and we need to explicitly check its nullability before accessing the `.length` property. To fix this error we could either wrap that line with an `if` check, use a safe call `(?.)` or a non-null asserted `(!!.)` calls as suggested by the compiler.

Actually, looking at the code we can be sure that `token` won't be nullable inside the `assertEquals` check. We are asserting that `token` shouldn't be nullable at the line above. 

Would it be cool if the Kotlin compiler could understand that and restrict the type of `token` from `String?` to `String` after the `assertNotNull` call? Unfortunately the complier of Kotlin 1.2.x is not smart enough to understand this type restriction (and you can't really provide any hint to the complier to instruct it).

Good news everyone! As of next week, that Kotlin code will compile. You can try it out on TryKotlin (TODO link) by switching the Kotlin release in the the drop-down menu on the bottom right to **1.3-RC1**. This is possible because of **Kotlin Contracts**, introduced in this last iteration of the language.

# Conference Driven Development

<!-- TODO Some timeline -->
To better understand what's going to happen next week, we need to give some timeline of the 1.3.x releases:

The first EAP (early access preview) of Kotlin 1.3, the next milestone of the Kotlin language was announced on [July the 26th](https://blog.jetbrains.com/kotlin/2018/07/see-whats-coming-in-kotlin-1-3-m1/). Contracts were actually introduced in the second EAP, release on [August the 8th](https://blog.jetbrains.com/kotlin/2018/08/kotlin-1-3-m2/). As of [September the 20th](https://blog.jetbrains.com/kotlin/2018/09/kotlin-1-3-rc-is-here-migrate-your-coroutines/) Kotlin 1.3 is in the Release Candidate phase, so the stable release should be really near. 

Next week [KotlinConf](https://kotlinconf.com/) will take place in Amsterdam. I guess we can all bet on Kotlin 1.3.0 being one of the Keynote announcement. As soon as you'll bump that version number inside your project, you can start using Contracts. Also several functions inside the `stdlib` will benefit of Koltin Contracts, let's see how.

# Kotlin Contracts

# Effects

# Returns 

# CallInPlace

# Limitations