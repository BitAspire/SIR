package me.croabeast.sir.plugin.hook;

import me.croabeast.sir.plugin.SIRPlugin;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.CustomChart;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.SimplePie;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public final class MetricsLoader {

    private static MetricsLoader defaultLoader = null;
    private final Metrics metrics;

    private MetricsLoader(SIRPlugin plugin) {
        this.metrics = new Metrics(plugin, 12806);
    }

    public MetricsLoader addChart(CustomChart chart) {
        metrics.addCustomChart(chart);
        return this;
    }

    public MetricsLoader addSimplePie(String id, Object value) {
        return addChart(new SimplePie(id, value::toString));
    }

    public MetricsLoader addDrillDownPie(String id, String title, Object value, String def) {
        return addChart(new DrilldownPie(id, () -> {
            Map<String, Map<String, Integer>> map = new HashMap<>();
            Map<String, Integer> entry = new HashMap<>();

            entry.put(title, 1);
            map.put(value == null ? def : value.toString(), entry);

            return map;
        }));
    }

    @NotNull
    public static MetricsLoader initialize(SIRPlugin plugin) {
        return defaultLoader == null ? (defaultLoader = new MetricsLoader(plugin)) : defaultLoader;
    }
}
