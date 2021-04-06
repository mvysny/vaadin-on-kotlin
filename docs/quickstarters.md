---
layout: default
title: Quick Starters
permalink: /quick-starters/
nav_order: 97
---

## Vaadin 14 Quick Starters

Vaadin 14 components are based on the Web Components standard; Vaadin 14-based apps
are also themable more easily than Vaadin 8-based apps.

Every VoK project tends to have several files (database migrations,
Gradle build script, themes, logger configuration etc) and our project will
be no exception. Therefore, it makes sense to
have an archetype app with all of those files already provided.

<div style="display: flex; flex-wrap: wrap">
<div onclick="location.href='https://github.com/mvysny/karibu10-helloworld-application';" class="box bg-grey-dk-000"><div class="caption">UI Base</div><div class="body">A project with one view and no db; perfect for your UI experiments</div></div>
<div onclick="location.href='https://github.com/mvysny/vok-helloworld-app';" class="box bg-blue-000"><div class="caption">VoK Project Base</div><div class="body">Skeletal app with support for SQL db; start building your app here</div></div>
<div onclick="location.href='https://github.com/mvysny/beverage-buddy-vok';" class="box bg-green-000"><div class="caption">Full Stack App</div><div class="body">The "Beverage Buddy" app backed by SQL db; demoes two tables</div></div>
<div onclick="location.href='https://github.com/mvysny/bookstore-vok';" class="box bg-yellow-000"><div class="caption">Full Stack App</div><div class="body">The "Bookstore" app backed by SQL db; also demoes security</div></div>
<div onclick="location.href='https://github.com/mvysny/vaadin-kotlin-pwa';" class="box bg-red-000"><div class="caption">Full Stack PWA</div><div class="body">Full-stack task list app backed by SQL db; for desktop and mobile browsers</div></div>
</div>

<style>
.box {
  border-radius: 4px;
  padding: 5px 10px;
  margin: 10px;
  width: 200px;
  transition: box-shadow 200ms;
  transition-timing-function: cubic-bezier(0.55, 0, 0.1, 1);
  color: rgba(0, 0, 0, 0.6);
  cursor: pointer;
}
.box:hover {
  box-shadow: 0 5px 10px rgba(0,0,0,.15);
}
.box .caption {
  font-size: 22px;
}
.box .body {
  padding-top: 8px;
  font-size: 14px;
}
</style>