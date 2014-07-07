@echo off
mvn --quiet -DskipTests=true clean package
mvn --quiet exec:java -Dexec.mainClass="safbuilder.BatchProcess" -Dexec.args="%1 %2"
