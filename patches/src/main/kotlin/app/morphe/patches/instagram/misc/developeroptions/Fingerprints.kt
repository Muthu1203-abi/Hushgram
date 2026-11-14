/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 * Adapted for HushGram. See NOTICE for GPLv3 §7(b) attribution terms.
 */
package app.morphe.patches.instagram.misc.developeroptions

import app.morphe.patcher.Fingerprint
import app.morphe.util.registersUsed
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

/** Instagram's home-tab long-click handler. Keep its identifying strings together and require one match. */
internal object HomeIconOnClickListenerFingerprint : Fingerprint(
    name = "onLongClick",
    strings = listOf("click", "activity"),
)

/** FragmentNavigator transition implementation method. */
internal object FragmentNavigatorTransitionFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Ljava/lang/Integer;"),
    strings = listOf("FragmentNavigator.transitionInternal", "FragmentNavigator", "Fragment is null when attempting transition"),
)

/** The internal developer-options opener, identified by its error toast key. */
internal object DeveloperOptionCallWithCallableFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.STATIC, AccessFlags.FINAL, AccessFlags.PUBLIC),
    strings = listOf("debug_options_error"),
)

/** The helper that decodes a Quick Experiment MobileConfig specifier into its config ID. */
internal object ExperimentsGetMobileConfigSpecifierFingerprint : Fingerprint(
    strings = listOf("ExperimentParameter"),
    returnType = "I",
    parameters = emptyList(),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    custom = { method, _ ->
        val instructions = method.implementation?.instructions?.take(4).orEmpty()
        val field = (instructions.getOrNull(0) as? ReferenceInstruction)?.reference as? FieldReference
        val converter = (instructions.getOrNull(1) as? ReferenceInstruction)?.reference as? MethodReference
        val registers = instructions.getOrNull(0)?.registersUsed.orEmpty()
        !AccessFlags.STATIC.isSet(method.accessFlags) &&
            instructions.map { it.opcode } == listOf(Opcode.IGET_WIDE, Opcode.INVOKE_STATIC, Opcode.MOVE_RESULT, Opcode.RETURN) &&
            field?.definingClass == method.definingClass && field.type == "J" &&
            converter?.parameterTypes?.map(Any::toString) == listOf("J") && converter.returnType == "I" &&
            registers.size == 2 && instructions[1].registersUsed == listOf(registers[0], registers[0] + 1)
    },
)

/** The builder creating the Quick Experiments registry and its obfuscated class name. */
internal object ExperimentsValueBuilderFingerprint : Fingerprint(
    strings = listOf("default[after mc dispose]", "default[before mc init]", "override", "server"),
)
