# 1. Gradle로 빌드하기 위한 베이스 이미지
FROM gradle:8.5-jdk21 AS builder

# Gradle 캐시 권한 문제 대응
RUN rm -rf /home/gradle/.gradle/caches && \
    mkdir -p /home/gradle/.gradle/caches && \
    chmod -R 777 /home/gradle/.gradle

# 프로젝트 복사 및 빌드
COPY . /home/gradle/project
WORKDIR /home/gradle/project

# gradlew 실행 권한 부여
RUN chmod +x ./gradlew

# 빌드 실행
RUN ./gradlew build --no-daemon -x test

# 2. 실행 전용 베이스 이미지
FROM openjdk:21

# 빌드된 jar 복사
COPY --from=builder /home/gradle/project/build/libs/*.jar app.jar

# 실행
ENTRYPOINT ["java", "-jar", "/app.jar"]