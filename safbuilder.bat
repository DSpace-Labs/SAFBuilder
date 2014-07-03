@echo off
mvn --quiet exec:java -Dexec.mainClass="safbuilder.BatchProcess" -Dexec.args="%1 %2"
