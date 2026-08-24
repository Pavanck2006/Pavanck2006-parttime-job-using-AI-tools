@echo off
set "JAVA_HOME=C:\Program Files\Java\jdk-25.0.2"
set "PATH=C:\Users\pavan\tools\maven\bin;C:\Program Files\Java\jdk-25.0.2\bin;%PATH%"
if "%~1"=="" (
    %SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe -NoProfile -ExecutionPolicy Bypass
) else (
    %SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe -NoProfile -ExecutionPolicy Bypass %*
)
