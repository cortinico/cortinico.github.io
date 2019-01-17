---
title: "Jetifier Reverse Mode"
categories: "Android"

excerpt: "How to use AndroidX migrated libraries on not yet migrated Android projects"

header:
    image: "/assets/images/posts/header-jetifier-reverse.jpg"
    teaser: "/assets/images/posts/teaser-jetifier-reverse.jpg"
    caption: "Santa Cruz - California"
---

If you're working with Android nowadays, you'll probably be familiar with **Android X**, the _major package renaming_ of the Android Support library announced at Google I/O 2018. It's time to migrate!

Although, if you're working on a big project (or if you love procasticating) you probably haven't migrated yet. One of the tool can come handy in this case is **Jetifier** in **reverse mode**.

# AndroidX

Google finally decided to cleanup and re-organize the good old Support Library (launched in [March 2011](https://developer.android.com/topic/libraries/support-library/rev-archive#rev1)!). Historically, classes in the Support Library used to have packages like `android.support.v4.app.`. 

The `.v4` in the package meant that the code provided was supposed to be backward compatible for API level 4 and higher. Given that now we're at API level 28, having a `.v4` in the package name doesn't add much value (it moslty creates confusion).

All the packages in the Support Library got standardized under the `androidx.` package. So for examples `android.support.v4.app.Fragment` will be `androidx.fragment.app.Fragment`. The list with all the mapping is [pretty long](https://developer.android.com/jetpack/androidx/migrate#class_mappings). You need to take care that everything in your codebase is migrated to use the new packages.

Ideally, this process should be _smooth_ using the *Refactor -> Migrate to AndroidX* tool inside Android Studio to do all the job. Unfortunately is not always working, and you probably need to do some manual adjustments, especially on bigger projects.

The last thing you need to do is add this line to your `gradle.properties`

```groovy
android.useAndroidX=true
```

What about your **dependencies**? They need to be migrated as well.
Here is where Jetifier comes in play. 

# Jetifier

If you migrated to AndroidX and you're using a library that is not migrated yet, your will probably have some problems. Let's say that you're using a library that provides you a `LibraryFragment` that is a subclass of `android.support.v4.app.Fragment`. You will not be able to use it as your application is expecting a `androidx.fragment.app.Fragment` subclass.

Jetifier will help you solve this issue by converting (_jetifying_) the bytecode of your dependencies to use the AndroidX classes following the mapping.

The enabled Jetifer you just need to add this line to your `gradle.properties`

```groovy
android.useJetifier=true
```

Or you can [download](https://dl.google.com/dl/android/studio/jetifier-zips/1.0.0-beta02/jetifier-standalone.zip) the standalone version to convert single artifacts.

Thanks to this you'll be able to use either migrated or not migrated dependencies in your project.

What about Android libraries? 

# The library developer point of view

If you're a library developer, you probably asked yourself: [Should I migrate my library to AndroidX?](https://www.reddit.com/r/androiddev/comments/9yd1ht/should_i_use_support_or_androidx_in_my_own_library/). The risk here is that if you migrate to AndroidX, users that haven't yet migrated can't use your library.

You basically have 3 options:

### Don't migrate.

The _laziest_ alternative is to don't migrate. Apps that are using your library and are not migrated, will keep on working as usual. Apps that are migrate insted will jetify your compiled code to make it compatible with their codebase.

### Maintain two variants of your library.

You can maintain two versions of your app. The `-androidx` version will be the migrated version and you should point out in the documentation which artifacts to use.

This is probably the _safest_ approach as users won't need Jetifier at all, avoiding all the [potential bugs](https://issuetracker.google.com/issues?q=componentid:460323%20status:open) of this tool. At the same time is probably the _most costly_ solution as you need to take care of two artifacts and you might end up in a lot of code duplication.

### Migrate.

Do a major release of your library and announce that from version `X.` you will be supporting only AndroidX. Don't forget to point out that the latest version of the library that is not migrated to AndroidX in the Readme file.

This is the approach I used for a library I'm maintaing: [AppIntro](https://github.com/paolorotolo/AppIntro). We decided that from `5.x` the library will support only AndroidX.

People that wants to use the latest version of the library should either update to AndroidX or use Jetifier in **Reverse Mode**. 

# Reverse Mode

Jetifier in reverse mode will _de-Jetify_ the bytecode of a library. Converting the new AndroidX packages to the old one.

Unfortunately the Reverse mode is not integrated into the AGP and there is no gradle property to set to enable it. The only way to run it is to use the standalone version. 

Luckily the command line jetifier is really easy to use:

```bash
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

Let's try to use it with the [AppIntro artifact](https://jitpack.io/com/github/paolorotolo/AppIntro/v5.1.0/AppIntro-v5.1.0.aar):

```bash
./jetifier-standalone -r -i AppIntro-v5.1.0.aar -o test.aar
```


