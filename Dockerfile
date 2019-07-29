FROM azul/zulu-openjdk-alpine:11.0.4-jre
COPY build/libs/com.fuyongxing-0.0.1.jar /app.jar
CMD ["java","-jar","/app.jar"]