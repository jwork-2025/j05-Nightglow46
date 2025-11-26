Write-Host "启动游戏引擎..."

# 执行编译脚本
./compile.ps1

if ($LASTEXITCODE -eq 0) {
    Write-Host "运行游戏..."
    java -cp build/classes com.gameengine.example.GameExample
}
