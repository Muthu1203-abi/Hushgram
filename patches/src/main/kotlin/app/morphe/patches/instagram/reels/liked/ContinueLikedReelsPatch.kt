/*
 * Copyright 2026 HushGram contributors
 * https://github.com/SysAdminDoc/HushGram
 */
package app.morphe.patches.instagram.reels.liked

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.instagram.misc.extension.EXTENSION_PACKAGE
import app.morphe.patches.instagram.misc.extension.classesAccessing
import app.morphe.patches.instagram.misc.extension.enableStatus
import app.morphe.patches.instagram.misc.extension.instagramExtensionPatch
import app.morphe.patches.instagram.misc.extension.requireStatusMethod
import app.morphe.patches.instagram.misc.settings.settingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

private const val PATCH = "Continue through liked Reels"
private const val VIEWER_SOURCE = "Lcom/instagram/clips/intf/ClipsViewerSource;"
private const val CLIPS_VIEWER_CONFIG = "Lcom/instagram/clips/intf/ClipsViewerConfig;"
private const val USER_SESSION = "Lcom/instagram/common/session/UserSession;"
private const val LIKED_SOURCE_HELPER = "$EXTENSION_PACKAGE/reels/LikedReelsScroll;->source(Ljava/lang/Object;)Ljava/lang/Object;"

private data class LikedReelsSite(val owner: String, val name: String, val parameters: List<String>, val sourceRead: Int, val sourceField: FieldReference)

@Suppress("unused")
val continueLikedReelsPatch = bytecodePatch(
    name = PATCH,
    description = "Uses Instagram's liked-Reels source when opening a Reel from Your activity > Likes, so swiping " +
        "continues through that liked feed.",
) {
    category("Reels")
    dependsOn(settingsPatch, instagramExtensionPatch)
    compatibleWith(*AppCompatibilities.instagram())

    execute {
        requireStatusMethod("likedReelsScroll")
        val site = findLikedReelsSite()
        hookLikedReelsSource(site)
        enableStatus("likedReelsScroll")
    }
}

private fun refuse(detail: String): Nothing = throw PatchException("$PATCH: $detail")

/** Finds the FEED_LIKED source field from Instagram's enum initializer and the viewer launch using it. */
private fun BytecodePatchContext.findLikedReelsSite(): LikedReelsSite {
    val sourceClass = classDefBy(VIEWER_SOURCE)
    val initializer = sourceClass.methods.singleOrNull { it.name == "<clinit>" }
        ?: refuse("$VIEWER_SOURCE has no unique enum initializer")
    val initCode = initializer.implementation?.instructions?.toList()
        ?: refuse("$VIEWER_SOURCE enum initializer has no code")
    val feedLikedString = initCode.indexOfFirst { instruction -> stringAt(instruction) == "feed_liked" }
        ?: refuse("$VIEWER_SOURCE no longer names the feed_liked source")
    val sourceWrite = (feedLikedString + 1 until initCode.size).firstOrNull { index ->
        val instruction = initCode[index]
        instruction.opcode == Opcode.SPUT_OBJECT && (instruction as? ReferenceInstruction)?.reference.let { ref ->
            ref is FieldReference && ref.definingClass == VIEWER_SOURCE && ref.type == VIEWER_SOURCE
        }
    } ?: refuse("could not find the static field for feed_liked")
    val sourceField = (initCode[sourceWrite] as ReferenceInstruction).reference as FieldReference

    val candidates = classesAccessing(VIEWER_SOURCE, sourceField.name, Opcode.SGET_OBJECT)
        .flatMap { it.methods }
        .mapNotNull { method ->
            val code = method.implementation?.instructions?.toList() ?: return@mapNotNull null
            val reads = code.indices.filter { index ->
                code[index].opcode == Opcode.SGET_OBJECT &&
                    (code[index] as? ReferenceInstruction)?.reference == sourceField
            }
            if (reads.size != 1 || !code.any { stringAt(it) == "clips" }) return@mapNotNull null
            val opensClipsViewer = code.any { instruction ->
                val called = (instruction as? ReferenceInstruction)?.reference as? MethodReference ?: return@any false
                called.name == "<init>" && called.parameterTypes.map(CharSequence::toString) == listOf(VIEWER_SOURCE, USER_SESSION)
            } && code.any { instruction ->
                val called = (instruction as? ReferenceInstruction)?.reference as? MethodReference ?: return@any false
                called.parameterTypes.map(CharSequence::toString) ==
                    listOf("Landroidx/fragment/app/FragmentActivity;", CLIPS_VIEWER_CONFIG, USER_SESSION)
            }
            if (!opensClipsViewer) return@mapNotNull null
            LikedReelsSite(method.definingClass, method.name, method.parameterTypes.map(CharSequence::toString), reads.single(), sourceField)
        }
    return candidates.singleOrNull()
        ?: refuse("expected one liked-feed clip viewer launch, found ${candidates.size}")
}

private fun stringAt(instruction: com.android.tools.smali.dexlib2.iface.instruction.Instruction): String? {
    if (instruction.opcode != Opcode.CONST_STRING && instruction.opcode != Opcode.CONST_STRING_JUMBO) return null
    return ((instruction as? ReferenceInstruction)?.reference as? StringReference)?.string
}

/** Changes only the source read used to open a clip from the Likes feed. */
private fun BytecodePatchContext.hookLikedReelsSource(site: LikedReelsSite) {
    val method = mutableClassDefBy(site.owner).methods.singleOrNull {
        it.name == site.name && it.parameterTypes.map(CharSequence::toString) == site.parameters
    } ?: refuse("${site.owner}->${site.name} moved after discovery")
    val code = method.implementation?.instructions?.toList()
        ?: refuse("${site.owner}->${site.name} has no code")
    val instruction = code.getOrNull(site.sourceRead) as? OneRegisterInstruction
        ?: refuse("the liked-feed source read no longer has a result register")
    method.addInstructions(
        site.sourceRead + 1,
        """
            invoke-static/range { v${instruction.registerA} .. v${instruction.registerA} }, $LIKED_SOURCE_HELPER
            move-result-object v${instruction.registerA}
        """.trimIndent(),
    )
}
