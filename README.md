[![License](https://img.shields.io/badge/license-Eclipse%20Public%20License-blue.svg?style=for-the-badge)](https://raw.githubusercontent.com/metabase/macaw/master/LICENSE)
[![GitHub last commit](https://img.shields.io/github/last-commit/metabase/second-date?style=for-the-badge)](https://github.com/metabase/macaw/commits/)

[![Clojars Project](https://clojars.org/metabase/macaw/latest-version.svg)](https://clojars.org/metabase/macaw)

![Macaw logo](./assets/logo.png)

# Macaw

Macaw is a limited Clojure wrapper for
[JSqlParser](https://github.com/JSQLParser/JSqlParser). Similar to its parrot
namesake, it's intelligent, can be taught to speak SQL, and has many colors
(supports many dialects).


## Building

To build a local JAR, use

```
./bin/build-jar
```

This will create a JAR in the `target` directory.

## Working with the Java files

To compile the Java files, use

```
./bin/java-compile
```

If you're working on Macaw and make changes to a Java file, you must:

1. Recompile
2. Restart your Clojure REPL

for the changes to take effect.
