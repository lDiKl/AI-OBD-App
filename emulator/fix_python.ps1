# Создаём python.cmd в папке Scripts (уже есть в PATH)
$scriptsDir = "C:\Users\dmytr\AppData\Local\Python\pythoncore-3.14-64\Scripts"
$pythonExe  = "C:\Users\dmytr\AppData\Local\Python\pythoncore-3.14-64\python.exe"
$cmdFile    = Join-Path $scriptsDir "python.cmd"

$content = "@echo off`r`n`"$pythonExe`" %*"
Set-Content -Path $cmdFile -Value $content -Encoding ASCII

Write-Host "Создан: $cmdFile"
Write-Host "Теперь команда 'python' должна работать в новом терминале"
