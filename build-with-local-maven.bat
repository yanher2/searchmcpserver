@echo off
setlocal enabledelayedexpansion

echo 构建使用本地Maven仓库的Docker镜像
echo =====================================

:: 获取Maven本地仓库路径
if "%MAVEN_REPO%"=="" (
    :: 尝试从默认位置获取
    if exist "%USERPROFILE%\.m2\repository" (
        set "MAVEN_REPO=%USERPROFILE%\.m2\repository"
    ) else (
        echo 错误：未找到Maven本地仓库
        echo 请设置MAVEN_REPO环境变量指向你的Maven仓库位置
        exit /b 1
    )
)

:: 转换路径分隔符（将反斜杠转换为正斜杠）
set "MAVEN_REPO_UNIX=%MAVEN_REPO:\=/%"

echo 使用Maven仓库路径: %MAVEN_REPO%

:: 构建第一阶段镜像（使用本地Maven仓库）
echo 正在构建构建阶段镜像...
docker build -t java-search-mcp-server-build:latest -f dockerbuild1 . ^
    --build-arg MAVEN_OPTS="-Dmaven.repo.local=/root/.m2/repository" ^
    --mount type=bind,source="%MAVEN_REPO_UNIX%",target=/root/.m2/repository

if %ERRORLEVEL% neq 0 (
    echo 构建阶段镜像构建失败
    exit /b 1
)

:: 构建第二阶段镜像
echo 正在构建运行环境镜像...
docker build -t java-search-mcp-server:latest .

if %ERRORLEVEL% neq 0 (
    echo 运行环境镜像构建失败
    exit /b 1
)

echo.
echo 构建完成!
echo 构建镜像: java-search-mcp-server-build:latest
echo 运行镜像: java-search-mcp-server:latest

endlocal
