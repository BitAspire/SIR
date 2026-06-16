package com.bitaspire.sir.chat;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Manages the registration and execution of {@link ChatProcessor} instances.
 *
 * <p>Processors fall into two categories:
 * <ul>
 *   <li><b>Runtime processors</b> - registered programmatically by the plugin core or addons,
 *       not backed by a module (e.g. the configuration editor).</li>
 *   <li><b>Module processors</b> - {@link com.bitaspire.sir.module.SIRModule} instances that
 *       also implement {@link ChatProcessor} and are managed by the module loader.</li>
 * </ul>
 *
 * <p>Use {@link com.bitaspire.sir.SIRApi#getProcessorManager()} to obtain the active instance.
 */
public interface ProcessorManager {

    /**
     * Registers a runtime chat processor.
     *
     * <p>If the processor is already registered it is silently ignored.
     *
     * @param processor processor to register.
     */
    void register(@NotNull ChatProcessor processor);

    /**
     * Unregisters a previously registered runtime chat processor.
     *
     * <p>If the processor is not registered this is a no-op.
     *
     * @param processor processor to unregister.
     */
    void unregister(@NotNull ChatProcessor processor);

    /**
     * Returns the active chat processors in execution order.
     *
     * @param runtime {@code true} to include runtime processors in addition to module processors;
     *                {@code false} to return only module processors.
     * @return sorted list of processors.
     */
    @NotNull
    List<ChatProcessor> getProcessors(boolean runtime);

    /**
     * Returns all active chat processors (runtime and module) in execution order.
     *
     * <p>Equivalent to {@code getProcessors(true)}.
     *
     * @return sorted list of processors.
     */
    @NotNull
    default List<ChatProcessor> getProcessors() {
        return getProcessors(true);
    }

    /**
     * Processes a chat message through all active processors in order.
     *
     * <p>Processing stops for a given processor if the context has been cancelled.
     *
     * @param context mutable chat context passed to each processor.
     */
    default void process(@NotNull ChatProcessor.Context context) {
        for (ChatProcessor processor : getProcessors())
            if (!context.isCancelled()) processor.process(context);
    }

    /**
     * Returns whether the modern Paper chat pipeline is currently active.
     *
     * <p>When {@code true}, legacy {@code AsyncPlayerChatEvent} listeners should not
     * process chat independently, as the Channels module is already handling it through
     * Paper's {@code AsyncChatEvent}.
     *
     * @return {@code true} if the modern pipeline is active.
     */
    boolean isModernPipelineActive();
}
