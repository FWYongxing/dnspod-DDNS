FROM azul/zulu-openjdk-alpine:12
COPY . /code
RUN mkdir /app &&\
  /code/gradlew -p /code clean distTar &&\
  tar -xf /code/build/distributions/dnspod-DDNS-0.0.1.tar -C /app --strip-components 1 &&\
  rm -rf ./code

CMD ["ash","/app/bin/dnspod-DDNS"]