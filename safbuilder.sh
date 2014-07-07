#!/bin/sh
if [ ! -d "target" ]; then
  echo "Recompiling SAFBuilder, just a moment..."
  mvn --quiet -DskipTests=true clean package
fi

## arg1 = Path to content files directory
## arg2 = Metadata CSV filename
mvn --quiet exec:java -Dexec.mainClass="safbuilder.BatchProcess" -Dexec.args="$1 $2"
