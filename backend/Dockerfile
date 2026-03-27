# 使用OpenJDK 21作为基础镜像
FROM eclipse-temurin:21-jre

# 设置工作目录
WORKDIR /app

# 复制Maven构建产物
COPY target/ai-translate-1.0.0.jar app.jar

# 暴露端口
EXPOSE 8080

# 设置JVM参数
ENV JAVA_OPTS="-Xms512m -Xmx1024m"

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
