Write-Host "编译游戏引擎..."

# 创建输出目录
if (!(Test-Path "build/classes")) {
    New-Item -ItemType Directory -Path "build/classes" -Force | Out-Null
}

# 获取所有Java文件
$javaFiles = Get-ChildItem -Path "src/main/java" -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName

# 编译
if ($javaFiles) {
    # 使用文件列表文件来避免命令行过长的问题
    $javaFiles | Out-File -Encoding utf8 "sources.txt"
    
    javac -d build/classes -cp . @sources.txt
    
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
