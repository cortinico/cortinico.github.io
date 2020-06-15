---
title: "Jetifier Reverse Mode"
categories: "Android"

excerpt: "How to use AndroidX migrated libraries on not yet migrated Android projects"

header:
    image: "/assets/images/posts/header-jetifier-reverse.jpg"
    teaser: "/assets/images/posts/teaser-jetifier-reverse.jpg"
    caption: "Mitchell's Cove Beach - Santa Cruz, California"
---

If you're working with Android nowadays, you'll probably be familiar with **Android X**, the _major package renaming_ of the Android Support library announced at Google I/O 2018. It's time to migrate!

Although, you might be in the situation **where you can't yet migrate**. Either because you're working on a big project (that you're not fully responsible for) or just because you love procrastinating. One of the tools can come handy in this case is **Jetifier** in **reverse mode**.

# AndroidX

Google finally decided to clean up and re-organize the good old Support Library (launched in [March 2011](https://developer.android.com/topic/libraries/support-library/rev-archive#rev1)!). Historically, classes in the Support Library used to have packages like `android.support.v4.app.`. 

The `.v4` in the package meant that the code provided was supposed to be backward compatible for API level 4 and higher. Given that now we're at API level 28, having a `.v4` in the package name doesn't add much value (it mostly creates confusion).

All the packages in the Support Library got standardized under the `androidx.` package. So for examples `android.support.v4.app.Fragment` will be `androidx.fragment.app.Fragment`. The list with all the mapping is [pretty long](https://developer.android.com/jetpack/androidx/migrate#class_mappings). You need to take care that everything in your codebase is migrated to use the new packages.

Ideally, this process should be _smooth_ using the *Refactor -> Migrate to AndroidX* tool inside Android Studio to do all the job. Unfortunately is not always working, and you probably need to do some manual adjustments, especially on bigger projects.

The last thing you need to do is add this line to your `gradle.properties`

```groovy
android.useAndroidX=true
```

What about your **dependencies**? They need to be migrated as well.
Here is where Jetifier comes into play. 

# Jetifier

If you migrated to AndroidX and you're using a library that is not migrated yet, you will probably have some problems. Let's say that you're using a library that provides you a `LibraryFragment` that is a subclass of `android.support.v4.app.Fragment`. You will not be able to use it as your application is expecting a `androidx.fragment.app.Fragment` subclass.

[Jetifier](https://developer.android.com/studio/command-line/jetifier) is a tool provided by Google to help you solve exactly this kind of issue. Jetifiery will **convert the bytecode** of your dependencies (aka _jetify_) to use the AndroidX classes following the mapping.

The enable Jetifer you just need to add this line to your `gradle.properties`

```groovy
android.useJetifier=true
```

and the dependencies will be converted **automagically** during the build.

Alternatively, you can [download](https://dl.google.com/dl/android/studio/jetifier-zips/1.0.0-beta02/jetifier-standalone.zip) the standalone version to convert single artifacts directly from your command line.

Thanks to this, you'll be able to use either migrated or not migrated dependencies in your project.

What about if you're maintaining an Android Library?

# The library developer's point of view

If you're a library developer, you probably asked yourself: [Should I migrate my library to AndroidX?](https://www.reddit.com/r/androiddev/comments/9yd1ht/should_i_use_support_or_androidx_in_my_own_library/). The risk here is that if you migrate to AndroidX, users that haven't yet migrated can't use your library.

You basically have 3 options:

### Don't migrate.

The _laziest_ alternative is to don't migrate. Apps that are using your library and are not migrated, will keep on working as usual. Apps that are migrated instead will jetify your compiled code to make it compatible with their codebase.

### Maintain two variants of your library.

You can maintain two variants/flavors of your app. The `-androidx` variant will be the migrated instance and you should point out in the documentation which artifacts each user should use.

This is probably the _safest_ approach as users won't need Jetifier at all, avoiding all the [potential bugs](https://issuetracker.google.com/issues?q=componentid:460323%20status:open) of this tool. At the same time is probably the _most costly_ solution as you need to take care of two artifacts and you might end up in some code duplication.

### Migrate.

Do a major release of your library and announce that from version `X.` you will be supporting only AndroidX. Don't forget to point out that the in your Readme file (e.g. pinning the latest version of the library that is not migrated to AndroidX).

This is the approach I used for a library I maintain: [AppIntro](https://github.com/AppIntro/AppIntro). We decided that from `5.x` the library will support only AndroidX.

People that want to use the latest version of the library should either update to AndroidX or use Jetifier in **Reverse Mode**. 

# Reverse Mode

Jetifier in reverse mode can be used to _de-Jetify_ the bytecode of a library, converting the new AndroidX packages to the old one.

Unfortunately, the reverse mode is **not integrated** into the Android Gradle Plugin and there is no property to set to enable it. The only way to run it is to use the standalone version. 

Luckily the command line jetifier is not so hard to use:

```
$ ./bin/jetifier-standalone
ERROR: [Main] Missing required options: i, o
usage: Jetifier (standalone)
 -c,--config <arg>                      Input config path (otherwise
                                        default is used)
 -i,--input <arg>                       Input library path (jar, aar, zip)
 -l,--log <arg>                         Logging level. Values: error,
                                        warning (default), info, verbose
 -o,--output <arg>                      Output file path
 -r,--reversed                          Run reversed process
                                        (de-jetification)
 -rebuildTopOfTree,--rebuildTopOfTree   Rebuild the zip of maven
                                        distribution according to the
                                        generated pom file.If set, all
                                        libraries being rewritten are
                                        assumed to be part of Support
                                        Library. Not needed for
                                        jetification.
 -s,--strict                            Don't fallback in case rules are
                                        missing and throw errors instead
```

We need the `-r` option to de-jetifiy. Let's try to use it with the [AppIntro artifact](https://jitpack.io/com/github/AppIntro/AppIntro/v5.1.0/AppIntro-v5.1.0.aar):

```bash
./bin/jetifier-standalone -r -i AppIntro-v5.1.0.aar -o deJetified.aar
```

That's it! Your `.aar` is now de-jetified and can be integrated into your app.
To actually use it you also need the `.pom` file where all the library dependencies are listed. 

For AppIntro, for example, we have those dependencies in the [pom file](https://jitpack.io/com/github/paolorotolo/AppIntro/v5.1.0/AppIntro-v5.1.0.aar):

```xml
<dependencies>
    <dependency>
      <groupId>androidx.appcompat</groupId>
      <artifactId>appcompat</artifactId>
      <version>1.0.0</version>
      <scope>compile</scope>
    </dependency>
    <dependency>
      <groupId>androidx.annotation</groupId>
      <artifactId>annotation</artifactId>
      <version>1.0.0</version>
      <scope>compile</scope>
    </dependency>
    ...
</dependencies>
```

You can either update them manually or let Jetifier do the job. You can't pass the `.pom` directly to the command line tool, but you have to create a `.zip` file where you can put all the content of the maven repository: `.aar`, `.pom`, `-sources.jar`, etc.

Using this tool, you can probably encounter this warning:

WARNING: [ProGuardTypesMap] Conflict: [ProGuardType(value=androidx/preference/{any})] -> (ProGuardType(value=android/support/v14/preference/{any}), ProGuardType(value=android/support/v7/preference/{any}))
{: .notice--warning}

Here Jetifier complains because it's not able to reverse the mapping for `androidx.preference.` in the ProGuard file. The problem is due to both `android.support.v14.preference.` and `android.support.v7.preference.` being migrated to the same AndroidX package, so the mapping is ambiguous.

You can fix this warning by passing a custom mapping file to Jetifier with the `-c` option. You can use [this](https://gist.github.com/cortinico/c48bc02411b7fb45f383c9ac01b8b595) as a starting point and customize it as you wish. The warning is raised by those [lines](https://gist.github.com/cortinico/c48bc02411b7fb45f383c9ac01b8b595#file-default-generated-config-L4411). You can remove one of the two rules in the config file to remove the warning.

```json
  "proGuardMap": {
    "rules": {
      ...
      "android/support/v7/preference/{any}": [
        "androidx/preference/{any}"
      ],
      "android/support/v14/preference/{any}": [
        "androidx/preference/{any}"
      ],
      ...
```

Now you have your de-jefitifed artifact ready, that can be added as a dependency to your project. 

Happy (de)jetification! 🕺