package it.raffaele.esposito.requestapp.request;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import it.raffaele.esposito.requestapp.request.domain.Request;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

class LayeringTest {

    private static final String DOMAIN = "..request.domain..";
    private static final String APPLICATION = "..request.application..";
    private static final String PORTS = "..request.ports..";
    private static final String PORTS_IN = "..request.ports.in";

    private static JavaClasses requestModule;

    @BeforeAll
    static void importTheModule() {
        requestModule = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("it.raffaele.esposito.requestapp.request");
    }

    @Test
    void theDomainDoesNotDependOnTheApplicationLayer() {
        final ArchRule rule = noClasses().that().resideInAPackage(DOMAIN)
                .should().dependOnClassesThat().resideInAnyPackage(APPLICATION, PORTS);

        rule.check(requestModule);
    }

    @Test
    void theModuleDoesNotDependOnAnyAdapter() {
        final ArchRule rule = noClasses().that().resideInAnyPackage(DOMAIN, APPLICATION, PORTS)
                .should().dependOnClassesThat().resideInAPackage("..adapter..");

        rule.check(requestModule);
    }

    @Test
    void everyDeliberateExceptionSharesTheCommonRoot() {
        final ArchRule rule = classes().that().haveSimpleNameEndingWith("Exception")
                .or().haveSimpleNameEndingWith("NotAllowed")
                .should().beAssignableTo("it.raffaele.esposito.requestapp.request.domain.exceptions.RequestException");

        rule.check(requestModule);
    }

    @Test
    void theInboundPortDoesNotHandOutTheAggregate() {
        final ArchRule rule = noMethods().that().areDeclaredInClassesThat().resideInAPackage(PORTS_IN)
                .should().haveRawReturnType(Request.class);

        rule.check(requestModule);
    }

    @Test
    void theApplicationLayerDoesNotRaiseDomainExceptions() {
        final ArchRule rule = noClasses().that().resideInAPackage(APPLICATION)
                .and().haveSimpleNameNotEndingWith("Exception")
                .should().dependOnClassesThat().resideInAPackage("..request.domain.exceptions..");

        rule.check(requestModule);
    }
}
