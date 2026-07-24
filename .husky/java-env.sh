#!/usr/bin/env sh

# GUI Git clients often don't inherit the terminal's JAVA_HOME or PATH.
if [ -z "${JAVA_HOME:-}" ]; then
  for java_home in \
    /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
    /usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
  do
    if [ -x "$java_home/bin/java" ]; then
      export JAVA_HOME="$java_home"
      break
    fi
  done
fi

if [ -n "${JAVA_HOME:-}" ]; then
  export PATH="$JAVA_HOME/bin:$PATH"
fi
