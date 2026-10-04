# demo-app 压测/演示镜像：仅依赖 JRE，starter 功能为进程内实现，无外部依赖服务
FROM eclipse-temurin:17-jre

WORKDIR /app
COPY target/web-common-demo-app-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
