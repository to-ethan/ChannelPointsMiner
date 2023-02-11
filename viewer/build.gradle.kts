plugins {
    jacoco
    alias(libs.plugins.springboot)
    alias(libs.plugins.springbootDependencies)
    alias(libs.plugins.jib)
    alias(libs.plugins.testLogger)
}

configurations {
    compileOnly {
        extendsFrom(configurations["annotationProcessor"])
    }
}

dependencies {
    implementation(libs.hikaricp)
    implementation(libs.mariadb)
    implementation(libs.sqlite)
    implementation(libs.mysql)
    implementation(libs.bundles.flyway)

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-web")

    testImplementation(libs.bundles.junit)
    testRuntimeOnly(libs.junitEngine)

    testImplementation(libs.bundles.assertj)
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-inline")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation(libs.awaitility)
    testImplementation(libs.bundles.jsonUnit)

    testImplementation("org.springframework.boot:spring-boot-starter-test")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    annotationProcessor("org.projectlombok:lombok")
    compileOnly("org.projectlombok:lombok")

    runtimeOnly("org.mariadb.jdbc:mariadb-java-client")
    runtimeOnly("org.xerial:sqlite-jdbc")

    developmentOnly("org.springframework.boot:spring-boot-devtools")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

testlogger {
    theme = com.adarshr.gradle.testlogger.theme.ThemeType.STANDARD_PARALLEL
    showPassed = false
    showPassedStandardStreams = false
}

tasks {
    test {
        useJUnitPlatform()
    }

    jacocoTestReport {
        reports {
            xml.required.set(true)
        }
    }
}

jib {
    from {
        image = "eclipse-temurin:17-jdk"
        platforms {
            platform {
                os = "linux"
                architecture = "arm64"
            }
            platform {
                os = "linux"
                architecture = "amd64"
            }
            platform {
                os = "linux"
                architecture = "arm"
            }
        }
    }
    to {
        auth {
            username = project.findProperty("dockerUsername").toString()
            password = project.findProperty("dockerPassword").toString()
        }
    }
    container {
        creationTime.set("USE_CURRENT_TIMESTAMP")
        ports = listOf("8080")
    }
}
