To run the jcstress concurrency tests directly without Gradle, do the following:

```
./gradlew ./gradlew :agrona-concurrency-tests:shadowJar
```
The above will build the appropriate JCStress binary.

And then:
```
java -jar agrona-concurrency-tests/build/libs/concurrency-tests.jar YourTest -jvmArgsPrepend "--add-exports java.base/jdk.internal.misc=ALL-UNNAMED"
```
Make sure to replace the `YourTest` by the appropriate JCStress test.