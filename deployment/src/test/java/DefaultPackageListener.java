import io.quarkiverse.modulith.ApplicationModuleListener;
import io.quarkiverse.modulith.deployment.fixtures.events.nomodel.Deliveries;
import io.quarkiverse.modulith.deployment.fixtures.events.nomodel.Other;

/**
 * A listener in the default package, so the application classes share no package and there is no module model.
 */
public class DefaultPackageListener {

    @ApplicationModuleListener(async = false)
    void on(Other event) {
        Deliveries.RECEIVED.add(event);
    }
}
