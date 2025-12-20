package me.croabeast.sir.module.announcement;

import lombok.Getter;
import me.croabeast.scheduler.GlobalTask;
import me.croabeast.sir.ExtensionFile;
import me.croabeast.sir.command.CommandProvider;
import me.croabeast.sir.command.SIRCommand;
import me.croabeast.sir.module.SIRModule;
import me.croabeast.takion.logger.LogLevel;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class Announcements extends SIRModule implements CommandProvider {

    @Getter
    private final Set<SIRCommand> commands = new HashSet<>();

    Config config;
    Data data;

    private GlobalTask task;
    private int count = 0;

    private void start() {
        if (!isEnabled() || config.getInterval() <= 0 || task != null)
            return;

        task = getApi().getScheduler().runTaskTimer(
                () -> {
                    int size = data.getAnnouncements().size() - 1;
                    if (count > size) count = 0;

                    Announcement unit = data.fromIndex(count);
                    if (unit != null) unit.announce();

                    if (config.isRandom()) {
                        count = new Random().nextInt(size + 1);
                        return;
                    }

                    count = count < size ? (count + 1) : 0;
                },
                0L, config.getInterval()
        );
    }

    private void stop() {
        if (isEnabled() || task == null)
            return;

        task.cancel();
        task = null;
    }

    public boolean isRunning() {
        return task != null && task.isRunning();
    }

    @Override
    public boolean register() {
        config = new Config(this);
        data = new Data(this);

        try {
            commands.add(new Command(this, new ExtensionFile(this, "lang", true)));
        } catch (Exception e) {
            getLogger().log(LogLevel.ERROR, "Command 'announce' cannot be loaded due to lang.yml was missing");
            e.printStackTrace();
        }

        start();
        return true;
    }

    @Override
    public boolean unregister() {
        stop();
        return true;
    }

    public void announce(String id) {
        Announcement announcement = data.getAnnouncements().get(id);
        if (announcement != null) announcement.announce();
    }
}
