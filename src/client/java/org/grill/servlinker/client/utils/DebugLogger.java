package org.grill.servlinker.client.utils;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;

public class DebugLogger {
    private final Logger logger;
    @Setter @Getter private boolean logsEnabled;

    public DebugLogger(Logger logger, boolean logsEnabled) {
        this.logger = logger;
        this.logsEnabled = logsEnabled;
    }

    public void debug(String msg, Object... arguments) {
        if (logsEnabled && logger.isDebugEnabled()) {
            logger.debug(msg, arguments);
        }
    }

    public void info(String msg, Object... arguments) {
        if (logsEnabled && logger.isInfoEnabled()) {
            logger.info(msg, arguments);
        }
    }

    public void warn(String msg, Object... arguments) {
        if (logsEnabled && logger.isWarnEnabled()) {
            logger.warn(msg, arguments);
        }
    }

    public void error(String msg, Object... arguments) {
        if (logsEnabled && logger.isErrorEnabled()) {
            logger.error(msg, arguments);
        }
    }

}
