@echo off
echo ================================
echo Building ShortcutBuddy JAR...
echo ================================
call mvn clean package
if %errorlevel% neq 0 (
    echo ERROR: 'mvn clean package' failed! Build aborted.
    goto End
)

echo.
echo ===================================
echo Copying Dependencies...
echo ===================================
call mvn dependency:copy-dependencies -Dmdep.includeScope=runtime -Dmdep.excludeScope=test -DoutputDirectory=target/libs
if %errorlevel% neq 0 (
    echo ERROR: 'mvn dependency:copy-dependencies' failed! Check output.
    goto End
)

echo.
echo ================================
echo Build Process Completed!
echo ================================
echo Application JAR is in 'target/'
echo Dependencies are in 'target/libs/'
echo Remember to package native libraries (.dll) and create the final distribution folder/zip.

:End
echo.
pause