$binPath = "C:\Users\dmytr\AppData\Local\Python\bin"
$current = [Environment]::GetEnvironmentVariable("PATH", "User")
$parts   = $current -split ";" | Where-Object { $_ -ne "" -and $_ -notmatch "pythoncore-3\.14-64" }

if ($parts -notcontains $binPath) {
    $parts = @($binPath) + $parts
}

$newPath = $parts -join ";"
[Environment]::SetEnvironmentVariable("PATH", $newPath, "User")
Write-Host "PATH обновлён. Теперь python3.14 должен работать в новом терминале."
Write-Host "Проверь: python3.14 --version"
