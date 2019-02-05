::
:: Gaia Sky start script
::

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

IF EXIST %GSDIR%jre\bin\java (
    :: Use bundled java
    SET JAVA_CMD=%GSDIR%jre\bin\java.exe
    @ECHO Using bundled java: %JAVA_CMD%
) ELSE (
    :: Look for java
    FOR /f %%j IN ("java.exe") DO (
        SET JAVA_LOC=%%~dp$PATH:j
    )

    IF %JAVA_LOC%.==. (
        @ECHO Java installation not found! Exiting.
        @EXIT 1
    ) ELSE (
        SET JAVA_CMD=%JAVA_LOC%jre\bin\java.exe
        @ECHO Using system java: %JAVA_CMD%
    )
)
:: Run
%JAVA_CMD% %OPTS% -cp "%GSDIR%lib\*" gaia.cu9.ari.gaiaorbit.desktop.GaiaSkyDesktop
