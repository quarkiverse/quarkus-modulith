package io.quarkiverse.modulith.docs;

import java.util.List;
import java.util.Set;

/**
 * A CDI bean of an application module, the counterpart of Spring Modulith's {@code SpringBean}.
 *
 * @param type the bean class
 * @param apiBean whether other modules may use the bean: its class or one of its interfaces is exposed
 * @param interfacesWithinModule the interfaces of the bean that belong to the same module
 * @param stereotypes the architectural roles of the bean
 * @param listenerMethods the event listener methods of the bean
 */
public record ModuleBean(ModuleType type, boolean apiBean, List<ModuleType> interfacesWithinModule,
        Set<Stereotype> stereotypes, List<ListenerMethod> listenerMethods) {

    public ModuleBean {
        interfacesWithinModule = List.copyOf(interfacesWithinModule);
        stereotypes = Set.copyOf(stereotypes);
        listenerMethods = List.copyOf(listenerMethods);
    }

    /**
     * @return whether the bean has the given role
     */
    public boolean is(Stereotype stereotype) {
        return stereotypes.contains(stereotype);
    }
}
