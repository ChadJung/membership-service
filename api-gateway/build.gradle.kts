plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:2023.0.0"))

    implementation("org.springframework.cloud:spring-cloud-starter-gateway")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
}
