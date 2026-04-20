$adb = "C:\Users\jinjin\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$modelPath = "C:\Users\jinjin\AndroidStudioProjects\FoodExpiryApp\app\model_backup\model.gguf"
$mmprojPath = "C:\Users\jinjin\AndroidStudioProjects\FoodExpiryApp\app\model_backup\mmproj.gguf"

function Push-FileToApp {
    param(
        [string]$SourcePath,
        [string]$DestPath
    )
    
    $fileSize = (Get-Item $SourcePath).Length
    Write-Host "Pushing $(Split-Path $SourcePath -Leaf) ($([math]::Round($fileSize/1MB, 1)) MB)..."
    
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = $adb
    $psi.Arguments = "shell run-as com.example.foodexpiryapp sh -c 'cat > $DestPath'"
    $psi.UseShellExecute = $false
    $psi.RedirectStandardInput = $true
    $psi.RedirectStandardError = $true
    $psi.RedirectStandardOutput = $true
    $psi.CreateNoWindow = $true
    
    $proc = [System.Diagnostics.Process]::Start($psi)
    
    $fileStream = [System.IO.File]::OpenRead($SourcePath)
    $buffer = New-Object byte[] (1024 * 1024) # 1MB buffer
    $totalWritten = 0
    
    try {
        while ($true) {
            $bytesRead = $fileStream.Read($buffer, 0, $buffer.Length)
            if ($bytesRead -le 0) { break }
            $proc.StandardInput.BaseStream.Write($buffer, 0, $bytesRead)
            $totalWritten += $bytesRead
            $pct = [math]::Round($totalWritten / $fileSize * 100, 1)
            Write-Host "`r  Progress: $pct% ($([math]::Round($totalWritten/1MB, 1)) MB)" -NoNewline
        }
        Write-Host ""
        $proc.StandardInput.BaseStream.Flush()
        $proc.StandardInput.BaseStream.Close()
    } catch {
        Write-Host "`nError: $_"
    } finally {
        $fileStream.Close()
    }
    
    $proc.WaitForExit()
    $stderr = $proc.StandardError.ReadToEnd()
    if ($stderr) { Write-Host "Stderr: $stderr" }
    Write-Host "Done. Written: $([math]::Round($totalWritten/1MB, 1)) MB"
}

Push-FileToApp -SourcePath $modelPath -DestPath "/data/data/com.example.foodexpiryapp/files/llm/model.gguf"
Push-FileToApp -SourcePath $mmprojPath -DestPath "/data/data/com.example.foodexpiryapp/files/llm/mmproj.gguf"
