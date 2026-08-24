$ErrorActionPreference = 'Stop'
Write-Host "=======================================================" -ForegroundColor Cyan
Write-Host " PartTime Job Platform - Launching Server" -ForegroundColor Green
Write-Host " Tagline: 'Find Work. Earn More. Work Safely.'" -ForegroundColor Yellow
Write-Host "=======================================================" -ForegroundColor Cyan

$env:JAVA_HOME = "C:\Program Files\Java\jdk-25.0.2"
$toolsDir = "C:\Users\pavan\tools"
$mavenDir = "$toolsDir\maven"
$mvnBin = "$mavenDir\bin\mvn.cmd"

$env:PATH = "$mavenDir\bin;$($env:JAVA_HOME)\bin;$env:PATH"

Write-Host "Starting Spring Boot Application on http://localhost:8080 ..." -ForegroundColor Green
& $mvnBin spring-boot:run
