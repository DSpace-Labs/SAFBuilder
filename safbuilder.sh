#!/bin/sh
mvn --quiet exec:java -Dexec.mainClass="safbuilder.BatchProcess" -Dexec.args="$1 $2"
