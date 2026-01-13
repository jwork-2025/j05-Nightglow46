Write-Host "编译游戏引擎..."

# 创建输出目录
if (!(Test-Path "build/classes")) {
    New-Item -ItemType Directory -Path "build/classes" -Force | Out-Null
}

# 获取所有Java文件
$javaFiles = Get-ChildItem -Path "src/main/java" -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName

# 编译
if ($javaFiles) {
    # 直接编译
    javac -d build/classes -cp . $javaFiles
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "编译成功！"
        Write-Host "运行游戏: java -cp build/classes com.gameengine.example.GameExample"
    } else {
        Write-Host "编译失败！"
    }
    
    Remove-Item "sources.txt" -ErrorAction SilentlyContinue
} else {
    Write-Host "未找到Java源文件！"
}
