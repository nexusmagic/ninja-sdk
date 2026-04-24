#!/bin/sh

APP_NAME="Gradle"
APP_BASE_NAME=${0##*/}
DEFAULT_JVM_OPTS="-Xmx64m -Xms64m"
MAX_FD=maximum

warn() { echo "$*"; }
die() { echo; echo "$*"; echo; exit 1; }

cygwin=false
msys=false
darwin=false
nonstop=false
case "$(uname)" in
  CYGWIN*)  cygwin=true ;;
  Darwin*)  darwin=true ;;
  MSYS*|MINGW*) msys=true ;;
  NONSTOP*) nonstop=true ;;
esac

app_path=$0
while [ -h "$app_path" ]; do
  ls=$(ls -ld "$app_path")
  link=${ls#*' -> '}
  case $link in
    /*) app_path=$link ;;
    *)  app_path=$(dirname "$app_path")/$link ;;
  esac
done
APP_HOME=$(cd "$(dirname "$app_path")" && pwd -P) || exit

CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

if [ -n "$JAVA_HOME" ]; then
  JAVA_CMD="$JAVA_HOME/bin/java"
  [ -x "$JAVA_CMD" ] || die "JAVA_HOME apunta a un directorio inválido: $JAVA_HOME"
else
  JAVA_CMD=java
  command -v java >/dev/null 2>&1 || die "JAVA_HOME no está configurado y no se encontró 'java' en PATH."
fi

if ! "$cygwin" && ! "$darwin" && ! "$nonstop"; then
  case $MAX_FD in
    max*) MAX_FD=$(ulimit -H -n) || warn "No se pudo consultar el límite de descriptores" ;;
  esac
  case $MAX_FD in
    ''|soft) ;;
    *) ulimit -n "$MAX_FD" || warn "No se pudo establecer el límite de descriptores a $MAX_FD" ;;
  esac
fi

exec "$JAVA_CMD" $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS \
  -classpath "$CLASSPATH" \
  org.gradle.wrapper.GradleWrapperMain "$@"
