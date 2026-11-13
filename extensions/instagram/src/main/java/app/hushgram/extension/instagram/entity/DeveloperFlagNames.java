/*
 * Copyright 2026 HushGram contributors
 * https://github.com/SysAdminDoc/HushGram
 */
package app.hushgram.extension.instagram.entity;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import app.hushgram.extension.shared.Utils;

/** Import/export for the Rex-compatible recommended-flags JSON, used only to label exact IDs. */
public final class DeveloperFlagNames {
    private static final String PREFS = "hushgram_developer_flag_names";
    private static final String KEY = "json";
    private static final int MAX_BYTES = 4 * 1024 * 1024;
    private static volatile Map<Integer, String> configs = new HashMap<>();
    private static volatile Map<Long, String> parameters = new HashMap<>();

    private DeveloperFlagNames() { }

    public static String configName(int config) { return configs.get(config); }
    public static String parameterName(int config, int parameter) {
        return parameters.get(((long) config << 32) | (parameter & 0xffffffffL));
    }

    /** Validates all codes before replacing the saved map. Accepts Rex objects and Instagram ID-name rows. */
    public static synchronized int importJson(String source) throws Exception {
        if (source == null || source.getBytes("UTF-8").length > MAX_BYTES) throw new IllegalArgumentException("File too large");
        // JSON files saved by Windows tools may begin with a UTF-8 BOM. Android's JSONArray
        // parser does not consistently ignore it, so remove it before parsing and persisting.
        String json = source.startsWith("\uFEFF") ? source.substring(1) : source;
        JSONArray rows = new JSONArray(json);
        if (rows.length() > 10000) throw new IllegalArgumentException("Too many flags");
        Map<Integer, String> nextConfigs = new HashMap<>();
        Map<Long, String> nextParameters = new HashMap<>();
        JSONArray clean = new JSONArray();
        boolean idNameRows = rows.length() > 0 && rows.opt(0) instanceof String;
        for (int i = 0; i < rows.length(); i++) {
            if (idNameRows) {
                if (!(rows.opt(i) instanceof String)) throw new IllegalArgumentException("Mixed flag JSON formats");
                String raw = rows.optString(i, "");
                String[] fields = raw.split(":", -1);
                if (fields.length < 4 || fields.length % 2 != 0 || fields[1].trim().isEmpty()) {
                    throw new IllegalArgumentException("Invalid ID-name entry");
                }
                int config = Integer.parseInt(fields[0]);
                if (config < 0 || fields[1].length() > 256) throw new IllegalArgumentException("Invalid config ID");
                nextConfigs.putIfAbsent(config, fields[1].trim());
                for (int field = 2; field + 1 < fields.length; field += 2) {
                    int parameter = Integer.parseInt(fields[field]);
                    String name = fields[field + 1].trim();
                    if (parameter < 0 || name.isEmpty() || name.length() > 256) {
                        throw new IllegalArgumentException("Invalid parameter mapping");
                    }
                    nextParameters.putIfAbsent(((long) config << 32) | (parameter & 0xffffffffL), name);
                }
                continue;
            }
            JSONObject row = rows.optJSONObject(i);
            if (row == null) throw new IllegalArgumentException("Invalid flag entry");
            String name = row.optString("name", "").trim();
            String code = row.optString("code", "").trim();
            if (name.isEmpty() || name.length() > 256 || code.isEmpty() || code.length() > 256) throw new IllegalArgumentException("Invalid flag entry");
            JSONObject saved = new JSONObject().put("name", name).put("desc", row.optString("desc", "")).put("code", code);
            clean.put(saved);
            for (String part : code.split(",")) {
                String[] ids = part.trim().split("::", -1);
                if (ids.length == 1) {
                    int config = Integer.parseInt(ids[0]);
                    if (config < 0) throw new IllegalArgumentException("Invalid config ID");
                    nextConfigs.putIfAbsent(config, name);
                } else if (ids.length == 2) {
                    int config = Integer.parseInt(ids[0]);
                    int parameter = Integer.parseInt(ids[1]);
                    if (config < 0 || parameter < 0) throw new IllegalArgumentException("Invalid flag ID");
                    nextParameters.putIfAbsent(((long) config << 32) | (parameter & 0xffffffffL), name);
                } else throw new IllegalArgumentException("Invalid flag ID");
            }
        }
        Context context = Utils.getContext();
        if (context == null) throw new IllegalStateException("App context unavailable");
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String savedJson = idNameRows ? json : clean.toString();
        if (!prefs.edit().putString(KEY, savedJson).commit()) throw new IllegalStateException("Could not save flags");
        configs = nextConfigs;
        parameters = nextParameters;
        return rows.length();
    }

    public static String exportJson() {
        Context context = Utils.getContext();
        if (context != null) {
            String saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null);
            if (saved != null) return saved;
        }
        try (java.io.InputStream input = DeveloperFlagNames.class.getClassLoader()
                .getResourceAsStream("rex_recommended_flags_v3.json")) {
            if (input == null) return "[]";
            java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return output.toString("UTF-8");
        } catch (Exception ignored) {
            return "[]";
        }
    }

    /** Loads saved mappings once, lazily; malformed or older data is ignored. */
    public static synchronized void loadSaved() {
        if (!configs.isEmpty() || !parameters.isEmpty()) return;
        Context context = Utils.getContext();
        if (context == null) return;
        String source = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null);
        if (source == null || "[]".equals(source)) return;
        try { importJson(source); } catch (Exception ignored) { /* Keep active Instagram labels as fallback. */ }
    }
}
