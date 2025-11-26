#!/bin/bash

# 简单编译脚本
echo "编译游戏引擎..."

# 创建输出目录
mkdir -p build/classes

# 编译所有Java文件
# 自动查找所有Java文件进行编译
find src/main/java -name "*.java" > sources.txt
javac -d build/classes -cp . @sources.txt
rm sources.txt

if [ $? -eq 0 ]; then
    echo "编译成功！"
    echo "运行游戏: java -cp build/classes com.gameengine.example.GameExample"
else
    echo "编译失败！"
    exit 1
fi
