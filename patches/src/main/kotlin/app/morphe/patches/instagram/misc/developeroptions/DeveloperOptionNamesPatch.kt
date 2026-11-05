/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 * Adapted for HushGram. See NOTICE for GPLv3 §7(b) attribution terms.
 */
package app.morphe.patches.instagram.misc.developeroptions

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.instagram.misc.extension.EXTENSION_PACKAGE
import app.morphe.patches.instagram.misc.extension.instagramExtensionPatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

private const val ENTITY_OPTIONS = "$EXTENSION_PACKAGE/entity/DeveloperOptions;"
private const val ENTITY_ITEM = "$EXTENSION_PACKAGE/entity/DeveloperOptionsItem;"

/** Resolve Piko-derived reflection anchors against the Instagram APK being patched. */
internal val developerOptionNamesPatch = bytecodePatch {
    dependsOn(instagramExtensionPatch)

    execute {
        ExperimentsGetMobileConfigSpecifierFingerprint.matchAll(1..1)
        val schemaMethod = ExperimentsGetMobileConfigSpecifierFingerprint.method
        val universalIdCall = schemaMethod.implementation!!.instructions.mapNotNull { instruction ->
            if (instruction.opcode != Opcode.INVOKE_STATIC && instruction.opcode != Opcode.INVOKE_STATIC_RANGE) return@mapNotNull null
            ((instruction as? ReferenceInstruction)?.reference as? MethodReference)?.takeIf {
                it.parameterTypes.map(Any::toString) == listOf("J") && it.returnType == "I"
            }
        }.singleOrNull() ?: throw PatchException("Developer option names: the MobileConfig ID converter is missing or ambiguous")

        val quickExperiments = ExperimentsValueBuilderFingerprint.classDef
        val getAllExperiments = quickExperiments.methods.firstOrNull { it.returnType == "Ljava/util/List;" }
            ?: throw PatchException("Developer option names: Instagram's Quick Experiments list getter was not found")

        val options = mutableClassDefBy(ENTITY_OPTIONS)
        options.methods.single { it.name == "getQuickExperimentHelperClass" }
            .replaceString("X.Sb1", quickExperiments.type.binaryClassName())
        options.methods.single { it.name == "getAllExperiments" }
            .replaceString("A06", getAllExperiments.name)
        options.methods.single { it.name == "getExperimentItemHelperClass" }
            .replaceString("X.BSw", schemaMethod.definingClass.binaryClassName())

        val item = mutableClassDefBy(ENTITY_ITEM)
        item.methods.single { it.name == "getUniversalIdHelperClass" }
            .replaceString("X.0B3D", universalIdCall.definingClass.binaryClassName())
    }
}

private fun String.binaryClassName(): String = removePrefix("L").removeSuffix(";").replace('/', '.')

private fun MutableMethod.replaceString(previous: String, replacement: String) {
    val matches = implementation!!.instructions.withIndex().filter { (_, instruction) -> instruction.string() == previous }
    val match = matches.singleOrNull()
        ?: throw PatchException("Developer option names: expected one '$previous' string in $definingClass->$name, found ${matches.size}")
    val register = (match.value as? OneRegisterInstruction)?.registerA
        ?: throw PatchException("Developer option names: '$previous' has no destination register in $definingClass->$name")
    val opcode = if (match.value.opcode == Opcode.CONST_STRING_JUMBO) "const-string/jumbo" else "const-string"
    replaceInstruction(match.index, "$opcode v$register, \"$replacement\"")
}

private fun com.android.tools.smali.dexlib2.iface.instruction.Instruction.string(): String? =
    if (opcode != Opcode.CONST_STRING && opcode != Opcode.CONST_STRING_JUMBO) null
    else ((this as? ReferenceInstruction)?.reference as? StringReference)?.string
