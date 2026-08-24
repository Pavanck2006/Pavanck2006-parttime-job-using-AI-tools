@echo off
echo =====================================================================
echo  Starting PartTime Job Platform (Standalone Demo / In-Memory Mode)
echo  Tagline: "Find Work. Earn More. Work Safely."
echo =====================================================================
echo.
echo Starting application on http://localhost:8080 ...
echo.

mvn spring-boot:run -Dspring-boot.run.profiles=h2

pause
