package com.syncadmaximo;

import com.syncadmaximo.orchestration.SyncOrchestratorMockTest;
import com.syncadmaximo.service.DefaultValidationServiceSpec;
import com.syncadmaximo.service.IdentityMatchingServiceSpec;
import com.syncadmaximo.service.InactivationServiceTest;
import com.syncadmaximo.service.ReportServiceTest;
import com.syncadmaximo.service.RunHistoryServiceSpec;
import com.syncadmaximo.service.SyncDataLoadServiceSpec;
import com.syncadmaximo.web.demo.DemoDataFactoryMockTest;
import com.syncadmaximo.web.servlet.ReportControllerHistorySpec;
import com.syncadmaximo.validation.SyncValidatorMockTest;

public final class MockTestRunner {

    private MockTestRunner() {
    }

    public static void main(String[] args) {
        int failures = 0;
        failures += run("DemoDataFactoryMockTest", DemoDataFactoryMockTest::runAll);
        failures += run("DefaultValidationServiceSpec", DefaultValidationServiceSpec::runAll);
        failures += run("IdentityMatchingServiceSpec", IdentityMatchingServiceSpec::runAll);
        failures += run("InactivationServiceTest", InactivationServiceTest::runAll);
        failures += run("ReportServiceTest", ReportServiceTest::runAll);
        failures += run("RunHistoryServiceSpec", RunHistoryServiceSpec::runAll);
        failures += run("SyncDataLoadServiceSpec", SyncDataLoadServiceSpec::runAll);
        failures += run("SyncValidatorMockTest", SyncValidatorMockTest::runAll);
        failures += run("ReportControllerHistorySpec", ReportControllerHistorySpec::runAll);
        failures += run("SyncOrchestratorMockTest", SyncOrchestratorMockTest::runAll);
        if (failures > 0) {
            System.err.println("FAILURES=" + failures);
            System.exit(1);
        }
        System.out.println("ALL MOCK TESTS PASSED");
    }

    private static int run(String name, ThrowingRunnable runnable) {
        try {
            runnable.run();
            System.out.println("PASS " + name);
            return 0;
        } catch (Throwable throwable) {
            System.err.println("FAIL " + name + ": " + throwable.getMessage());
            throwable.printStackTrace(System.err);
            return 1;
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
