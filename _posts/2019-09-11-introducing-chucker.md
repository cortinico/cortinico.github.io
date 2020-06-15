---
title: "Introducing Chucker"
categories: "Android"

excerpt: "Chucker is a simple library to inspect OkHTTP traffic directly on your Android device"

header:
    image: "/assets/images/posts/header-introducing-chucker.jpg"
    teaser: "/assets/images/posts/teaser-introducing-chucker.jpg"
    caption: "MoMa - San Francisco, California"
---

Nearly one year ago I stumbled upon this blogpost: [Top 10 Android Libraries Every Android Developer Should Know About](https://infinum.co/the-capsized-eight/top-10-android-libraries-every-android-developer-should-know-about). 

Initially, it looked like yet another top 10 android library list and I expected to know all the libraries already, but... **not this time!** That blogpost contained a surprise: [Chuck](https://github.com/jgilfelt/chuck), by [Jeff Gilfelt](https://github.com/jgilfelt).

Chuck is an **OkHTTP inspector** that allows investigating the ongoing HTTP(S) traffic showing all the details of **every request & response**. I generally love tools and libraries that can improve the Android Developer's daily life. The idea behind Chuck was great so I gave it a try!

The library worked as promised but the last commit on Github was **long ago**... 😕

Apparently the project needed some **refreshment**: for instance, the `README`file still suggested to use Gradle dependencies with the [`compile` configuration](https://github.com/jgilfelt/chuck/pull/79) (that was deprecated in 2017) rather than `implementation`.

The library appeared **unmaintained** on GitHub, with a lot of open unanswered issues and pull requests but no real activity on any branch.

# Forking Chuck

After playing a little with it, I liked the idea behind Chuck!

I generally use [Charles](https://www.charlesproxy.com/) to inspect the network traffic of my app with a proxy on my computer. Unfortunately, Charles can be sometimes annoying to configure. It requires you to set up a Proxy on the Android device and a SSL certificate on the device.

Chuck is a great to **streamline this process** and to easily investigate what the app is doing on the network. On the other hand, depending on unmaintained libraries is not ideal.

It was **time for a fork**! 🌱 

Looks like I was not alone with this idea as already two forks of Chuck were available out there:

*  [Chucky](https://github.com/olivierperez/chucky) created by [@olivierperez](https://github.com/olivierperez)
*  [Gander](https://github.com/Ashok-Varma/Gander) created by [@Ashok-Varma](https://github.com/Ashok-Varma)

Olivier directly reached out to me and asked me if I would love to join his project and I couldn't say no! We renamed it and [ChuckerTeam/Chucker](https://github.com/ChuckerTeam/chucker) was officially born 🐣.

# Chucker

After months of open source work, we're finally releasing **Chucker 3.x**, the next major release of Chucker. Let's see how to use it and what this new release will bring to developers.

![chucker example](/assets/images/posts/chucker-1.gif)

## How to use it

To use Chucker you just need to add those lines to your gradle build:

```groovy
debugImplementation "com.github.ChuckerTeam.Chucker:library:3.0.1"
releaseImplementation "com.github.ChuckerTeam.Chucker:library-no-op:3.0.1"
```

And add the interceptor offered by the library to your OkHTTP client:

```kotlin
val client = OkHttpClient.Builder()
                .addInterceptor(ChuckerInterceptor(context))
                .build()
```

**That's it!** 🎉 Chucker will now record all HTTP interactions made by your OkHttp client.

## What's new in 3.x

Chucker 3.x comes with a lot of **new features** and improvements that will hopefully improve your developer experience. Moreover, we updated the library to be aligned with the _state of the art_ of the Android ecosystem in 2019.

## Public API

We have converted the public API of Chucker to **100% Kotlin** 🎉 You can now use named parameters and default values to easily configure a `ChuckerInterceptor` like this:

```kotlin
val interceptor = ChuckerInterceptor(
    context = this,
    // The max body content length, after this responses will be truncated.
    maxContentLength = 250000L,
    // List of headers to obfuscate in the Chucker UI
    headersToRedact = listOf("Auth-Token")
)
```

Moreover, the public API of Chucker is now isolated inside the `com.chuckerteam.chucker.api` package. On classes inside this package, we are following strict **semantic versioning**. Expect a major version bump if a breaking change is going to be introduced in those classes. 

Classes inside the `.internal` package are part of the implementation internals and are not part of the API surface. Don't use them as they might be broken at any time. 

## Database

We've updated the **ORM** used by the library to store traffic information. Historically Chuck used to use [Cupboard](https://bitbucket.org/littlerobots/cupboard/src/default/), a great ORM for Android written by [Hugo Visser](https://twitter.com/botteaap).

Cupboard is also [not updated](https://bitbucket.org/hvisser/cupboard/commits/all) since 2016 and it was time to migrate to something more up to date. Chucker is now using [**Room**](https://developer.android.com/topic/libraries/architecture/room). If you don't know Room, it's one of the Android Architecture components from Google and it allows to abstract over the local SqLite database.

## UI

The UI of the library was **fully revamped**. Time to say goodbye to good old `RelativeLayout` and [replace them with `ConstraintLayout`](https://github.com/ChuckerTeam/chucker/pull/10). Moreover, we polished the color palette and updated the library logo to resemble Chuck's logo and palette (after all this is just a fork).

If you're worried about pulling in too many dependencies inside your final APK (Room, ConstraintLayout, etc.), you **don't have to worry**. Those dependencies are defined only inside the `library` artifact. Your release APK will be built using the `library-no-op` artifact that contains **no dependency at all**.

## New Features

Chucker 3.x is not only about updating external dependencies. We're also shipping some new features:

### Throwables

**Throwables** ☄️ You can now use Chucker to collect and display Throwables that are fired from your application. To inform Chucker that a `Throwable` was fired you need to call the `onError` method of the `ChuckerCollector` (you need to retain an instance of your collector):

```kotlin
try {
    // Do something risky
} catch (IOException exception) {
    chuckerCollector.onError("TAG", exception)
}
```

Throwables will also be shown in a push notification and are available under the Error tab in the Chucker UI.

![image loading sample](/assets/images/posts/chucker-2.png)

### Images

**Images** 🖼 contained inside the body of HTTP responses **will now be rendered**! A lot of HTTP traffic generated by our Android app is actually fetching of images. Now you can now get a preview of what every image looks like to help debugging potential problems.

![image loading sample](/assets/images/posts/chucker-3.png)

### Body Search

We added the support to **search** 🔎 inside the body of plain/text HTTP requests. This can be helpful if you're receiving a big JSON response and you're interested in checking a specific field.

![search sample](/assets/images/posts/chucker-4.png)

That's all! I finally want to say a big **thank you** ❤️ to [@alorma](https://github.com/alorma) [@Ashok-Varma](https://github.com/Ashok-Varma) [@koral--](https://github.com/koral--) [@olivierperez](https://github.com/olivierperez) [@OlliZi](https://github.com/OlliZi) [@PaulWoitaschek](https://github.com/PaulWoitaschek) [@psh](https://github.com/psh) [@redwarp](https://github.com/redwarp) [@uOOOO](https://github.com/uOOOO) for making Chucker 3.x possible! 

Please try Chucker and give us feedback, we're looking for more contributors!