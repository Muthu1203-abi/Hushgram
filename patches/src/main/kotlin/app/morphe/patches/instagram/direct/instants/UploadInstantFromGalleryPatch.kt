/*
 * Copyright 2026 HushGram contributors
 * https://github.com/SysAdminDoc/HushGram
 * The QuickSnap gallery handoff follows InstaHacks (GPL-3.0): https://github.com/xdreizein666/InstaHacks
 */
package app.morphe.patches.instagram.direct.instants

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.instagram.misc.extension.EXTENSION_PACKAGE
import app.morphe.patches.instagram.misc.extension.enableStatus
import app.morphe.patches.instagram.misc.extension.instagramExtensionPatch
import app.morphe.patches.instagram.misc.extension.requireLocals
import app.morphe.patches.instagram.misc.extension.requireStatusMethod
import app.morphe.patches.instagram.misc.settings.settingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.Method

private const val PATCH = "Upload Instant from gallery"
private const val VM = "Lcom/instagram/quicksnap/camera/domain/QuickSnapCameraViewModel;"
private const val CONTEXT = "Landroid/content/Context;"
private const val BITMAP = "Landroid/graphics/Bitmap;"
private const val ACTIVITY = "Landroidx/fragment/app/FragmentActivity;"
private const val INTENT = "Landroid/content/Intent;"
private const val UPLOAD = "$EXTENSION_PACKAGE/direct/InstantUpload;"

val uploadInstantFromGalleryPatch = bytecodePatch(
    name = PATCH,
    description = "Choose a photo from your gallery and send it as an Instant from Instagram's camera.",
    default = true,
) {
    category("Messages")
    dependsOn(settingsPatch, instagramExtensionPatch)
    compatibleWith(*AppCompatibilities.instagram())

    execute {
        requireStatusMethod("instantUpload")
        val camera = mutableClassDefByOrNull(VM)
            ?: throw PatchException("$PATCH: Instagram's QuickSnap camera model was not found")
        val cameraMethods = camera.methods.filter {
            !AccessFlags.STATIC.isSet(it.accessFlags) && it.parameterTypes.isEmpty() &&
                it.returnType == "V" && it.implementation != null
        }
        if (cameraMethods.isEmpty()) throw PatchException("$PATCH: no QuickSnap camera lifecycle method was found")
        val handoffs = camera.methods.filter { method ->
            val parameters = method.parameterTypes.map(CharSequence::toString)
            if (!AccessFlags.STATIC.isSet(method.accessFlags) || method.returnType != "Ljava/lang/Object;" ||
                parameters.size < 4 || parameters[0] != CONTEXT || parameters[1] != BITMAP ||
                parameters.any { it == "Ljava/io/File;" } || parameters.count { it == VM } != 1
            ) return@filter false
            val bitmapCount = parameters.count { it == BITMAP }
            val legacy = parameters.size == 9 && bitmapCount == 2 && parameters[2] == BITMAP && parameters[3] == VM &&
                modelType(parameters[4]) && parameters[5] == "Ljava/lang/String;" &&
                parameters[6] == "Ljava/lang/String;" && modelType(parameters[7]) && parameters[8] == "J"
            val modern = bitmapCount == 1 && modelType(parameters[2]) && parameters[2] != VM
            legacy || modern
        }
        val handoff = handoffs.singleOrNull()
            ?: throw PatchException("$PATCH: expected one QuickSnap photo handoff, found ${handoffs.size}")
        val bitmapSlots = handoff.parameterTypes.withIndex().filter { it.index > 1 && it.value.toString() == BITMAP }
        handoff.requireLocals(PATCH, if (bitmapSlots.isEmpty()) 1 else 2)
        val ownerIndex = handoff.parameterTypes.indexOfFirst { it.toString() == VM }
        val owner = handoff.parameterRegister(ownerIndex)
        val duplicateInstructions = bitmapSlots.mapIndexed { ordinal, slot ->
            val register = handoff.parameterRegister(slot.index)
            val skip = "instant_upload_secondary_$ordinal"
            """
                if-eqz $register, :$skip
                invoke-static { v0 }, $UPLOAD->duplicate(Landroid/graphics/Bitmap;)Landroid/graphics/Bitmap;
                move-result-object v1
                move-object $register, v1
                :$skip
            """.trimIndent()
        }.joinToString("\n")
        handoff.addInstructionsWithLabels(
            0,
            """
                invoke-static { p0, p1, $owner }, $UPLOAD->takePending(Landroid/content/Context;Landroid/graphics/Bitmap;Ljava/lang/Object;)Landroid/graphics/Bitmap;
                move-result-object v0
                if-eqz v0, :instant_upload_continue
                move-object p1, v0
            """ + duplicateInstructions +
                "\n:instant_upload_continue",
            ExternalLabel("instant_upload_continue", handoff.getInstruction(0)),
        )

        val activity = mutableClassDefByOrNull(ACTIVITY)
            ?: throw PatchException("$PATCH: AndroidX FragmentActivity was not found")
        val results = activity.methods.filter {
            it.name == "onActivityResult" && it.returnType == "V" &&
                it.parameterTypes.map(CharSequence::toString) == listOf("I", "I", INTENT) && it.implementation != null
        }
        val result = results.singleOrNull()
            ?: throw PatchException("$PATCH: expected FragmentActivity.onActivityResult, found ${results.size}")
        cameraMethods.forEach { it.addInstruction(0, "invoke-static { }, $UPLOAD->cameraReady()V") }
        result.addInstruction(
            0,
            "invoke-static/range { p0 .. p3 }, $UPLOAD->onActivityResult(Landroid/app/Activity;IILandroid/content/Intent;)V",
        )
        enableStatus("instantUpload")
    }
}

private fun modelType(type: String): Boolean = type.startsWith("L") && type !in
    listOf(BITMAP, CONTEXT, VM, "Ljava/lang/Object;", "Ljava/io/File;", "Ljava/lang/String;")

private fun Method.parameterRegister(index: Int): String {
    var register = if (AccessFlags.STATIC.isSet(accessFlags)) 0 else 1
    for (i in 0 until index) {
        register += if (parameterTypes[i].toString() in listOf("J", "D")) 2 else 1
    }
    return "p$register"
}
