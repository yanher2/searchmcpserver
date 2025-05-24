#!/bin/bash

# 京东二手笔记本搜索服务 Kubernetes 部署脚本

# 显示彩色输出
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${YELLOW}===== 京东二手笔记本搜索服务 Kubernetes 部署脚本 =====${NC}"

# 检查 kubectl 是否可用
if ! command -v kubectl &> /dev/null; then
    echo -e "${RED}错误: kubectl 命令未找到，请安装 kubectl${NC}"
    exit 1
fi

# 检查 Kubernetes 集群是否可用
echo -e "${YELLOW}检查 Kubernetes 集群状态...${NC}"
if ! kubectl cluster-info &> /dev/null; then
    echo -e "${RED}错误: 无法连接到 Kubernetes 集群，请确保集群正在运行${NC}"
    exit 1
fi
echo -e "${GREEN}Kubernetes 集群正常运行${NC}"

# 构建应用镜像
echo -e "${YELLOW}构建应用镜像...${NC}"
if ! docker build -t jd-laptop-app:latest ..; then
    echo -e "${RED}错误: 镜像构建失败${NC}"
    exit 1
fi
echo -e "${GREEN}镜像构建成功${NC}"

# 创建 PostgreSQL 相关资源
echo -e "${YELLOW}部署 PostgreSQL 资源...${NC}"
kubectl apply -f postgres-configmap.yaml
kubectl apply -f postgres-pvc.yaml
kubectl apply -f postgres-deployment.yaml
kubectl apply -f postgres-service.yaml

# 等待 PostgreSQL 就绪
echo -e "${YELLOW}等待 PostgreSQL 就绪...${NC}"
kubectl wait --for=condition=ready pod -l app=postgres --timeout=120s
if [ $? -ne 0 ]; then
    echo -e "${RED}错误: PostgreSQL 未能在指定时间内就绪${NC}"
    echo -e "${YELLOW}请检查 PostgreSQL pod 状态:${NC}"
    kubectl get pods -l app=postgres
    kubectl describe pods -l app=postgres
    exit 1
fi
echo -e "${GREEN}PostgreSQL 已就绪${NC}"

# 创建应用相关资源
echo -e "${YELLOW}部署应用资源...${NC}"
kubectl apply -f app-deployment.yaml
kubectl apply -f app-service.yaml

# 等待应用就绪
echo -e "${YELLOW}等待应用就绪...${NC}"
kubectl wait --for=condition=ready pod -l app=jd-laptop-app --timeout=120s
if [ $? -ne 0 ]; then
    echo -e "${RED}错误: 应用未能在指定时间内就绪${NC}"
    echo -e "${YELLOW}请检查应用 pod 状态:${NC}"
    kubectl get pods -l app=jd-laptop-app
    kubectl describe pods -l app=jd-laptop-app
    exit 1
fi
echo -e "${GREEN}应用已就绪${NC}"

# 获取服务访问信息
echo -e "${YELLOW}获取服务访问信息...${NC}"
SERVICE_IP=$(kubectl get service jd-laptop-app -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
SERVICE_PORT=$(kubectl get service jd-laptop-app -o jsonpath='{.spec.ports[0].port}')

if [ -z "$SERVICE_IP" ]; then
    SERVICE_IP="localhost"
    echo -e "${YELLOW}使用 Docker Desktop Kubernetes 的本地访问地址${NC}"
fi

echo -e "${GREEN}部署完成!${NC}"
echo -e "${GREEN}应用访问地址: http://${SERVICE_IP}:${SERVICE_PORT}${NC}"
echo -e "${YELLOW}可以使用以下命令查看应用日志:${NC}"
echo -e "kubectl logs -f deployment/jd-laptop-app"
#!/