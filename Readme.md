# Pablo

[![Clojars Project](https://img.shields.io/clojars/v/teknql/pablo.svg)](https://clojars.org/teknql/pablo)
[![cljdoc badge](https://cljdoc.org/badge/teknql/pablo)](https://cljdoc.org/d/teknql/pablo/CURRENT)

## About

Pablo is a thin layer on top of `tools.deps` to bring build configuration to
`deps.edn` files. It uses a tiny configuration map to manage builds and
publishing of artifacts. In addition to simplifying JAR creation and publishing,
it also makes it easy to manage multiple artifacts in a single repository.

It builds heavily on top of [EwenG/badigeon](https://github.com/EwenG/badigeon)
and takes monorepo inspiration from [jacobobryant/trident](https://github.com/jacobobryant/trident).

## Features, Status, and Roadmap

Pre-Alpha - Currently missing major features.

- [x] Building (skinny) JARs
- [x] Publishing JARs to Clojars
- [x] Monorepo support
- [X] REPL based API
- [ ] CLI API
- [ ] Uberjar target
- [ ] Native image target


## Configuration

Pablo is configured using a special `:pablo/config` entry in your `deps.edn`
file.

### Normal Configuration

```clj
{:pablo/config
 {:github      "teknql/pablo"
  :desc        "The secret weapon of build utilities"
  :target      :jar
  :group-id    teknql
  :artifact-id pablo}
```

### Monorepo configuration

```clj
{:pablo/config
 {:group-id wing
  :github   "teknql/wing"
  :projects
  {core      {:desc   "Extensions to the standard library"
              :target :jar
              :deps   [org.clojure/core.match
                       slingshot
                       metosin/malli
                       teknql/utopia]}
   integrant {:desc   "Utilities for working with Integrant"
              :target :jar
              :deps   [integrant]}
   repl      {:desc       "Utility functions for the REPL"
              :target     :jar
              :deps       [integrant/repl]
              :local-deps [integrant]}}}}
```
