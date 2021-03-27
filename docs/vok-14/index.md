---
layout: default
title: Vok-14
nav_order: 1
has_children: true
---

# Vaadin-on-Kotlin Guides for Vaadin 14

Vaadin 14 is based on Web Components. The programming model
tends to differ a bit from Vaadin 8, and that's why there is a separate set of guides
for Vaadin 8 and for Vaadin 14. Don't worry though - Vaadin 14 still uses the
component-oriented approach we love from Vaadin 8.

## Getting Started Guides

When you are not familiar with Vaadin-on-Kotlin (or VoK for short), start here:

- The [Getting Started](/vok-14/gettingstarted.html) guide

## Building the UIs

- [Creating UIs](creating_ui-v10.md) explains how to compose your UI out of visual components such as Buttons and TextFields
- [Creating Forms](forms-v10.md) walks you through the forms - how to create UI for them and how to validate the data.
- [Navigating](https://vaadin.com/docs/v10/flow/routing/tutorial-routing-annotation.html)
  allows you to create multiple pages and explains how to navigate between them.
- [Using Grids](grids-v10.md) shows best practices on how to display a lazy-loaded tabular list of data.
- [DSLs: Explained](dsl_explained-v10.md) explains how exactly VoK takes advantage of the DSL Kotlin language feature.

## Database Access Guides

Continue reading here to understand how exactly VoK accesses the database.

- The [Accessing Databases Guide](databases-v10.md)
- The [Accessing NoSQL or REST data sources](nosql_rest_datasources.md) for displaying data
  from a NoSQL database, or a REST service endpoint.
- [Writing services](services.md) to place your business logic in

## Security

Security is an important part of any web-based application. Please check the following resources for more information:

- The [vok-security](https://github.com/mvysny/vaadin-on-kotlin/blob/master/vok-security/README.md) module description.

## Translating Your App

You can target broader audience if you offer your app in multiple languages.

- The [Translating Your App](i18n.md) Guide.

## Testing

- The [Browserless Testing Guide](https://github.com/mvysny/karibu-testing)

## Contributing

- [Contributing To Vaadin On Kotlin](contributing.md)
