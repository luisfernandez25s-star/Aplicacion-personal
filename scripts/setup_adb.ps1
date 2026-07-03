# PowerShell script for Windows

$toolsUrl = "https://dl.google.com/android/repository/platform-tools-latest-windows.zip"
$destDir = "$PSScriptRoot\platform-tools"
$zipFile = "$PSScriptRoot\platform-tools.zip"

if (-not (Test-Path "$destDir\adb.exe")) {
    Write-Host "Downloading Android Platform Tools..."
    Invoke-WebRequest -Uri $toolsUrl -OutFile $zipFile
    Write-Host "Extracting..."
    Expand-Archive -Path $zipFile -DestinationPath $PSScriptRoot -Force
    Remove-Item $zipFile
}

$env:PATH += ";$destDir"

Write-Host "--- ADB Status ---"
& "$destDir\adb.exe" devices

Write-Host "--- Connecting to device via TCP/IP ---"
$ip = Read-Host "Enter device IP address (e.g. 192.168.1.10)"
& "$destDir\adb.exe" tcpip 5555
& "$destDir\adb.exe" connect "$ip:5555"

Write-Host "--- Pair device (if needed) ---"
$choice = Read-Host "Do you want to pair? (y/n)"
if ($choice -eq 'y') {
    $pairCode = Read-Host "Enter pairing code"
    $pairIp = Read-Host "Enter pairing address (IP:PORT)"
    & "$destDir\adb.exe" pair $pairIp $pairCode
}

Write-Host "--- Logs ---"
& "$destDir\adb.exe" logcat *:S SensorService:D WearDataManager:D MongoRepository:D
