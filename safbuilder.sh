#!/bin/sh
echo "Recompiling SAFBuilder, just a moment..."
mvn --quiet -DskipTests=true clean package

## arg1 = Path to content files directory
## arg2 = Metadata CSV filename
args="$1 $2 $3 $4 $5 $6"
argsTrim=$(echo $args | sed 's/ *$//')
mvn --quiet exec:java -Dexec.mainClass="safbuilder.BatchProcess" -Dexec.args="$argsTrim"
