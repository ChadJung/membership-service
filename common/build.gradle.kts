plugins {
    `java-library`
}

dependencies {
    // ApiResponse/ErrorCode/GlobalExceptionHandler use Spring MVC + Jackson types.
    api("org.springframework:spring-web")
    implementation("org.springframework:spring-context")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("org.slf4j:slf4j-api")
}
