/*
 * Copyright (C) 2026 rex <https://github.com/xboyrex/rex-ig-lite>
 * Adapted for HushGram. See NOTICE for GPLv3 §7(b) attribution terms.
 */
package app.morphe.patches.instagram.misc.developeroptions

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.instagram.misc.extension.EXTENSION_PACKAGE
import app.morphe.patches.instagram.misc.extension.instagramExtensionPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.AccessFlags

private const val OVERRIDES = "$EXTENSION_PACKAGE/entity/DeveloperFlagOverrides;"

/** The MobileConfig reader class contains its typed getters and this stable fallback marker. */
internal object MobileConfigReadFingerprint : Fingerprint(
    strings = listOf("__fbt_null__"),
    returnType = "Ljava/lang/String;",
)

/** Optional runtime bridge for imported, explicitly typed Developer Options values. */
@Suppress("unused")
val developerFlagOverridesPatch = bytecodePatch(
    name = "Import/export developer flag values",
    description = "Apply imported bool and long Developer Options values by exact MobileConfig ID. Starts off.",
    default = false,
) {
    category("Settings")
    dependsOn(instagramExtensionPatch, developerOptionNamesPatch)
    compatibleWith(*AppCompatibilities.instagram())

    execute {
        MobileConfigReadFingerprint.apply {
            val methods = classDef.methods
            val sourceType = parameters[0].type
            val booleanReaders = methods.filter {
                it.returnType == "Z" && it.parameters.size == 3 && it.parameters[0].type == sourceType
            }
            val longReaders = methods.filter {
                it.returnType == "J" && it.parameters.size == 3 && it.parameters[0].type == sourceType
            }
            if (booleanReaders.size != 1 || longReaders.size != 1) {
                throw PatchException("Developer flag values: expected one boolean and one long MobileConfig reader")
            }
            booleanReaders.single().apply {
                val isStatic = AccessFlags.STATIC.isSet(accessFlags)
                var id = parameters.indexOfFirst { it.type == "J" }
                if (id < 0) throw PatchException("Developer flag values: boolean reader has no config ID")
                if (!isStatic) id++
                addInstructions(0, """
                    invoke-static/range { p$id .. p${id + 1} }, $OVERRIDES->booleanValue(J)Ljava/lang/Boolean;
                    move-result-object v0
                    if-eqz v0, :hushgram_original_bool
                    invoke-virtual {v0}, Ljava/lang/Boolean;->booleanValue()Z
                    move-result v1
                    return v1
                    :hushgram_original_bool
                    nop
                """.trimIndent())
            }
            longReaders.single().apply {
                val isStatic = AccessFlags.STATIC.isSet(accessFlags)
                var id = parameters.indexOfFirst { it.type == "J" }
                if (id < 0) throw PatchException("Developer flag values: long reader has no config ID")
                if (!isStatic) id++
                addInstructions(0, """
                    invoke-static/range { p$id .. p${id + 1} }, $OVERRIDES->longValue(J)Ljava/lang/Long;
                    move-result-object v0
                    if-eqz v0, :hushgram_original_long
                    invoke-virtual {v0}, Ljava/lang/Long;->longValue()J
                    move-result-wide v1
                    return-wide v1
                    :hushgram_original_long
                    nop
                """.trimIndent())
            }
        }
    }
}
