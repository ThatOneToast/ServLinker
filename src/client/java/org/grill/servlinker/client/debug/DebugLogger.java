package org.grill.servlinker.client.debug;

import org.slf4j.Logger;

public class DebugLogger {
    private final Logger logger;
    private boolean debugEnabled;

    public DebugLogger(Logger logger, boolean debugEnabled) {
        this.logger = logger;
        this.debugEnabled = debugEnabled;
    }

    public void debug(String msg, Object... arguments) {
        if (debugEnabled && logger.isDebugEnabled()) {
            logger.debug(msg, arguments);
        }
    }

    public void info(String msg, Object... arguments) {
        if (debugEnabled && logger.isInfoEnabled()) {
            logger.info(msg, arguments);
        }
    }

    public void warn(String msg, Object... arguments) {
        if (debugEnabled && logger.isWarnEnabled()) {
            logger.warn(msg, arguments);
        }
    }

    public void error(String msg, Object... arguments) {
        if (debugEnabled && logger.isErrorEnabled()) {
            logger.error(msg, arguments);
        }
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public void setDebugEnabled(boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
    }
}
