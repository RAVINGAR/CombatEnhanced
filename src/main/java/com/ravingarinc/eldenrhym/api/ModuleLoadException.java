package com.ravingarinc.eldenrhym.api;

import java.util.Iterator;

public class ModuleLoadException extends Exception {
    public ModuleLoadException(final Module module, final Reason reason) {
        super(reason.getMessage(module));
    }

    public ModuleLoadException(final Module module, final Throwable throwable) {
        super(Reason.EXCEPTION.getMessage(module) + throwable.getMessage(), throwable);
    }

    public enum Reason {
        DEPENDENCY() {
            @Override
            public String getMessage(final Module module) {
                final StringBuilder builder = new StringBuilder();
                builder.append("Could not load ");
                builder.append(module.getName());
                builder.append(" as ");

                final Iterator<Class<? extends Module>> iterator = module.getDependsOn().iterator();
                int loaded = 0;
                while (iterator.hasNext()) {
                    final Class<? extends Module> clazz = iterator.next();
                    if (!isModuleLoaded(clazz, module)) {
                        loaded++;
                        final String[] split = clazz.getName().split("\\.");
                        builder.append(split[split.length - 1]);
                        if (iterator.hasNext()) {
                            builder.append(",");
                        }
                        builder.append(" ");
                    }
                }
                builder.append(loaded == 1 ? "was" : "were");
                builder.append(" not loaded!");
                return builder.toString();
            }

            private boolean isModuleLoaded(final Class<? extends Module> clazz, final Module module) {
                try {
                    return module.plugin.getModule(clazz).isLoaded();
                } catch (final IllegalArgumentException ignored) {
                    return false;
                }
            }
        },
        EXCEPTION() {
            @Override
            public String getMessage(final Module module) {
                return "Could not load " + module.getName() + " due to; ";
            }
        },
        UNKNOWN {
            @Override
            public String getMessage(final Module module) {
                return "Could not load " + module.getName() + " due to an unknown reason!";
            }
        };

        public abstract String getMessage(Module module);
    }
}
