package com.bitaspire.sir;

import com.bitaspire.sir.chat.ChatProcessor;
import com.bitaspire.sir.chat.ModernChatPipeline;
import com.bitaspire.sir.module.SIRModule;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Default implementation of {@link com.bitaspire.sir.chat.ProcessorManager}.
 *
 * <p>Maintains a thread-safe list of runtime processors and combines them with
 * module processors sourced from the {@link SIRApi} on each query. The API
 * reference is resolved lazily so this manager can be instantiated before the
 * module manager is available.
 */
@RequiredArgsConstructor
final class ProcessorManagerImpl implements com.bitaspire.sir.chat.ProcessorManager {

    private final SIRApi api;
    private final List<ChatProcessor> runtimeProcessors = new CopyOnWriteArrayList<>();

    @Override
    public void register(@NotNull ChatProcessor processor) {
        if (!runtimeProcessors.contains(processor))
            runtimeProcessors.add(processor);
    }

    @Override
    public void unregister(@NotNull ChatProcessor processor) {
        runtimeProcessors.remove(processor);
    }

    @NotNull
    public List<ChatProcessor> getProcessors(boolean runtime) {
        List<ChatProcessor> processors = runtime
                ? new ArrayList<>(runtimeProcessors)
                : new ArrayList<>();

        for (SIRModule module : api.getModuleManager().getModules()) {
            if (!module.isEnabled() || !module.isRegistered() || !(module instanceof ChatProcessor))
                continue;

            processors.add((ChatProcessor) module);
        }

        processors.sort(Comparator.comparingInt(ChatProcessor::getPriority));
        return processors;
    }

    @Override
    public boolean isModernPipelineActive() {
        SIRModule module = api.getModuleManager().getModule("Channels");
        return module instanceof ModernChatPipeline && ((ModernChatPipeline) module).isActive();
    }
}
