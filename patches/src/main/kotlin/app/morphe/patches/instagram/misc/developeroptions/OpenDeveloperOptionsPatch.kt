/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 * Adapted for HushGram. See NOTICE for GPLv3 §7(b) attribution terms.
 */
package app.morphe.patches.instagram.misc.developeroptions

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.instagram.misc.extension.instagramExtensionPatch
import app.morphe.patches.instagram.misc.settings.settingsPatch
import app.morphe.util.findFreeRegister
import app.morphe.util.registersUsed
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * Opens Instagram's own developer options by long-pressing the Home tab. This only adds an entry
 * point to Instagram's existing screen; it does not force-enable remote flags or import presets.
 */
@Suppress("unused")
val openDeveloperOptionsPatch = bytecodePatch(
    name = "Open developer options",
    description = "Long-press the Home tab to open Instagram's developer options. Changes there can affect " +
        "Instagram's behavior for this account. Known active experiment names replace matching numeric labels. Starts off.",
    default = false,
) {
    category("Settings")
    dependsOn(settingsPatch, instagramExtensionPatch, developerOptionNamesPatch)
    compatibleWith(*AppCompatibilities.instagram())

    execute {
        // Label replacement is an enhancement to Instagram's own Developer Options screen. If a
        // build changes the schema row, keep the entry-point patch usable and leave its native
        // numeric labels alone rather than failing the whole patch.
        val flagNames = try {
            findFlagNameList()
        } catch (unavailable: PatchException) {
            //patchLog.warning("${unavailable.message}. Developer Options will still open; experiment labels keep Instagram's defaults.")
            patchLog.warning(
                "${unavailable.message}. Developer Options will still open, but imported flag names won't appear " +
                    "because this Instagram build's label hook wasn't installed.",
            )
            null
        }
        val listener = HomeIconOnClickListenerFingerprint.method
        val openerClass = DeveloperOptionCallWithCallableFingerprint.classDef
        val openerInstance = openerClass.fields.singleOrNull {
            AccessFlags.STATIC.isSet(it.accessFlags) && it.type == openerClass.type
        }
            ?: throw PatchException("Open developer options: Instagram's developer-options singleton field was not found")
        val openerParameters = listOf(
            "Landroid/content/Context;",
            "Landroidx/fragment/app/FragmentActivity;",
            "Lcom/instagram/common/session/UserSession;",
        )
        val openerMethod = openerClass.methods.singleOrNull {
            !AccessFlags.STATIC.isSet(it.accessFlags) && it.returnType == "V" &&
                it.parameterTypes.map { type -> type.toString() } == openerParameters
        }
            ?: throw PatchException("Open developer options: Instagram's developer-options opener method was not found")

        listener.apply {
            val activityRegister = getInstruction(0).registersUsed.firstOrNull()
                ?: throw PatchException("Open developer options: Home long-click activity register was not found")
            val sessionRegister = getInstruction(1).registersUsed.firstOrNull()
                ?: throw PatchException("Open developer options: Home long-click session register was not found")
            val scratch = findFreeRegister(2)

            addInstructionsWithLabels(
                2,
                """
                    sget-object v$scratch, $openerInstance
                    invoke-virtual {v$scratch, v$activityRegister, v$activityRegister, v$sessionRegister}, $openerMethod
                    const/4 v$scratch, 0x1
                    return v$scratch
                """.trimIndent(),
            )
        }
        if (flagNames != null) applyFlagNameList(flagNames)
    }
}
