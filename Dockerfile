# 1. Gradle로 빌드하기 위한 베이스 이미지
FROM gradle:7.6.2-jdk17 AS builder

# root 권한으로 gradle 캐시 디렉토리 권한 처리
USER root
RUN mkdir -p /home/gradle/.gradle/caches && chmod -R 777 /home/gradle/.gradle

# gradle 사용자로 변경
USER gradle

# 프로젝트 복사
COPY --chown=gradle:gradle . /home/gradle/project
WORKDIR /home/gradle/project

# 빌드 실행
RUN gradle build --no-daemon

# 2. 실행 전용 이미지
FROM openjdk:17
COPY --from=builder /home/gradle/project/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
