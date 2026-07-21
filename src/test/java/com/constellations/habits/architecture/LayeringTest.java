package com.constellations.habits.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Hace ejecutable la Arquitectura Limpia. Sin estas reglas, "el dominio no depende de
 * Spring" es una intencion que se erosiona al primer import comodo.
 */
@AnalyzeClasses(
        packages = "com.constellations.habits",
        importOptions = ImportOption.DoNotIncludeTests.class)
class LayeringTest {

    @ArchTest
    static final ArchRule las_dependencias_apuntan_hacia_dentro = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Domain").definedBy("com.constellations.habits.domain..")
            .layer("Application").definedBy("com.constellations.habits.application..")
            .layer("Infrastructure").definedBy("com.constellations.habits.infrastructure..")
            .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Infrastructure")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure");

    @ArchTest
    static final ArchRule el_dominio_no_conoce_frameworks = noClasses()
            .that().resideInAPackage("com.constellations.habits.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta.persistence..", "jakarta.validation..",
                    "io.jsonwebtoken..", "lombok..")
            .because("el dominio debe poder compilarse y testearse sin ningun framework");

    @ArchTest
    static final ArchRule los_casos_de_uso_no_conocen_la_persistencia = noClasses()
            .that().resideInAPackage("com.constellations.habits.application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "jakarta.persistence..", "org.springframework.data..", "org.hibernate..")
            .because("los casos de uso hablan con puertos, no con JPA");

    @ArchTest
    static final ArchRule los_casos_de_uso_no_conocen_http = noClasses()
            .that().resideInAPackage("com.constellations.habits.application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework.web..", "org.springframework.http..", "jakarta.servlet..")
            .because("los casos de uso no deben saber que se exponen por REST");

    /**
     * Se comprueba por paquete y no por sufijo "Entity": ese sufijo tambien lo llevan
     * clases de Spring como ResponseEntity, y la regla se disparaba con falsos positivos.
     */
    @ArchTest
    static final ArchRule el_adaptador_de_persistencia_es_hermetico = noClasses()
            .that().resideOutsideOfPackage("com.constellations.habits.infrastructure.persistence..")
            .should().dependOnClassesThat()
            .resideInAPackage("com.constellations.habits.infrastructure.persistence..")
            .because("las entidades JPA son un detalle del adaptador, no un modelo compartido: "
                    + "el resto de la app solo debe ver los puertos");
}
