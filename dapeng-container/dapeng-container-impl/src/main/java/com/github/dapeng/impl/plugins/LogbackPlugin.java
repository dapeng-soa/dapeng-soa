package com.github.dapeng.impl.plugins;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.util.StatusPrinter;
import com.github.dapeng.core.Plugin;
import com.github.dapeng.impl.container.DapengContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.InputStream;

/**
 * Logback Container
 *
 * @author craneding
 * @date 16/1/18
 */
public class LogbackPlugin implements Plugin {
    private static Logger LOGGER = LoggerFactory.getLogger(LogbackPlugin.class);

    @Override
    public void start() {
        LOGGER.warn("Plugin::" + getClass().getSimpleName() + "::start");

        try (InputStream logbackCnfgStream = new BufferedInputStream(DapengContainer.loadInputStreamInClassLoader("logback.xml"))) {
            LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(lc);
            lc.reset();
            configurator.doConfigure(logbackCnfgStream);

            StatusPrinter.printInCaseOfErrorsOrWarnings(lc);
        } catch (Exception e) {
            LOGGER.error("LogbackContainer failed, ignoring ..." + e.getMessage(), e);
            // throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        LOGGER.warn("Plugin::" + getClass().getSimpleName() + "::stop");
    }

}
