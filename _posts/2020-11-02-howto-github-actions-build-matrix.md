---
title: "How-to Github Actions: Build Matrix"
categories: "Android"

excerpt: "How to use Build Matrix to test your project against different version of a language, a library or an operating system"

header:
    image: "./assets/images/posts/howto-github-actions-build-matrix/header.jpg"
    teaser: "./assets/images/posts/howto-github-actions-build-matrix/teaser.jpg"
    caption: "Kubuswoningen - Rotterdam, The Netherlands"
---

My favorite feature of Github Action is **build matrix**.

A build matrix is a **set of keys and values** that allows you to **spawn several jobs** starting from a single job definition. The CI will use every key/value combination performing value substitution when running your job. This allows you to run a job to test **different versions** of a language, a library, or an operating system.

In this blog-post, you will discover how to create a build matrix for your workflow with two real-world examples.

{% capture notice-info %}
This blog-post is part of a blogpost series: _How-to Github Actions_. 
You can find the other posts of this series here:
* [Building your Android App (Introduction)](/blog/howto-github-actions-building-android)
* Build Matrix - This blogpost 
{% endcapture %}
<div class="notice--info">{{ notice-info | markdownify }}</div>

# Setup a Build Matrix

You can define a build matrix when defining your job in your workflow file:

{% raw %}
```yaml
jobs:
  build:
    strategy:
      matrix:
        os: [ubuntu-latest, macos-latest, windows-latest]
    runs-on: ${{ matrix.os }}
```
{% endraw %}

In this example, the build matrix has one variable `os` and three possible values (`ubuntu-latest` , `macos-latest`, and `windows-latest`). This will result in Github Actions running a total of **three separate jobs**, one for each value of the `os` variable.

With this configuration, you can run our workflow on all the operating systems supported by Github Actions workers. 

If you wish to also test several versions of our language (e.g. Python), you can add **another variable** to our build matrix: 

```yaml
jobs:
  build:
    strategy:
      matrix:
        os: [ubuntu-latest, macos-latest, windows-latest]
        python: [2.7, 3.6, 3.8]
```

{% raw %}
In this case, Github Actions will run a job for every combination, resulting in a total of **nine jobs** executed. The value of the `python` variable will be available inside the workflow definition as `${{ matrix.python }}`
{% endraw %}

By default, Github Actions will fail your workflow and will stop all the running jobs if **any** of the jobs in the matrix **fails**. This can be annoying as you probably want to see the outcome of **all** your jobs. The change this behavior you can use the `fail-fast` property:

```yaml
jobs:
  build:
    strategy:
      fail-fast: false
      matrix:
        ...
```

Let's see a real-world example of a build matrix in action with Detekt.

# Example: Detekt

If you don't know [detekt/detekt](https://github.com/detekt/detekt), is a static analyzer for Kotlin. 

Historically, the project used to run on a mixture of CIs: Travis CI for Linux/macOS builds and AppVeyor for Windows builds. Having two separate CI services was **inconvenient** as they have slightly different syntax for their build files. Moreover, it required more effort to maintain them in sync.

Early this year we decided to [migrate to Github Actions](https://github.com/detekt/detekt/pull/2512). A build matrix allowed us to migrate to a single CI that would run all our jobs. 

The resulting configuration looks like this (simplified for brevity):

{% raw %}
```yaml
jobs:
  gradle:
    strategy:
      fail-fast: false
      matrix:
        os: [ubuntu-latest, macos-latest, windows-latest]
        jdk: [8, 11, 14]
    runs-on: ${{ matrix.os }}
    env:
      JDK_VERSION:  ${{ matrix.jdk }}

  steps:
    - name: Checkout Repo
      uses: actions/checkout@v2
    ...
    - name: Setup Java
      uses: actions/setup-java@v1
      with:
        java-version: ${{ matrix.jdk }}
```
{% endraw %}

In this workflow, we define a build matrix that allows us to test across every operating system and **three different versions of java** (Java 8, 11 & 14). 

We use the value of the `jdk` variable in a couple of places:

* An assignment to an environment variable `JDK_VERSION`.
* Inside the [actions/setup-java](https://github.com/actions/setup-java) action to configure the Java version.

You can find the [actual workflow file here](https://github.com/detekt/detekt/blob/0e433b48399dfa91de25af72677383ef405c58d8/.github/workflows/pre-merge.yaml#L11-L21).

This setup helps us ensure that detekt runs correctly on the majority of our users.

# Shadow CI Jobs

A great use case for build matrix is the setup of **shadow CI jobs** 👻.

A shadow CI job is a job that tests your project against an **unreleased/unstable** version of a dependency of your project. This helps you spot integration problems and regressions **early on**.

Generally, you want to treat a failure in a shadow job like a **warning** and don't fail your whole workflow. This because you just want to get notified of a potential failure in the future, once a dependency becomes stable. 

With such a setup, you could reach out to the library maintainer and notify them about unexpected problems or breaking changes.

To achieve this, you can use the `include` key together with `continue-on-error`:

{% raw %}
```yaml
jobs:
  build:
    strategy:
      matrix:
        python: [2.7, 3.6]
        experimental: [false]
        include:
          - python: 3.8
            experimental: true
  continue-on-error: ${{ matrix.experimental }}
```
{% endraw %}

With `include`, you can **add an entry** to the build matrix. In our example, the matrix would normally trigger two builds (`python:2.7, experimental:false` and `python:3.6, experimental:false`). In this case `include` will add the `python:3.8, experimental:true` entry to the build matrix.

With `continue-on-error`, you can specify if a failure in the job should trigger a **failure in the whole workflow**.

Thanks to this setup, you can add shadow jobs to the matrix with the `experimental` key set to `true`. Those jobs will run without invalidating the whole workflow if they happen to fail due to an unstable dependency.

Let's see a real-world example of a shadow CI jobs in action with AppIntro.

# Example: AppIntro

[AppIntro/AppIntro](https://github.com/AppIntro/AppIntro) it's a library to create intro carousels for Android Apps. 

In AppIntro we use a build matrix to **build a debug APK** of our library against:

* Unreleased versions of the Android Gradle Plugin (AGP)
* EAP versions of Kotlin

Our workflow file looks like this:

{% raw %}
```yaml
jobs:
  build-debug-apk:
    strategy:
      fail-fast: false
      matrix:
        agp: [""]
        kotlin: [""]
        experimental: [false]
        name: ["stable"]
        include:
          - agp: 4.2.+
            experimental: true
            name: AGP-4.2.+
          - kotlin: 1.4.20+
            experimental: true
            name: kotlin-EAP-1.4.20+

    continue-on-error: ${{ matrix.experimental }}
    name: Build Debug APK - ${{ matrix.name }} - Experimental ${{ matrix.experimental }}
    env:
      VERSION_AGP: ${{ matrix.agp }}
      VERSION_KOTLIN: ${{ matrix.kotlin }}
```
{% endraw %}

As mentioned before, we use `include` and `continue-on-error` to add two experimental entries to our build matrix. 

Here we also specify a `name` key to make our job **easier to recognize**:

{% raw %}
```yaml
name: Build Debug APK - ${{ matrix.name }} - Experimental ${{ matrix.experimental }}
```
{% endraw %}

The values of the build matrix keys are then passed as environment variables here:

{% raw %}
```yaml
env:
  VERSION_AGP: ${{ matrix.agp }}
  VERSION_KOTLIN: ${{ matrix.kotlin }}
```
{% endraw %}

Those environment variables are then accessed in the `build.gradle` file:

{% raw %}
```groovy
buildscript {
    ext.kotlin_version = "1.4.10"	
    
    ext {
        kotlin_version = System.getenv("VERSION_KOTLIN") ?: "1.4.10"
        agp_version = System.getenv("VERSION_AGP") ?: "4.1.0"
    }

    dependencies {
        classpath "com.android.tools.build:gradle:$agp_version"
        ...
    }
}
```
{% endraw %}

{% raw %}
For the **regular job**, the `${{ matrix.agp }}` key is empty (`""`). This causes the `System.getenv("VERSION_AGP")` to return `null` and the resulting version is the **stable** one (specified in the `build.gradle`).

For the shadow job instead, the `${{ matrix.agp }}` key is `4.2.+`. This causes the dependency string to be resolved to 

```
com.android.tools.build:gradle:4.2.+
```

Here we use Gradle [dynamic versions](https://docs.gradle.org/current/userguide/dynamic_versions.html), to specify a **version range**:  `4.2.+`. This allows us to test on the latest installment of AGP 4.2 without having to update the workflow file for every alpha/beta/RC release.
{% endraw %}

You can find the [actual workflow file here](https://github.com/AppIntro/AppIntro/blob/5f961d6acb9a3413914ab0f4d44353d410c10ca8/.github/workflows/pre-merge.yml#L68-L90).

A similar mechanism can be used to test your Android App against:

* An upcoming versions of Gradle
* A bump of `targetSdkVersions`
* A snapshot of a library that is not released yet

and much more.


# Conclusions

Github Actions' build matrixes are a great tool to help you build & test your project against several versions of a language, a library, or an operating system.

Shadow jobs take build matrixes a step further, allowing you to test against **unreleased versions** of such languages or libraries.

Make sure you don't miss the upcoming articles, you can find me as [@cortinico on Twitter <i class="fab fa-twitter"></i>](https://twitter.com/cortinico).

# References

* [Github Actions Build Matrix reference](https://docs.github.com/en/free-pro-team@latest/actions/reference/workflow-syntax-for-github-actions#jobsjob_idstrategymatrix)
* [Gradle Dynamic Versions](https://docs.gradle.org/current/userguide/dynamic_versions.html)

