@echo off
:: Add all .jar files to the classpath.
setLocal EnableDelayedExpansion
set CLASSPATH="classes
for /R . %%a in (*.jar) do (
  set CLASSPATH=!CLASSPATH!;%%a
)
set CLASSPATH=!CLASSPATH!"
:: echo !CLASSPATH!


java -cp %CLASSPATH% edu.osu.kb.batch.BatchProcess %1 %2
