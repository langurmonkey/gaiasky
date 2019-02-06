::
::
:: Gaia Sky start script
::

@ECHO OFF
SETLOCAL ENABLEEXTENSIONS

:: Gaia Sky location
SET GSDIR=%~dp0

:: Memory
SET OPTS=-Xms2g -Xmx4g
:: Garbage Collector G1
SET OPTS=%OPTS% -XX:+UseG1GC
:: Assets location
SET OPTS=%OPTS% -Dassets.location=%GSDIR%
:: SimpleLogger defaults
SET OPTS=%OPTS% -Dorg.slf4j.simpleLogger.defaultLogLevel=warn -Dorg.slf4j.simpleLogger.showThreadName=false

IF EXIST %GSDIR%jre\bin\java.exe (
    :: Use bundled java
    SET JAVA_CMD=%GSDIR%jre\bin\java.exe
) ELSE (
    :: Look for java
    IF "%JAVA_HOME%"=="" (
        ECHO Java installation not found! Exiting
        GOTO :END
    ) ELSE (
        SET JAVA_CMD=%JAVA_HOME%\bin\java.exe
    )
)
@ECHO ON
:: Run
"%JAVA_CMD%" %OPTS% -cp "%GSDIR%lib\*" gaia.cu9.ari.gaiaorbit.desktop.GaiaSkyDesktop
:END
