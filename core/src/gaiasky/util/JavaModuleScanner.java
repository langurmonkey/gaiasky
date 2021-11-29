package gaiasky.util;

import java.lang.StackWalker.Option;
import java.lang.StackWalker.StackFrame;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.lang.module.ResolvedModule;
import java.net.URI;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.AbstractMap.SimpleEntry;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Stream;

/**
 * Prints used modules
 */
public class JavaModuleScanner {

    /** Recursively find the topological sort order of ancestral layers. */
    private static void findLayerOrder(ModuleLayer layer,
            Set<ModuleLayer> visited, Deque<ModuleLayer> layersOut) {
        if (visited.add(layer)) {
            List<ModuleLayer> parents = layer.parents();
            for (ModuleLayer parent : parents) {
                findLayerOrder(parent, visited, layersOut);
            }
            layersOut.push(layer);
        }
    }

    /** Get ModuleReferences from a Class reference. */
    private static List<Entry<ModuleReference, ModuleLayer>> findModuleRefs(
            Class<?>[] callStack) {
        Deque<ModuleLayer> layerOrder = new ArrayDeque<>();
        Set<ModuleLayer> visited = new HashSet<>();
        for (Class<?> aClass : callStack) {
            ModuleLayer layer = aClass.getModule().getLayer();
            if (layer != null) {
                findLayerOrder(layer, visited, layerOrder);
            }
        }
        Set<ModuleReference> addedModules = new HashSet<>();
        List<Entry<ModuleReference, ModuleLayer>> moduleRefs = new ArrayList<>();
        for (ModuleLayer layer : layerOrder) {
            Set<ResolvedModule> modulesInLayerSet = layer.configuration()
                    .modules();
            final List<Entry<ModuleReference, ModuleLayer>> modulesInLayer =
                    new ArrayList<>();
            for (ResolvedModule module : modulesInLayerSet) {
                modulesInLayer
                        .add(new SimpleEntry<>(module.reference(), layer));
            }
            // Sort modules in layer by name for consistency
            modulesInLayer.sort(Comparator.comparing(e -> e.getKey().descriptor().name()));
            // To be safe, dedup ModuleReferences, in case a module occurs in multiple
            // layers and reuses its ModuleReference (no idea if this can happen)
            for (Entry<ModuleReference, ModuleLayer> m : modulesInLayer) {
                if (addedModules.add(m.getKey())) {
                    moduleRefs.add(m);
                }
            }
        }
        return moduleRefs;
    }

    /** Get the classes in the call stack. */
    private static Class<?>[] getCallStack() {
        // Try StackWalker (JDK 9+)
        PrivilegedAction<Class<?>[]> stackWalkerAction = () ->
                StackWalker.getInstance(
                        Option.RETAIN_CLASS_REFERENCE)
                        .walk(s -> s.map(
                                StackFrame::getDeclaringClass)
                                .toArray(Class[]::new));
        try {
            // Try with doPrivileged()
            return AccessController
                    .doPrivileged(stackWalkerAction);
        } catch (Exception ignored) {
        }
        try {
            // Try without doPrivileged()
            return stackWalkerAction.run();
        } catch (Exception ignored) {
        }

        // Try SecurityManager
        PrivilegedAction<Class<?>[]> callerResolverAction = () ->
                new SecurityManager() {
                    @Override
                    public Class<?>[] getClassContext() {
                        return super.getClassContext();
                    }
                }.getClassContext();
        try {
            // Try with doPrivileged()
            return AccessController
                    .doPrivileged(callerResolverAction);
        } catch (Exception ignored) {
        }
        try {
            // Try without doPrivileged()
            return callerResolverAction.run();
        } catch (Exception ignored) {
        }

        // As a fallback, use getStackTrace() to try to get the call stack
        try {
            throw new Exception();
        } catch (final Exception e) {
            final List<Class<?>> classes = new ArrayList<>();
            for (final StackTraceElement elt : e.getStackTrace()) {
                try {
                    classes.add(Class.forName(elt.getClassName()));
                } catch (final Throwable e2) {
                    // Ignore
                }
            }
            if (classes.size() > 0) {
                return classes.toArray(new Class<?>[0]);
            } else {
                // Last-ditch effort -- include just this class
                return new Class<?>[] { JavaModuleScanner.class };
            }
        }
    }

    /**
     * Return true if the given module name is a system module.
     * There can be system modules in layers above the boot layer.
     */
    private static boolean isSystemModule(
            final ModuleReference moduleReference) {
        String name = moduleReference.descriptor().name();
        if (name == null) {
            return false;
        }
        return name.startsWith("java.") || name.startsWith("jdk.")
                || name.startsWith("javafx.") || name.startsWith("oracle.");
    }

    public static void main(String[] args) throws Exception {
        // Get ModuleReferences for modules of all classes in call stack,
        List<Entry<ModuleReference, ModuleLayer>> systemModuleRefs = new ArrayList<>();
        List<Entry<ModuleReference, ModuleLayer>> nonSystemModuleRefs = new ArrayList<>();

        Class<?>[] callStack = getCallStack();
        List<Entry<ModuleReference, ModuleLayer>> moduleRefs = findModuleRefs(
                callStack);
        // Split module refs into system and non-system modules based on module name
        for (Entry<ModuleReference, ModuleLayer> m : moduleRefs) {
            (isSystemModule(m.getKey()) ? systemModuleRefs
                    : nonSystemModuleRefs).add(m);
        }

        // List system modules
        System.out.println("\nSYSTEM MODULES:\n");
        for (Entry<ModuleReference, ModuleLayer> e : systemModuleRefs) {
            ModuleReference ref = e.getKey();
            System.out.println("  " + ref.descriptor().name());
        }

        // Show info for non-system modules
        System.out.println("\nNON-SYSTEM MODULES:");
        for (Entry<ModuleReference, ModuleLayer> e : nonSystemModuleRefs) {
            ModuleReference ref = e.getKey();
            ModuleLayer layer = e.getValue();
            System.out.println("\n  " + ref.descriptor().name());
            System.out.println(
                    "    Version: " + ref.descriptor().toNameAndVersion());
            System.out.println(
                    "    Packages: " + ref.descriptor().packages());
            System.out.println("    ClassLoader: "
                    + layer.findLoader(ref.descriptor().name()));
            Optional<URI> location = ref.location();
            location.ifPresent(uri -> System.out.println("    Location: " + uri));
            try (ModuleReader moduleReader = ref.open()) {
                Stream<String> stream = moduleReader.list();
                stream.forEach(s -> System.out.println("      File: " + s));
            }
        }
    }
}
