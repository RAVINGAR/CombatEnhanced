package com.ravingarinc.eldenrhym.api;

import java.util.Iterator;

public class ManagerLoadException extends Exception {
    public ManagerLoadException(final Manager manager, final Reason reason) {
        super(reason.getMessage(manager));
    }

    public ManagerLoadException(final Manager manager, final Throwable throwable) {
        super(Reason.EXCEPTION.getMessage(manager) + throwable.getMessage(), throwable);
    }

    public enum Reason {
        DEPENDENCY() {
            @Override
            public String getMessage(final Manager manager) {
                final StringBuilder builder = new StringBuilder();
                builder.append("Could not load ");
                builder.append(manager.getName());
                builder.append(" as ");
                final Iterator<Class<? extends Manager>> iterator = manager.getDependsOn().iterator();
                iterator.forEachRemaining(clazz -> {
                    final String[] split = clazz.getName().split("\\.");
                    builder.append(split[split.length - 1]);
                    if (iterator.hasNext()) {
                        builder.append(",");
                    }
                });
                builder.append(" were not loaded!");
                return builder.toString();
            }
        },
        EXCEPTION() {
            @Override
            public String getMessage(final Manager manager) {
                return "Could not load " + manager.getName() + " due to; ";
            }
        },
        UNKNOWN {
            @Override
            public String getMessage(final Manager manager) {
                return "Could not load " + manager.getName() + " due to an unknown reason!";
            }
        };

        public abstract String getMessage(Manager manager);
    }
}
