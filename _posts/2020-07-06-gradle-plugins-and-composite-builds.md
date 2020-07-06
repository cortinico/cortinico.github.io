---
title: "Gradle Plugins and Composite Builds"
categories: "Kotlin"

excerpt: "How Composite Builds can simplify your Gradle build, especially if you're building a custom Gradle Plugin."

header:
    image: "./assets/images/posts/gradle-plugins-and-composite-builds/header.jpg"
    teaser: "./assets/images/posts/gradle-plugins-and-composite-builds/teaser.jpg"
    caption: "Tierpark Hagenbeck - Hamburg - Germany"
---

Over the last couple of weeks, I had the opportunity to help other Android/Kotlin developers struggling with Gradle 🐘, specifically with Gradle [custom plugins](https://docs.gradle.org/current/userguide/custom_plugins.html). To my surprise, none of them knew about a feature of Gradle called [composite builds](https://docs.gradle.org/current/userguide/composite_builds.html). 

**Composite builds** are a great tool to support the development of custom Gradle plugin as they help to streamline the development cycle. 

In this blog-post, we will discover the composite build feature, and how it can support you when writing Gradle plugins. Lately, we will walk through the **pros & cons** of this approach.

All the examples presented in this blog-post comes from this **Github template**, that contains a setup for a custom Gradle plugin using composite builds:

[cortinico/kotlin-gradle-plugin-template](https://github.com/cortinico/kotlin-gradle-plugin-template)

All the examples presented in this blog-post as well as the linked template use Gradle Kotlin DSL (.gradle.kts files). If you're not familiar with it, you can find useful material in the [official documentation](https://docs.gradle.org/current/userguide/kotlin_dsl.html).
{: .notice--info}

## Sharing your build logic

> The more your project grows, the bigger your Gradle files will get. 

After some time, you might want to **share** your build logic between different modules. Maybe you want to configure a static analysis tool or publish several modules, and you don't want to copy-and-paste your config all-around your `build.gradle.kts` files.

Initially you could think about refactoring the common build logic with an external build script, say `common.gradle.kts`. You can then apply the external build script wherever you need it with:

```kotlin
apply(from = "./common.gradle.kts")
```

While this approach is definitely the easiest, it's not much portable. Sharing a build script might be enough for a single project, but for bigger projects this could not be a viable option.

External build scripts are also limited in several ways when using Gradle Kotlin DSL (see [here](https://docs.gradle.org/current/userguide/writing_build_scripts.html#sec:configuring_arbitrary_objects_using_an_external_script) and [here](https://discuss.gradle.org/t/plugins-and-apply-from-in-the-kotlin-dsl/28662)).

Gradle gets you covered with an API to get **full access** to your build: [Custom Gradle Plugins](https://docs.gradle.org/current/userguide/custom_plugins.html)

With a custom Gradle plugin you can apply all the desired customization such as creating new tasks, managing dependencies, etc. They are more portable than external script, as you can reuse them across different projects and modules. Moreover, you can also **publish** them to the [Gradle Plugin Portal](https://plugins.gradle.org/) so that other developers can use them easily in their builds.

## What are Gradle plugins?

Gradle plugins are a fundamental component of the Gradle ecosystem. If you haven't written one, you certainly already used them in your build. 

You can apply them using either the Gradle **Plugin DSL**:

```kotlin
plugins {
  id("io.gitlab.arturbosch.detekt") version "..."
}
```

or the `buildscript{}` block: 
 
```kotlin
buildscript {
    repositories {
        gradlePluginPortal()
    }
    dependencies {
        classpath("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:...")
    }
}
apply(plugin = "io.gitlab.arturbosch.detekt")
```

When creating your custom Gradle plugin, you need to decide **where** to place it:

- **Build script**. The least flexible solution as the custom plugin will live only within the `.gradle` script file where you place it.
- **[Precompiled script plugins](https://docs.gradle.org/current/userguide/custom_plugins.html#sec:precompiled_plugins)**. An intermediate approach as it allows to expose your custom plugin to other modules in your project. 
- **Standalone project**. The most flexible solution that allows you to reuse your Gradle plugin across different projects.

My rule of thumb here is: if your plugin applies customization **specific to your project** (e.g., customizing detekt with your own desired flags) go for a precompiled script plugin (and place it in your `buildSrc` folder). 

If **others can benefit** from your plugin, or you just need to use it between different projects, you need to place it in a **standalone project**. 

For the sake of simplicity this blog-post will focus only on gradle plugin in standalone project and composite builds. You can find more documentation on precompiled script plugins in the [official Gradle documentation](https://docs.gradle.org/current/userguide/custom_plugins.html#sec:precompiled_plugins).
{: .notice--info}

Here composite builds play a fundamental role, let's see how. 

## Why composite builds?

You can think of a Gradle plugin **as a Java library**. You will distribute it as a `.jar` file that Gradle collects and applies during a build. As a Java library, you can upload it to a Maven Repository with a `group-id`, an `artifact-id`, and a version number.

When setting up a repository for a new Java library, you often have a `library` module where your library code lives and an `example` project that uses your library. 

A standalone project for a custom Gradle plugin is **no different**. You will have a `plugin` module with the plugin code inside and an `example` that uses the plugin. Your project structured will look like this:

```
.
├── build.gradle.kts
├── settings.gradle.kts
├── example
│   ├── src // Your example project here
│   └── build.gradle.kts
└── plugin
    ├── src // Your plugin code here
    └── build.gradle.kts
``` 

Say you want to create a Gradle plugin that adds a simple `printExample` task that **prints a message** on a file. 

You will invoke it in this way:

```bash
./gradlew example:printExample
```

and you set up your `example/build.gradle.kts` to look like this one:

{% highlight kotlin linenos %}
buildscript {
  repositories {
    mavenLocal()
  }
  dependencies {
    classpath("com.example:my-gradle-plugin:0.0.1-SNAPSHOT")
  }
}
apply(plugin = "com.my.gradle.plugin")
{% endhighlight %}

Ideally, you would love your development cycle to look like this:
1. **Edit** the source code of the plugin in the `plugin` folder,
1. **Invoke** the `./gradlew example:printExample` task,
1. **See** the result of the invoked task with your change.

This unfortunately will not work because of your dependency on `mavenLocal()`. Changing line 6 of the example to `classpath project(':plugin')` to directly depend on a project module will also not work.

The reason is simple to explain: you're building a Gradle plugin. The plugin must be available to Gradle before the build starts. You can't compile the plugin and use it in the same build invocation 🔃. 

### The "hacky" solution

You could end up with invoking a command like this to overcome this problem:

```bash
./gradlew library:publishToMavenLocal && ./gradlew example:printExample
```

This has a couple of drawbacks:

1. It involves **two Gradle invocations**. This means you have to wait for 2 configuration phases with an obvious impact on performance.
1. It involves **publishing to Maven Local**. This has an impact on the _reproducibility_ of your build. A developer might have stale artifacts on Maven Local that bleed inside the build and provide unexpected build results.
1. Depending on how you use your custom plugin, the configuration phase of the first invocation might just fail and **prevent you from publishing** a fix to the failure.

The solution to this is using **composite builds**. They allow depending on a plugin project without having to publish it locally to Maven Local.

## Setting up a composite build

With [composite builds](https://docs.gradle.org/current/userguide/composite_builds.html), you can **compose independent builds together**. This, together with a feature called [dependency substitution](https://docs.gradle.org/current/userguide/composite_builds.html#included_build_declaring_substitutions) will simplify the setup of a Gradle plugin standalone project.

The [official Gradle documentation](https://docs.gradle.org/current/userguide/composite_builds.html) defines composite build as:

> A composite build is simply a build that includes other builds.

So composite builds are **not just for gradle plugins**. You could use it whenever you have two or more independent Gradle builds that you want to link together. 

Let's update our sample project to use a composite build and see how we can benefit from it.
 
We will separate our project in **two separate** Gradle project:

- `plugin-build` A Gradle project that contains only the Gradle Plugin
- `root` The root project that contains the `example` module and will include the `plugin-build` build.

Your project structured will look like this now:

```
.
├── build.gradle.kts 
├── settings.gradle.kts // The root project
├── example
│   ├── src
│   └── build.gradle.kts
└── plugin-build        // The composite build 
    ├── build.gradle.kts
    ├── settings.gradle.kts
    └── plugin
        ├── build.gradle.kts
        └── src
```

Please note that `plugin-build` is a fully **independent build**. This means that you can:

- `cd` inside the **folder** and invoke any Gradle task for that build
- Open it in any **IDE** and build it independently 

You can also invoke tasks of `plugin-build` from the root build using:

```bash
./gradlew --project-dir plugin-build assemble
# OR
./gradlew -p plugin-build assemble
```

The root `./settings.gradle.kts` file will now use **includeBuild** to specify the included build:

```kotlin
include(":example")
includeBuild(":plugin-build")
```

With this configuration, Gradle will then try to **substitute** dependency of the root build with a project dependency on an included build.

By default, Gradle will inspect the `${project.group}:${project.name}` of every included build to attempt a dependency substitution. 

If you configured project `group` and `name` correctly, dependency substitution will just work out of the box. If you need more flexibility (or you want to be explicit), you can specify dependency substitution in your `settings.gradle.kts` file like:  

```kotlin
includeBuild("plugin-build") {
    dependencySubstitution {
        substitute(module("com.example:my-gradle-plugin")).with(project(":plugin"))
    }
}
```

You can now update your `./example/build.gradle` to look like this:

{% highlight kotlin linenos %}
buildscript {
  dependencies {
    classpath("com.example:my-gradle-plugin")
  }
}

apply(plugin = "com.my.gradle.plugin")
{% endhighlight %}

Please note that you **don't specify** the version anymore, and you don't need to depend on `mavenLocal()`.

Now calling: 

```bash
./gradlew example:printExample
```

will work correctly with the latest change from your `plugin-build` project.

You don't need to `publishToMavenLocal &&...` anymore 🎉.

## Pros

I personally suggest composite builds as they come with a lot of benefits:

- They force you to **isolate your builds** into separate chunks that could be built independently.

- They allow you to **get rid** of `publishToMavenLocal` as well as `-SNAPSHOT` versions.

- You don't need **two separate Gradle invocations** when developing and testing your plugin. 

## Cons

Composite builds have [several limitations](https://docs.gradle.org/current/userguide/composite_builds.html#included_build_substitution_limitations), that you want to consider before switching. The most annoying drawbacks I experienced so far are:
  
- Independent build means potentially **more than one** `buildSrc` folder. This could end up in duplicated code between the two builds, specifically if you use it for managing your dependencies. Moreover, IDEs are not working well when they find more than one `buildSrc` folder.

- Calling tasks in the included build. Using the `-p` or `--project-dir` flag works, but it gets annoying in the long run.

- Calling tasks that should **propagate to all the modules**, also in the included build. As of today if you call `./gradlew ktlintFormat` from the root build, it will not invoke the `ktlintFormat` in the included build. My solution here is to create a `preMerge` task on the root build that invokes all the verification tasks on the included build ([source code is here](https://github.com/cortinico/kotlin-gradle-plugin-template/blob/master/build.gradle.kts#L68-L74)):

```kotlin
tasks.register("preMerge") {
    description = "Runs all the tests/verification on all the builds."
    dependsOn(":example:check")

    // Here specify dependencies on tasks from the included build 
    dependsOn(gradle.includedBuild("plugin-build").task(":plugin:check"))
    dependsOn(gradle.includedBuild("plugin-build").task(":plugin:validatePlugins"))
}
``` 

## Conclusions

I've updated all my Gradle plugin projects to use included builds and so far I enjoyed the experience.

nit: You can also check [yelp/swagger-gradle-codegen/](https://github.com/yelp/swagger-gradle-codegen/) that migrated to a composite build ([#97](https://github.com/Yelp/swagger-gradle-codegen/pull/97)). Kudos to [@martinbonnin](https://twitter.com/martinbonnin) for doing it.
{: .notice--info}

You can find all the examples from this blog-post on this **template** [cortinico/kotlin-gradle-plugin-template](https://github.com/cortinico/kotlin-gradle-plugin-template) on Github. It should get you started with composite build in a matter of seconds 🚀.

Have you liked this blog-post or do you have some feedback/experience to share?
Feel free to leave a comment below or find me as [@cortinico on Twitter <i class="fab fa-twitter"></i>](https://twitter.com/cortinico).

Happy composing 🎼

Thank you very much to Nelson Osacky ([@nellyspageli](https://twitter.com/nellyspageli)) and Martin Bonnin ([@martinbonnin](https://twitter.com/martinbonnin)) for proofreading this blog-post.

## References

* [Gradle Composite Builds](https://docs.gradle.org/current/userguide/composite_builds.html)
* [Developing Custom Gradle Plugins](https://docs.gradle.org/current/userguide/custom_plugins.html)
* [Precompiled Script Plugins](https://docs.gradle.org/current/userguide/custom_plugins.html#sec:precompiled_plugins)
* [Gradle Kotlin DSL](https://docs.gradle.org/current/userguide/kotlin_dsl.html)