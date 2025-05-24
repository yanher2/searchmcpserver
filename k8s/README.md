# Kubernetes 部署说明

本文档说明如何在 Kubernetes 环境中部署京东二手笔记本搜索服务。

## 前置条件

1. 已安装并启用 Docker Desktop 的 Kubernetes
2. 已安装 kubectl 命令行工具
3. Kubernetes 集群已正常运行（可通过 `kubectl cluster-info` 验证）

## 部署步骤

### 1. 构建应用镜像

```bash
# 在项目根目录下执行
docker build -t jd-laptop-app:latest .
```

### 2. 创建 Kubernetes 资源

按以下顺序创建资源：

```bash
# 创建 PostgreSQL 相关资源
kubectl apply -f k8s/postgres-configmap.yaml
kubectl apply -f k8s/postgres-pvc.yaml
kubectl apply -f k8s/postgres-deployment.yaml
kubectl apply -f k8s/postgres-service.yaml

# 等待 PostgreSQL 就绪
kubectl wait --for=condition=ready pod -l app=postgres

# 创建应用相关资源
kubectl apply -f k8s/app-deployment.yaml
kubectl apply -f k8s/app-service.yaml
```

### 3. 验证部署

```bash
# 查看所有资源状态
kubectl get all

# 查看 PostgreSQL pod 状态
kubectl get pods -l app=postgres

# 查看应用 pod 状态
kubectl get pods -l app=jd-laptop-app

# 查看服务访问地址
kubectl get services jd-laptop-app
```

### 4. 访问应用

应用通过 LoadBalancer 类型的 Service 暴露，在本地 Docker Desktop Kubernetes 环境中：
- 访问地址：http://localhost:8080

### 5. 查看日志

```bash
# 查看应用日志
kubectl logs -f deployment/jd-laptop-app

# 查看数据库日志
kubectl logs -f deployment/postgres
```

### 6. 清理资源

如果需要删除所有创建的资源：

```bash
kubectl delete -f k8s/
```

## 注意事项

1. **持久化存储**
   - PostgreSQL 数据存储在 PersistentVolumeClaim 中
   - 删除 PVC 会导致数据丢失

2. **Chrome 和 ChromeDriver**
   - 应用依赖 Chrome 和 ChromeDriver
   - 确保这些二进制文件在容器中可用

3. **资源限制**
   - 默认配置未设置资源限制
   - 根据需要在 Deployment 中添加资源请求和限制

4. **环境变量**
   - 数据库连接信息通过环境变量配置
   - 可以根据需要修改环境变量

5. **扩展性**
   - 应用和数据库都配置为单副本
   - 根据需要可以增加应用副本数
   - 数据库扩展需要额外配置

## 故障排除

1. **Pod 启动失败**
   ```bash
   kubectl describe pod <pod-name>
   kubectl logs <pod-name>
   ```

2. **数据库连接问题**
   - 检查 Service 名称解析
   - 验证环境变量配置
   - 检查数据库初始化状态

3. **应用访问问题**
   - 确认 Service 类型和端口配置
   - 检查 LoadBalancer 是否正常工作
   - 验证应用健康检查状态

## 配置说明

### PostgreSQL 配置
- 数据库名：jd_laptops
- 用户名：postgres
- 密码：postgres
- 端口：5432

### 应用配置
- 端口：8080
- 数据库连接 URL：jdbc:postgresql://postgres:5432/jd_laptops
- Chrome 路径：/usr/bin/google-chrome
- ChromeDriver 路径：/usr/bin/chromedriver
