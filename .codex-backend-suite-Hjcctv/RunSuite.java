import java.io.PrintWriter;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

public final class RunSuite {
    private RunSuite() {
    }

    public static void main(String[] args) {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(
                        DiscoverySelectors.selectClass(
                                "com.example.myfschool.AuthControllerIntegrationTest"
                        ),
                        DiscoverySelectors.selectClass(
                                "com.example.myfschool.TimetableControllerIntegrationTest"
                        )
                )
                .build();
        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);
        listener.getSummary().printTo(new PrintWriter(System.out, true));
        if (listener.getSummary().getTotalFailureCount() > 0) {
            System.exit(1);
        }
    }
}
