@echo off
call mvn --quiet -DskipTests=true clean package
call mvn --quiet exec:java -Dexec.mainClass="safbuilder.BatchProcess" -Dexec.args="%~1 %~2 %~3 %~4"
