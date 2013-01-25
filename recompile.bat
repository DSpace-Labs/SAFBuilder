:: A recompile script for Windows, that should simplify what is required by the end-user to type. 
:: Its making assumptions that you have the JDK installed, it finds it, and calls JAVAC

@echo off
:: Set JAVA_HOME
for /d %%i in ("C:\Program Files\Java\jdk*") do set JAVA_HOME=%%i

set JAVA_HOME

:: Add all .jar files to the classpath.
setLocal EnableDelayedExpansion
set CLASSPATH="
for /R . %%a in (*.jar) do (
  set CLASSPATH=!CLASSPATH!;%%a
)
set CLASSPATH=!CLASSPATH!"
::echo !CLASSPATH!


"%JAVA_HOME%\bin\javac.exe" -classpath !CLASSPATH! src/edu/osu/kb/batch/*.java -d classes

echo SAFBuilder has recompiled...