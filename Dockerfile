FROM openjdk:21-jdk-slim AS build

WORKDIR /src

COPY gradle.properties gradlew *.gradle.kts /src/
COPY gradle /src/gradle
COPY .git /src/.git
COPY api /src/api
COPY server /src/server
COPY data /src/data

RUN apt-get -y update && apt-get -y install git
RUN ./gradlew :server:shadowJar

FROM openjdk:21-jdk-slim AS run

LABEL maintainer="AllayPlus"

COPY --from=build /src/server/build/libs/allayplus-server-*-shaded.jar /home/allayplus/jar/allayplus.jar

RUN useradd --user-group \
            --no-create-home \
            --home-dir /home/allayplus \
            --shell /usr/sbin/nologin \
            allayplus

EXPOSE 19132/tcp
EXPOSE 19132/udp

RUN mkdir -p /home/allayplus/data && \
    chown -R allayplus:allayplus /home/allayplus

USER allayplus:allayplus

VOLUME /home/allayplus/data

WORKDIR /home/allayplus/data

ENTRYPOINT ["java"]
CMD [ "-jar", "/home/allayplus/jar/allayplus.jar"]
