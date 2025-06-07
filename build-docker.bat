@echo off
echo 开始构建Docker镜像...

REM 设置Maven路径 - 请根据实际情况修改
set MAVEN_PATH=E:\apache-maven-3.9.9

echo 使用Maven路径: %MAVEN_PATH%

REM 执行Docker构建命令
docker buildx build --build-context maven-repo=%MAVEN_PATH% --progress=plain -t mcpserver1:localmaven -f dockerbuild1 .

echo 构建完成。
