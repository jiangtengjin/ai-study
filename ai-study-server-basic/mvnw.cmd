@REM Maven Wrapper script for Windows
@REM Forces Java 21 from the system javapath
@setlocal

@set JAVA_CMD=C:\Program Files\Common Files\Oracle\Java\javapath\java.exe

@if not exist "%JAVA_CMD%" (
    echo ERROR: Java 21 not found at %JAVA_CMD%
    exit /b 1
)

"%JAVA_CMD%" -version 2>&1 | findstr /I "21" >nul
@if errorlevel 1 (
    echo WARNING: Java at %JAVA_CMD% may not be version 21
)

@REM Download maven-wrapper.jar if not exists
@if not exist "%~dp0\.mvn\wrapper\maven-wrapper.jar" (
    echo Downloading Maven Wrapper...
    mkdir "%~dp0\.mvn\wrapper" 2>nul
    powershell -Command "Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar' -OutFile '%~dp0\.mvn\wrapper\maven-wrapper.jar'"
)

@REM Run Maven
"%JAVA_CMD%" ^
    -Dmaven.multiModuleProjectDirectory="%~dp0" ^
    -classpath "%~dp0\.mvn\wrapper\maven-wrapper.jar" ^
    "-Dmaven.home=E:\maven\apache-maven-3.8.3" ^
    org.apache.maven.wrapper.MavenWrapperMain %*

@endlocal
