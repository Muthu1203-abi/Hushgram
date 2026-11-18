/*
 * Copyright 2026 HushGram contributors
 * https://github.com/SysAdminDoc/HushGram
 */
package app.hushgram.extension.instagram.misc;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import app.hushgram.extension.instagram.entity.DeveloperFlagNames;
import app.hushgram.extension.shared.SettingsContextRule;

@RunWith(RobolectricTestRunner.class)
public class DeveloperFlagNamesTest {
    @Rule public final SettingsContextRule settings = new SettingsContextRule();

    @Test
    public void importsInstagramIdNameRowsForBothConfigAndParameterLabels() throws Exception {
        assertEquals(1, DeveloperFlagNames.importJson(
                "[\"987654:sample_config:0:first_parameter:2:third_parameter\"]"));

        assertEquals("sample_config", DeveloperFlagNames.configName(987654));
        assertEquals("first_parameter", DeveloperFlagNames.parameterName(987654, 0));
        assertEquals("third_parameter", DeveloperFlagNames.parameterName(987654, 2));
        assertEquals("sample_config", app.hushgram.extension.instagram.entity.DeveloperOptions.configName(987654, "_987654"));
        assertEquals("first_parameter", app.hushgram.extension.instagram.entity.DeveloperOptions.parameterName(987654, 0, "_0"));
    }

    @Test
    public void importsUtf8BomIdNameFilesAndExportsCleanJson() throws Exception {
        assertEquals(1, DeveloperFlagNames.importJson(
                "\uFEFF[\"987655:sample_config:0:first_parameter\"]"));

        assertEquals("sample_config", DeveloperFlagNames.configName(987655));
        assertEquals("first_parameter", DeveloperFlagNames.parameterName(987655, 0));
        assertEquals("sample_config", app.hushgram.extension.instagram.entity.DeveloperOptions.configName(987655, "_987655"));
        assertEquals("first_parameter", app.hushgram.extension.instagram.entity.DeveloperOptions.parameterName(987655, 0, "_0"));
        assertEquals("[\"987655:sample_config:0:first_parameter\"]", DeveloperFlagNames.exportJson());
    }

    @Test
    public void usesBundledInstagramIdNameMappingForDeveloperOptionsLabels() {
        assertEquals("ig4a_profile_message_button_fix",
                app.hushgram.extension.instagram.entity.DeveloperOptions.configName(153711, "153711"));
        assertEquals("use_is_ai_agent_check",
                app.hushgram.extension.instagram.entity.DeveloperOptions.parameterName(153711, 0, "0"));
    }
}
