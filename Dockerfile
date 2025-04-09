# 1. Gradle로 빌드하기 위한 베이스 이미지
FROM gradle:8.5.0-jdk17 AS builder

# Gradle 캐시 권한 문제 대응
RUN rm -rf /home/gradle/.gradle/caches && \
    mkdir -p /home/gradle/.gradle/caches && \
    chmod -R 777 /home/gradle/.gradle

# Gradle 실행 시 JVM 메모리 사용량 제한 (최대 512MB)
ENV GRADLE_OPTS="-Xmx512m -Dorg.gradle.daemon=false"

# gradlew만 먼저 복사하고 실행 권한 부여
COPY gradlew /home/gradle/project/
RUN chmod +x /home/gradle/project/gradlew

# 의존성 캐싱을 위해 gradle 설정 파일 먼저 복사
COPY build.gradle settings.gradle /home/gradle/project/
COPY gradle /home/gradle/project/gradle

# 작업 디렉토리 설정
WORKDIR /home/gradle/project

# 의존성 다운로드 (캐싱용)
RUN ./gradlew dependencies --no-daemon --build-cache

# 전체 프로젝트 복사
COPY . /home/gradle/project

# 다시 gradlew 실행 권한 부여 (덮어쓰기 보완!)
RUN chmod +x ./gradlew

# 실행 가능한 Spring Boot JAR만 빌드(테스트 제외)
RUN ./gradlew bootJar --no-daemon -x test --build-cache

# 2. 실행 전용 베이스 이미지
FROM openjdk:17

# 빌드된 jar 복사
COPY --from=builder /home/gradle/project/build/libs/app.jar /app.jar

# 실행
ENTRYPOINT ["java", "-jar", "/app.jar"]