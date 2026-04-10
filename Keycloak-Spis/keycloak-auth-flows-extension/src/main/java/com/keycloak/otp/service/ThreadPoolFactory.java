package com.keycloak.otp.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.keycloak.otp.util.Constants;
import org.jboss.logging.Logger;
import org.keycloak.utils.StringUtil;

/**
 * Factory class for building a configurable thread pool executor used for handling
 * background tasks such as sending OTP emails or SMS asynchronously.
 *
 * @author Yasir Rehman
 */
public class ThreadPoolFactory {

    private static final Logger log = Logger.getLogger(ThreadPoolFactory.class);
    private static final Properties props = new Properties();

    // Static block to load configuration from file during class initialization
    static {
        String path = System.getenv(Constants.THREAD_POOL_CONFIG_PATH);
        String configPath = StringUtil.isNullOrEmpty(path)
                ? Constants.DEFAULT_THREAD_POOL_CONFIG_PATH
                : path;

        try (InputStream input = new FileInputStream(configPath)) {
            props.load(input);
            log.infof("Loaded thread pool config from: %s", configPath);
        } catch (IOException e) {
            log.warnf("Failed to load config from '%s'. Using default values. Error: %s", configPath, e.getMessage());
        }
    }

    /**
     * Builds and returns a configured {@link ExecutorService} using values
     * from the loaded properties file or fallback defaults.
     *
     * @return a thread pool executor instance
     */
    public static ExecutorService buildExecutorFromEnv() {
        int coreSize  = getInt(Constants.THREAD_POOL_CORE_SIZE, Constants.DEFAULT_CORE_POOL_SIZE);
        int maxSize   = getInt(Constants.THREAD_POOL_MAX_SIZE, Constants.DEFAULT_MAX_POOL_SIZE);
        int queueSize = getInt(Constants.THREAD_POOL_QUEUE_SIZE, Constants.DEFAULT_QUEUE_SIZE);

        log.infof("Initializing ExecutorService with corePoolSize=%d, maxPoolSize=%d, queueSize=%d",
                coreSize, maxSize, queueSize);

        return new ThreadPoolExecutor(
                coreSize,
                maxSize,
                60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueSize),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * Retrieves an integer value from the loaded properties using the specified key.
     * If the key is not present or the value is invalid, the provided default is returned.
     *
     * @param key the property key to look up
     * @param defaultValue the fallback value if the key is not found or is invalid
     * @return the resolved integer value
     */
    private static int getInt(String key, int defaultValue) {
        String value = props.getProperty(key);
        if (StringUtil.isNullOrEmpty(value)) {
            log.infof("Property '%s' not found. Using default: %d", key, defaultValue);
            return defaultValue;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.warnf("Invalid value for property '%s': '%s'. Using default: %d", key, value, defaultValue);
            return defaultValue;
        }
    }
}
