/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 * https://github.com/crimera/piko/commit/04987953674f98b3f302dc7a8da7492ae96b5267
 *
 * Modified for HushGram (Instagram), 2026.
 * See NOTICE for GPLv3 §7(b) attribution terms.
 */
package app.hushgram.extension.instagram.entity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import app.hushgram.extension.shared.Logger;

/** Reads the active Quick Experiment list from Instagram's internal MobileConfig classes. */
public final class DeveloperOptions {
    private static final String QUICK_EXPERIMENT_CLASS = "X.Sb1";
    private static final String EXPERIMENT_ITEM_CLASS = "X.BSw";
    private static volatile ExperimentNames cachedNames;
    private static volatile ExperimentNames recommendedNames;

    private static final class ExperimentNames {
        final Map<Integer, String> configs;
        final Map<Long, String> parameters;

        ExperimentNames(Map<Integer, String> configs, Map<Long, String> parameters) {
            this.configs = configs;
            this.parameters = parameters;
        }
    }

    private static Class<?> getQuickExperimentHelperClass() throws ClassNotFoundException {
        return Class.forName(QUICK_EXPERIMENT_CLASS);
    }

    private static Class<?> getExperimentItemHelperClass() throws ClassNotFoundException {
        return Class.forName(EXPERIMENT_ITEM_CLASS);
    }

    private List<?> getAllExperiments() throws Exception {
        Class<?> helper = getQuickExperimentHelperClass();
        Method method = helper.getDeclaredMethod("A06");
        method.setAccessible(true);
        return (List<?>) method.invoke(null);
    }

    private DeveloperOptionsItem getDeveloperOptionsItem(Object experimentItem) throws Exception {
        Class<?> helper = getExperimentItemHelperClass();
        long specifier = ((Number) getField(helper, experimentItem, "A00")).longValue();
        String paramName = (String) getField(helper, experimentItem, "A01");
        String universalName = (String) getField(helper, experimentItem, "A02");
        return new DeveloperOptionsItem(specifier, universalName, paramName);
    }

    private static Object getField(Class<?> owner, Object instance, String name) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(instance);
    }

    /** Finds a Quick Experiment parameter name by the numeric IDs shown in MetaConfig. */
    public static String parameterName(int config, int index, String fallback) {
        if (config < 0 || index < 0) return fallback;
        String name = names().parameters.get(((long) config << 32) | (index & 0xffffffffL));
        if (name == null) {
            DeveloperFlagNames.loadSaved();
            name = DeveloperFlagNames.parameterName(config, index);
        }
        if (name == null) name = recommendedNames().parameters.get(((long) config << 32) | (index & 0xffffffffL));
        return name == null || name.trim().isEmpty() ? fallback : name;
    }

    /** Finds a Quick Experiment config name by the numeric ID shown in MetaConfig. */
    public static String configName(int config, String fallback) {
        if (config < 0) return fallback;
        String name = names().configs.get(config);
        if (name == null) {
            DeveloperFlagNames.loadSaved();
            name = DeveloperFlagNames.configName(config);
        }
        if (name == null) name = recommendedNames().configs.get(config);
        return name == null || name.trim().isEmpty() ? fallback : name;
    }

    /** Rex IG Lite's curated 453 recommendations are labels only; exact numeric ID matches are used. */
    private static ExperimentNames recommendedNames() {
        ExperimentNames ready = recommendedNames;
        if (ready != null) return ready;
        synchronized (DeveloperOptions.class) {
            ready = recommendedNames;
            if (ready != null) return ready;
            Map<Integer, String> configs = new HashMap<>();
            Map<Long, String> parameters = new HashMap<>();
            try (InputStream stream = DeveloperOptions.class.getClassLoader().getResourceAsStream("rex_recommended_flags_v3.json")) {
                if (stream != null) {
                    byte[] bytes = new byte[stream.available()];
                    int offset = 0;
                    while (offset < bytes.length) {
                        int count = stream.read(bytes, offset, bytes.length - offset);
                        if (count < 0) break;
                        offset += count;
                    }
                    JSONArray flags = new JSONArray(new String(bytes, 0, offset, StandardCharsets.UTF_8));
                    for (int i = 0; i < flags.length(); i++) {
                        JSONObject flag = flags.optJSONObject(i);
                        if (flag == null) continue;
                        String label = flag.optString("name", "").trim();
                        String codes = flag.optString("code", "");
                        if (label.isEmpty()) continue;
                        for (String code : codes.split(",")) {
                            String[] ids = code.trim().split("::", -1);
                            if (ids.length == 1) {
                                int config = Integer.parseInt(ids[0]);
                                if (config >= 0) configs.putIfAbsent(config, label);
                            } else if (ids.length == 2) {
                                int config = Integer.parseInt(ids[0]);
                                int index = Integer.parseInt(ids[1]);
                                if (config >= 0 && index >= 0) parameters.putIfAbsent(((long) config << 32) | (index & 0xffffffffL), label);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Logger.printException(() -> "Could not load recommended developer-option labels", e);
            }
            ready = new ExperimentNames(Collections.unmodifiableMap(configs), Collections.unmodifiableMap(parameters));
            recommendedNames = ready;
            return ready;
        }
    }

    private static ExperimentNames names() {
        ExperimentNames ready = cachedNames;
        if (ready != null) return ready;
        synchronized (DeveloperOptions.class) {
            ready = cachedNames;
            if (ready != null) return ready;
            Map<Integer, String> configs = new HashMap<>();
            Map<Long, String> parameters = new HashMap<>();
            try {
                DeveloperOptions reader = new DeveloperOptions();
                List<?> experiments = reader.getAllExperiments();
                if (experiments != null) {
                    for (Object experiment : experiments) {
                        DeveloperOptionsItem item = reader.getDeveloperOptionsItem(experiment);
                        int config = Integer.parseInt(item.getUniversalId());
                        int index = Integer.parseInt(item.getParamId());
                        if (config < 0 || index < 0) continue;
                        String configName = item.getUniversalName();
                        String parameterName = item.getParamName();
                        if (configName != null && !configName.trim().isEmpty()) configs.putIfAbsent(config, configName);
                        if (parameterName != null && !parameterName.trim().isEmpty()) {
                            parameters.putIfAbsent(((long) config << 32) | (index & 0xffffffffL), parameterName);
                        }
                    }
                }
            } catch (Exception e) {
                Logger.printException(() -> "Could not build Instagram developer-option names", e);
            }
            ready = new ExperimentNames(Collections.unmodifiableMap(configs), Collections.unmodifiableMap(parameters));
            cachedNames = ready;
            return ready;
        }
    }

    public JSONObject toJSONObject() {
        JSONObject mappings = new JSONObject();
        try {
            List<?> experiments = getAllExperiments();
            if (experiments == null) return mappings;
            for (Object item : experiments) {
                DeveloperOptionsItem option = getDeveloperOptionsItem(item);
                String universalId = option.getUniversalId();
                String paramId = option.getParamId();
                if (mappings.has(universalId)) {
                    mappings.accumulate(universalId, paramId);
                } else {
                    mappings.put(universalId, new JSONArray().put(paramId));
                }
            }
        } catch (Exception e) {
            Logger.printException(() -> "Could not extract Instagram developer option IDs", e);
        }
        return mappings;
    }

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder("All Experiments\n=====================\n\n");
        try {
            List<?> experiments = getAllExperiments();
            if (experiments == null) return output.toString();
            Set<String> seenUniversalNames = new HashSet<>();
            for (Object item : experiments) {
                DeveloperOptionsItem option = getDeveloperOptionsItem(item);
                String universalName = option.getUniversalName();
                if (seenUniversalNames.add(universalName)) {
                    output.append("---------------------\nUniversal name: ")
                            .append(universalName).append("\n\n");
                }
                output.append("Param name: ").append(option.getParamName()).append('\n')
                        .append("Hex id: ").append(Long.toHexString(option.getMobileConfigSpecifier())).append('\n')
                        .append("Config id: ").append(option.getConfigId()).append("\n\n");
            }
        } catch (Exception e) {
            Logger.printException(() -> "Could not list Instagram developer options", e);
            output.append("Could not read Instagram's experiment list: ")
                    .append(e.getClass().getSimpleName()).append('\n');
        }
        return output.toString();
    }
}
