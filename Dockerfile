# 1. Gradle로 빌드하기 위한 베이스 이미지
FROM gradle:7.6.4-jdk17 AS build

# 권한 문제 해결
RUN rm -rf /home/gradle/.gradle/caches && \
    mkdir -p /home/gradle/.gradle/caches && \
    chmod -R 777 /home/gradle/.gradle

# 프로젝트 복사
COPY . /home/gradle/project
WORKDIR /home/gradle/project

# 테스트 제외하고 빌드
RUN gradle build --no-daemon -x test

# 2. 실행 전용 베이스 이미지
FROM openjdk:17

# 빌드된 JAR 복사
COPY --from=builder /home/gradle/project/build/libs/*.jar app.jar

# 실행
ENTRYPOINT ["java", "-jar", "/app.jar"]
