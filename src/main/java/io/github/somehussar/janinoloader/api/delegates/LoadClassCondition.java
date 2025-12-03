package io.github.somehussar.janinoloader.api.delegates;

/**
 * Interface for filtering classes
 */
public interface LoadClassCondition {
    /**
     * @param name Fully qualified class name (example: <code>io.github.somehussar.MyVeryCoolClass</code>)
     * @return True if the class is allowed to load.
     */
    boolean isValid(String name);

    /**
     * Allows for a better error message based on the filter if need be.
     *
     * @param name Fully qualified class name (example: <code>io.github.somehussar.MyVeryCoolClass</code>)
     * @return Error message
     */
    default String classNotLoadedMessage(String name) {
        return name;
    }
}
