# Сборка фронтенда
FROM node:22-alpine AS frontend
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# Сборка бэкенда вместе со статикой фронтенда
FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /app
COPY backend/pom.xml ./
RUN mvn -q dependency:go-offline
COPY backend/src ./src
COPY --from=frontend /app/dist ./src/main/resources/static
RUN mvn -q -DskipTests package

# Запуск. Нужен именно JDK: сервер компилирует код встроенным javac.
# Версия закреплена на 21: песочница использует SecurityManager, которого нет в Java 24+.
# На более новой JVM сервер запустится, но откажется выполнять код учеников.
FROM eclipse-temurin:21-jdk
RUN useradd --system --uid 10001 --home-dir /app byte
WORKDIR /app
COPY --from=backend /app/target/byte.jar app.jar
# Анонимная статистика хранится в файле H2 в этом каталоге — подключите том, чтобы она переживала обновления.
RUN mkdir /app/data && chown byte /app/data
VOLUME /app/data
USER byte
ENV JAVA_OPTS="-Xmx512m -XX:+ExitOnOutOfMemoryError" \
    BYTE_ANALYTICS_DB="file:/app/data/analytics"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
