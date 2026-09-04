package com.petcampus.knockdog

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test

/**
 * 헥사고날 경계를 코드로 강제한다. 규칙이 깨지면 빌드가 실패한다.
 */
class HexagonalArchitectureTest {
    private val classes =
        ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.petcampus.knockdog")

    @Test
    fun `application 계층은 adapter 계층에 의존하지 않는다`() {
        noClasses()
            .that()
            .resideInAPackage("..application..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..adapter..")
            .check(classes)
    }

    @Test
    fun `application 계층은 JPA(jakarta persistence)에 의존하지 않는다`() {
        noClasses()
            .that()
            .resideInAPackage("..application..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("jakarta.persistence..")
            .check(classes)
    }

    @Test
    fun `도메인(domain) 패키지는 application, adapter 계층에 의존하지 않는다`() {
        noClasses()
            .that()
            .resideInAPackage("..domain")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..application..", "..adapter..")
            .check(classes)
    }

    @Test
    fun `정석형 순수 도메인(auth)은 Spring, JPA에 의존하지 않는다`() {
        noClasses()
            .that()
            .resideInAnyPackage(
                "com.petcampus.knockdog.domain.auth.domain..",
                "com.petcampus.knockdog.domain.breed.domain..",
                "com.petcampus.knockdog.domain.pet.domain..",
            ).should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta.persistence..")
            .check(classes)
    }
}
