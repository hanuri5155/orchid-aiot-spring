# 1. Gradle 빌드용 베이스 이미지
FROM gradle:7.6.2-jdk17 AS builder

# 루트 권한으로 실행
USER root

# 캐시 디렉토리 삭제 후 권한 재설정
RUN rm -rf /home/gradle/.gradle/caches && \
    mkdir -p /home/gradle/.gradle/caches && \
    chmod -R 777 /home/gradle/.gradle

# 프로젝트 복사
COPY . /home/gradle/project
WORKDIR /home/gradle/project

# 빌드 실행
RUN gradle build --no-daemon

# 2. 런타임용 이미지
FROM openjdk:17

# 빌드 결과 복사
COPY --from=builder /home/gradle/project/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]
