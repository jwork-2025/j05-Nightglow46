#!/bin/bash

# 运行游戏脚本
echo "启动游戏引擎..."

# 编译
./compile.sh

if [ $? -eq 0 ]; then
    echo "运行游戏..."
    java -cp build/classes com.gameengine.example.GameExample
else
    echo "编译失败，无法运行游戏"
    exit 1
fi
