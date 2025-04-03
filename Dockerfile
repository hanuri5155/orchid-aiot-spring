# 1. Gradle로 빌드하기 위한 베이스 이미지
FROM gradle:8.5.0-jdk17 AS builder

# Gradle 캐시 권한 문제 대응
RUN rm -rf /home/gradle/.gradle/caches && \
    mkdir -p /home/gradle/.gradle/caches && \
    chmod -R 777 /home/gradle/.gradle

# 빌드에 필요한 설정 파일 먼저 복사
COPY build.gradle settings.gradle gradlew /home/gradle/project/
COPY gradle /home/gradle/project/gradle

# 작업 디렉토리 설정
WORKDIR /home/gradle/project

# gradlew 실행 권한 부여
RUN chmod +x ./gradlew

# 의존성만 먼저 캐싱 (속도 핵심!)
RUN ./gradlew dependencies --no-daemon --build-cache

# 전체 프로젝트 복사 (이 시점에만 캐시 무효화됨)
COPY . /home/gradle/project

# 실제 빌드 수행
RUN ./gradlew build --no-daemon -x test --build-cache

# 2. 실행 전용 베이스 이미지
FROM openjdk:17

# 빌드된 jar 복사
COPY --from=builder /home/gradle/project/build/libs/*.jar app.jar

# 실행
ENTRYPOINT ["java", "-jar", "/app.jar"]