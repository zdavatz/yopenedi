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
