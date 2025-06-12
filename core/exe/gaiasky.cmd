::
::
:: Gaia Sky start script
::

@ECHO OFF
SETLOCAL ENABLEEXTENSIONS

:: Gaia Sky location
SET GSDIR=%~dp0

cd %GSDIR%

:: Memory
SET OPTS=-Xms2g -Xmx6g
:: ZGC
SET OPTS=%OPTS% -XX:+UseZGC -XX:+ZGenerational
:: Assets location
SET OPTS=%OPTS% -Dassets.location=.
:: SimpleLogger defaults
SET OPTS=%OPTS% -Dorg.slf4j.simpleLogger.defaultLogLevel=warn -Dorg.slf4j.simpleLogger.showThreadName=false
:: Unsafe access
SET OPTS=%OPTS% --sun-misc-unsafe-memory-access=allow

IF EXIST ".\jre\bin\java.exe" (
    :: Use bundled java
    SET JAVA_CMD=.\jre\bin\java.exe
) ELSE (
    :: Look for java
    IF "%JAVA_HOME%"=="" (
        ECHO Java installation not found! Exiting
        GOTO :END
    ) ELSE (
        SET JAVA_CMD="%JAVA_HOME%\bin\java.exe"
    )
)
@ECHO ON
:: Run
%JAVA_CMD% %OPTS% -cp .\lib\* gaiasky.desktop.GaiaSkyDesktop %*
::END
