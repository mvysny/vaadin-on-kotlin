[Index](index.html) | [Getting Started](gettingstarted.html)

# Getting started with Vaadin-on-Kotlin

This guide covers getting up and running with Vaadin On Kotlin (VOK).

After reading this guide, you will know:

* How to create a new VOK application, and connect your application to a database.
* The general layout of a VOK application.
* How to quickly generate the starting pieces of a VOK application.

## 1 Guide Assumptions

This guide is designed for beginners who want to get started with a VOK application from scratch.
It does not assume that you have any prior experience with Vaadin. However, to get the most out of it,
you need to have some prerequisites installed:
   
* Java 8 JDK or newer.

VOK is an opinonated web application framework which employs Vaadin framework running on the Kotlin programming language. If you have no 
prior experience with Kotlin nor Vaadin, you will find a very steep learning curve diving straight 
into VOK. There are several curated lists of online resources for learning Kotlin:
   
* [Official Kotlin Programming Language website](https://kotlinlang.org/)
* [Kotlin Koans](https://kotlinlang.org/docs/tutorials/koans.html)

To learn Vaadin:

* [Official Vaadin website](https://www.vaadin.com)
* [Vaadin Documentation](https://vaadin.com/docs) - we recommend to download and read the Vaadin Book PDF.

## 2 What is Vaadin-on-Kotlin?
VOK is a glue between Vaadin, Kotlin and other frameworks which allows you to write web apps smoothly. 
It is designed to make programming web applications easier by making assumptions about what 
every developer needs to get started. It allows you to write less code while accomplishing 
more than many other languages and frameworks.

VOK is opinionated software. It makes the assumption that there is a "best" way to do things,
and it's designed to encourage that way - and in some cases to discourage alternatives.

The VOK philosophy includes two major guiding principles:

* Simplicity - things are kept as simple as possible, and libraries are used only when absolutely necessary. Complex patterns such as Dependency Injection
  are deliberately left out.
* Components as basic building blocks - Vaadin is a single-page web component framework as opposed to
  the traditional multiple page frameworks. As such, it resembles the traditional fat client
  Swing/JavaFX programming and is closer to GUI software development than traditional web development with HTML and JavaScript.
  VOK promotes code/UI reuse by means of reusing components (your components will range from basic ones
  to a complex containers, even forms) instead of creating page templates.


## 3 Creating a New VOK Project
The best way to read this guide is to follow it step by step. All steps are essential to run this example application and no additional code or steps are needed.

By following along with this guide, you'll create a VOK project called blog, a (very) simple weblog.
