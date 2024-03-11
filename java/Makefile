identifier="com.ywesee.java.yopenedi"

jar:
	./gradlew cli:shadowJar
	./gradlew email-fetcher:shadowJar
	mkdir -p server/lib/
	./gradlew converter:shadowJar && cp converter/build/libs/converter-1.0-all.jar ./server/lib/
	cd server && sbt dist
.PHONEY: jar

clean:
	./gradlew clean
.PHONEY: clean

run-server:
	mkdir -p server/lib/
	./gradlew converter:shadowJar && cp converter/build/libs/converter-1.0-all.jar ./server/lib/
	cd server && sbt run
.PHONEY: run-server

dist-server:
	./gradlew converter:shadowJar && cp converter/build/libs/converter-1.0-all.jar ./server/lib/
	cd server && sbt dist
.PHONEY: dist-server
