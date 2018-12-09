---
title: "To migrate or to not migrate (to AndroidX), this is the question."
categories: "Android"

excerpt: "TODO"

header:
    image: "/assets/images/posts/header-migrate-to-androidx.jpg"
    teaser: "/assets/images/posts/teaser-migrate-to-androidx.jpg"
    caption: "Santa Cruz - California"
---

TODO Change image with the Bird

Is time to migrate to AndroidX! (Quote?) Not only your Android apps but also your **libraries**! In this blogpost I'll tell you my experience with the migration of AndroidX for a couple of libraries I'm maintaining, with both pros and cons of either migrating or not.

# Introduction

If you're an Android developer you've probably heard about **AndroidX**, the _refactoring_ (TODO) of the Support Library announced at the Google I/O this year. (If you haven't heard about it, you can catch up with this [online session](TODO)).

AndroidX comes with a lot of new features like (TODO). On the other hand this refactoring comes with some **costs**. 

Since all the packages have been update from `com.google.support` to `androidx.TODO`this means that the two libraries are incompatible. 

TODO Maybe quote from Apollo13?

If you try to just _bump_ your gradle depencecy your codebase will be broken. You need to **migrate** from the old support library to AndroidX.

Google developed a **migration tool** inside Android Studio with the intent of automatazing the migration. Unfortunately it doesn't always work 😕

You can find a lot of blogposts online telling experiences of Android developers migrating their app to AndroidX. I personally can recommend those:

* TODO

Unfortunately, if you're a library developers the story is a bit different and you might have some other problems to deal with. Let's see them.

# To not migrate

The laziest solution :) Just don't migrate! You don't really need to migrate your library to AndroidX. Your users can use Jetifier and use your library in their AndroidX projects. 

## Pro

* No effort required.
* Users that are not using AndroidX are unaffected.
* Users that are using AndroidX, can use Jetifier to use your library (and they probably have it already enabled).

## Cons

* You can't use the newest AndroidX features
* TODO

# To migrate

Migrating a library should be as easy as migrating an app. Just start with the **Migrate to AndroidX** tool and fix 

## Jetifier (reverse mode)

# Conclusions

# References

