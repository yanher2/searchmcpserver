@echo off
REM 京东二手笔记本搜索服务 Kubernetes 资源清理脚本 (Windows版)

echo ===== 京东二手笔记本搜索服务 Kubernetes 资源清理脚本 =====

REM 检查 kubectl 是否可用
where kubectl >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo 错误: kubectl 命令未找到，请安装 kubectl
    exit /b 1
)

echo 正在删除应用资源...
kubectl delete -f app-service.yaml
kubectl delete -f app-deployment.yaml

echo 正在删除 PostgreSQL 资源...
kubectl delete -f postgres-service.yaml
kubectl delete -f postgres-deployment.yaml

echo 是否删除持久化数据? 这将永久删除所有数据库数据! (Y/N)
set /p DELETE_DATA=

if /i "%DELETE_DATA%"=="Y" (
    echo 删除 PostgreSQL PVC 和 ConfigMap...
    kubectl delete -f postgres-pvc.yaml
    kubectl delete -f postgres-configmap.yaml
    echo 所有资源和数据已删除
) else (
    echo 保留 PostgreSQL PVC 和 ConfigMap
    echo 应用和数据库服务已删除，但数据仍然保留
)

echo 清理完成!
@echo off