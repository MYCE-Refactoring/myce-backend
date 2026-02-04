FROM eclipse-temurin:21-jre

## 실행 경로
WORKDIR /app/myce-backend/internal

## 실행 파일
COPY ./internal/build/libs/*.jar myce-core-internal.jar

## 실행 포트
EXPOSE 8082

ENTRYPOINT ["java", "-jar", "myce-core-internal.jar", "--spring.profiles.active=product"]