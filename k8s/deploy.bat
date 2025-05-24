@echo off
REM 京东二手笔记本搜索服务 Kubernetes 部署脚本 (Windows版)

echo ===== 京东二手笔记本搜索服务 Kubernetes 部署脚本 =====

REM 检查 kubectl 是否可用
where kubectl >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo 错误: kubectl 命令未找到，请安装 kubectl
    exit /b 1
)

REM 检查 Kubernetes 集群是否可用
echo 检查 Kubernetes 集群状态...
kubectl cluster-info >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo 错误: 无法连接到 Kubernetes 集群，请确保集群正在运行
    exit /b 1
)
echo Kubernetes 集群正常运行

REM 构建应用镜像
echo 构建应用镜像...
cd ..
docker build -t jd-laptop-app:latest .
if %ERRORLEVEL% neq 0 (
    echo 错误: 镜像构建失败
    exit /b 1
)
echo 镜像构建成功
cd k8s

REM 创建 PostgreSQL 相关资源
echo 部署 PostgreSQL 资源...
kubectl apply -f postgres-configmap.yaml
kubectl apply -f postgres-pvc.yaml
kubectl apply -f postgres-deployment.yaml
kubectl apply -f postgres-service.yaml

REM 等待 PostgreSQL 就绪
echo 等待 PostgreSQL 就绪...
kubectl wait --for=condition=ready pod -l app=postgres --timeout=120s
if %ERRORLEVEL% neq 0 (
    echo 错误: PostgreSQL 未能在指定时间内就绪
    echo 请检查 PostgreSQL pod 状态:
    kubectl get pods -l app=postgres
    kubectl describe pods -l app=postgres
    exit /b 1
)
echo PostgreSQL 已就绪

REM 创建应用相关资源
echo 部署应用资源...
kubectl apply -f app-deployment.yaml
kubectl apply -f app-service.yaml

REM 等待应用就绪
echo 等待应用就绪...
kubectl wait --for=condition=ready pod -l app=jd-laptop-app --timeout=120s
if %ERRORLEVEL% neq 0 (
    echo 错误: 应用未能在指定时间内就绪
    echo 请检查应用 pod 状态:
    kubectl get pods -l app=jd-laptop-app
    kubectl describe pods -l app=jd-laptop-app
    exit /b 1
)
echo 应用已就绪

REM 获取服务访问信息
echo 获取服务访问信息...
for /f "tokens=*" %%i in ('kubectl get service jd-laptop-app -o jsonpath^="{.spec.ports[0].port}"') do set SERVICE_PORT=%%i

echo 部署完成!
echo 应用访问地址: http://localhost:%SERVICE_PORT%
echo 可以使用以下命令查看应用日志:
echo kubectl logs -f deployment/jd-laptop-app
