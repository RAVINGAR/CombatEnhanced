package com.ravingarinc.eldenrhym.api;

import java.util.Iterator;

public class ManagerLoadException extends Exception {
    public ManagerLoadException(final Module module, final Reason reason) {
        super(reason.getMessage(module));
    }

    public ManagerLoadException(final Module module, final Throwable throwable) {
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
                iterator.forEachRemaining(clazz -> {
                    final String[] split = clazz.getName().split("\\.");
                    builder.append(split[split.length - 1]);
                    if (iterator.hasNext()) {
                        builder.append(", ");
                    }
                });
                builder.append(" were not loaded!");
                return builder.toString();
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
