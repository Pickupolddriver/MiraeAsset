# 构建阶段：Maven + JDK 21
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# 先复制 pom 并预拉依赖，利用 Docker 层缓存加速重复构建
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

# 再复制源码并打包（跳过测试以加速云端构建）
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# 运行阶段：仅 JRE，镜像更小
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# 优先使用 PaaS 注入的 PORT，否则回退到 8080
ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT:-8080}"]