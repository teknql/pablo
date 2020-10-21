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

## Usage Examples

To see an example project that uses pablo, check out [teknql/wing](https://github.com/teknql/wing),
which uses pablo to publish several smaller libraries that are part of a single repository.

```clj
(require '[pablo.core :as pablo])
(require '[pablo.utils])

(pablo.utils/with-cwd "/path/to/your/project-root"
  (let [opts {:credentials {:username "YOUR_CLOJARS_USERNAME"
                            :password "YOUR_CLOJARS_DEPLOY_TOKEN"}
              :version     "0.1.0"}]
    (pablo/extract! opts)   ;; ONLY for mono repos, extracts out sub projects to project-root/target.
    (pablo/jar! opts)       ;; Make JAR file(s), will be in project-root/target
    (pablo/install! opts)   ;; Install made jar file(s) to your local ~/.m2 repo
    (pablo/publish! opts))) ;; Publish the jar file(s) to your clojars
```
