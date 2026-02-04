FROM eclipse-temurin:21-jre

## 실행 경로
WORKDIR /app/myce-backend/api

## 실행 파일
COPY build/libs/*.jar myce-core-api.jar

## 실행 포트
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "myce-core-api.jar", "--spring.profiles.active=product"]