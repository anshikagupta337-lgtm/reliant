@echo off
cd /d "%~dp0"
powershell -NoProfile -Command "$s = Read-Host 'GitHub token paste karo (screen par nahi dikhega), phir Enter' -AsSecureString; $env:GITHUB_TOKEN = [System.Net.NetworkCredential]::new('', $s).Password; mvn -q compile org.codehaus.mojo:exec-maven-plugin:3.5.0:java"
echo.
echo Khatam. Upar ka output dekho.
pause
