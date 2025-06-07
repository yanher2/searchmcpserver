# Docker构建指南

本指南说明如何使用本地Maven仓库构建Docker镜像，避免重复下载依赖。

## 前提条件

- 已安装Docker
- 已安装Maven
- Windows或Linux/macOS操作系统

## 配置说明

本构建方案通过Docker的build context功能，将本地Maven仓库挂载到构建环境中，从而避免在构建过程中重复下载依赖。

## 使用方法

### Windows用户

1. 编辑`build-docker.bat`文件，将`MAVEN_PATH`变量设置为您的Maven安装路径
   ```
   set MAVEN_PATH=E:\apache-maven-3.9.9
   ```

2. 运行批处理文件
   ```
   build-docker.bat
   ```

### Linux/macOS用户

1. 编辑`build-docker.sh`文件，将`MAVEN_PATH`变量设置为您的Maven安装路径
   ```
   MAVEN_PATH="/path/to/your/maven"
   ```

2. 添加执行权限并运行脚本
   ```
   chmod +x build-docker.sh
   ./build-docker.sh
   ```

## 构建过程说明

构建过程中会执行以下操作：

1. 将本地Maven仓库挂载为构建上下文
2. 复制Maven仓库内容到Docker镜像中
3. 创建或使用现有的Maven配置文件
4. 下载缺失的依赖（如果有）
5. 构建Java应用程序
6. 将构建结果复制到最终位置

## 故障排除

如果构建过程中遇到问题，请检查：

1. Maven路径是否正确设置
2. Docker是否已正确安装并运行
3. 本地Maven仓库是否包含必要的依赖
4. 构建日志中是否有具体错误信息

## 自定义配置

您可以根据需要修改`dockerbuild1`文件来自定义构建过程，例如：

- 更改Maven镜像版本
- 添加其他构建参数
- 修改Maven镜像源配置
- 调整构建输出位置
