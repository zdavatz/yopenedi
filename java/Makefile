identifier="com.ywesee.java.yopenedi"

jar:
	./gradlew cli:shadowJar
	./gradlew email-fetcher:shadowJar
	./gradlew server:playBinary
.PHONEY: jar

clean:
	./gradlew clean
.PHONEY: clean

run-server:
	./gradlew server:runPlay
.PHONEY: run-server

dist-server:
	./gradlew server:dist
	echo "You can find the zips under java/server/build/distributions"
.PHONEY: dist-server
