# 1. Gradle로 빌드하기 위한 베이스 이미지
FROM gradle:7.6.4-jdk17 AS builder

# Gradle 캐시 권한 문제 대응
RUN rm -rf /home/gradle/.gradle/caches && \
    mkdir -p /home/gradle/.gradle/caches && \
    chmod -R 777 /home/gradle/.gradle

# 프로젝트 복사 및 빌드
COPY . /home/gradle/project
WORKDIR /home/gradle/project
RUN ./gradlew build --no-daemon -x test

# 2. 실행 전용 베이스 이미지
FROM openjdk:17

# 빌드된 jar 복사
COPY --from=builder /home/gradle/project/build/libs/*.jar app.jar

# 실행
ENTRYPOINT ["java", "-jar", "/app.jar"]
