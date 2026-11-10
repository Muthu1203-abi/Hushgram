/*
 * Copyright 2026 HushGram contributors
 * https://github.com/SysAdminDoc/HushGram
 */
package app.morphe.patches.instagram.media.quality

import app.morphe.patcher.StringComparisonType
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.instagram.misc.extension.enableStatus
import app.morphe.patches.instagram.misc.extension.instagramExtensionPatch
import app.morphe.patches.instagram.misc.extension.requireStatusMethod
import app.morphe.patches.instagram.misc.settings.settingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.Method

private const val FORMAT_CHANGE = "HeroServicePlayer.onDownstreamFormatChange"
private const val QUALITY_INDICATOR = "Lapp/hushgram/extension/instagram/media/QualityIndicator;->onFormat(Ljava/lang/Object;)V"

/** Reports the actual selected playback format to a small, temporary on-screen quality badge. */
@Suppress("unused")
val mediaQualityIndicatorPatch = bytecodePatch(
    name = "Media quality indicator",
    description = "Briefly shows the actual resolution of a feed video or Reel when Instagram changes its stream quality. " +
        "Turn it on in HushGram settings > Playback.",
    default = true,
) {
    category("Playback")
    dependsOn(settingsPatch)
    compatibleWith(*AppCompatibilities.instagram())
    dependsOn(instagramExtensionPatch)

    execute {
        requireStatusMethod("mediaQualityIndicator")
        val callbacks = classDefByStrings(FORMAT_CHANGE, StringComparisonType.EQUALS)
            .flatMap { owner -> owner.methods.filter { it.holds(FORMAT_CHANGE) } }
            .filter { it.returnType == "V" && it.parameterTypes.size == 1 && !AccessFlags.STATIC.isSet(it.accessFlags) }
        val callback = callbacks.singleOrNull()
            ?: throw PatchException("Media quality indicator: expected one downstream video-format callback, found ${callbacks.size}")
        val type = callback.parameterTypes.single().toString()
        if (classDefByOrNull(type) == null) {
            throw PatchException("Media quality indicator: callback format class $type is missing")
        }
        val mutable = mutable(callback)
        mutable.addInstructions(0, "invoke-static { p1 }, $QUALITY_INDICATOR")
        enableStatus("mediaQualityIndicator")
    }
}

private fun Method.holds(string: String): Boolean = implementation?.instructions?.any { instruction ->
    val reference = (instruction as? com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction)
        ?.reference as? com.android.tools.smali.dexlib2.iface.reference.StringReference
    reference?.string == string
} == true

private fun app.morphe.patcher.patch.BytecodePatchContext.mutable(method: Method) =
    mutableClassDefBy(method.definingClass).methods.single {
        it.name == method.name && it.returnType == method.returnType &&
            it.parameterTypes.map(CharSequence::toString) == method.parameterTypes.map(CharSequence::toString)
    }
