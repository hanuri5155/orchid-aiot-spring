# Java 17 이미지 사용
FROM openjdk:17

# Jar 파일 경로 설정
ARG JAR_FILE=build/libs/*.jar

# 해당 jar를 app.jar 이름으로 복사
COPY ${JAR_FILE} app.jar

# 실행
ENTRYPOINT ["java", "-jar", "/app.jar"]