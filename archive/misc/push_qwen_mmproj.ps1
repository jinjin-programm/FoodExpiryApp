param(
    [string]$MmprojPath
)
$adb = "C:\Users\jinjin\AppData\Local\Android\Sdk\platform-tools\adb.exe"

Write-Host "Pushing to tmp..."
& $adb push $MmprojPath "/data/local/tmp/mmproj.gguf"

Write-Host "Copying to app folder..."
& $adb shell "run-as com.example.foodexpiryapp cp /data/local/tmp/mmproj.gguf files/llm/mmproj.gguf"

Write-Host "Verifying size..."
& $adb shell "run-as com.example.foodexpiryapp ls -la files/llm/mmproj.gguf"

Write-Host "Done."
