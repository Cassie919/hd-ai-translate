# ===== 构建阶段 =====
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

# 先复制 pom.xml 提前下载依赖，利用 Docker 层缓存
COPY pom.xml .
RUN mvn -B dependency:go-offline

# 复制源码并打包
COPY src ./src
RUN mvn -B clean package -DskipTests

# ===== 运行阶段 =====
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=builder /build/target/ai-translate-1.0.0.jar app.jar

EXPOSE 8124

ENV JAVA_OPTS="-Xms512m -Xmx1024m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
