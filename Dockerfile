# 1. Gradle로 빌드하기 위한 베이스 이미지
FROM gradle:7.6.2-jdk17 AS builder

# gradle 사용자로 설정 (권한 문제 방지)
USER gradle

# 프로젝트 복사 및 의존성 캐싱
COPY --chown=gradle:gradle . /home/gradle/project
WORKDIR /home/gradle/project

# 빌드 실행 (jar 생성)
RUN gradle build --no-daemon

# 2. 실행 전용 베이스 이미지
FROM openjdk:17

# 빌드된 jar 복사
COPY --from=builder /home/gradle/project/build/libs/*.jar app.jar

# 실행
ENTRYPOINT ["java", "-jar", "/app.jar"]
