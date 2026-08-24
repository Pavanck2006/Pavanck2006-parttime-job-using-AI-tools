Write-Host "=====================================================================" -ForegroundColor Cyan
Write-Host " Starting PartTime Job Platform" -ForegroundColor Green
Write-Host " Tagline: 'Find Work. Earn More. Work Safely.'" -ForegroundColor Yellow
Write-Host "=====================================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Starting application on http://localhost:8080 ..." -ForegroundColor White
Write-Host ""

mvn spring-boot:run -Dspring-boot.run.profiles=h2
