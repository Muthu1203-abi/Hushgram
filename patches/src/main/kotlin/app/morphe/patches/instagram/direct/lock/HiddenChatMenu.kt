/*
 * Copyright 2026 HushGram contributors
 * https://github.com/SysAdminDoc/HushGram
 */
package app.morphe.patches.instagram.direct.lock

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.string
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.instagram.direct.seen.THREAD_KEY
import app.morphe.patches.instagram.direct.seen.USER_SESSION
import app.morphe.patches.instagram.misc.extension.EXTENSION_PACKAGE
import app.morphe.patches.instagram.misc.extension.requireLocals
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val HIDDEN_CHAT_MENU = "$EXTENSION_PACKAGE/direct/HiddenChats;"
private const val MENU_OPEN = "$HIDDEN_CHAT_MENU->menuOpening(Ljava/util/List;Ljava/lang/Object;)Ljava/util/List;"
private const val MENU_ACTION = "$HIDDEN_CHAT_MENU->onMenuButton(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Z"
private const val MENU_ENUM = "MARK_AS_READ"

/** The enum whose constants Instagram uses for actions on one chat's long-press menu. */
private object HiddenChatMenuEnumFingerprint : Fingerprint(
    filters = listOf(string(MENU_ENUM), string("LEAVE"), string("NFB_SETTINGS")),
    custom = { method, classDef -> method.name == "<clinit>" && classDef.superclass == "Ljava/lang/Enum;" },
)

/** The labels object used by the inbox menu builder and its action handler. */
private object HiddenChatMenuLabelsFingerprint : Fingerprint(
    returnType = "[Ljava/lang/String;",
    filters = listOf(string("[DEBUG] Thread info")),
)

/** Builds the chat's menu from its button list. */
private object HiddenChatMenuOpenFingerprint : Fingerprint(
    classFingerprint = HiddenChatMenuLabelsFingerprint,
    returnType = "V",
    custom = { method, _ ->
        AccessFlags.STATIC.isSet(method.accessFlags) &&
            method.parameterTypes.firstOrNull()?.toString() == "Landroid/graphics/RectF;" &&
            method.parameterTypes.count { it.toString() == "Ljava/util/List;" } == 1
    },
)

/** Handles a tap on one chat menu button. */
private object HiddenChatMenuActionFingerprint : Fingerprint(
    classFingerprint = HiddenChatMenuLabelsFingerprint,
    filters = listOf(string("direct_inbox_action"), string("thread_unflag")),
)

private fun refuseHiddenMenu(reason: String): Nothing = throw PatchException("$LOCK_PATCH: $reason")

internal data class HiddenChatMenuTargets(
    val open: MutableMethod,
    val openList: Int,
    val openMenu: Int,
    val action: MutableMethod,
    val actionButton: Int,
    val actionKey: Int,
    val session: FieldReference,
)

/** Resolves Instagram's menu builder and action route before either is changed. */
internal fun BytecodePatchContext.findHiddenChatMenuTargets(): HiddenChatMenuTargets {
    val enum = HiddenChatMenuEnumFingerprint.classDef.type
    if (classDefByOrNull(enum) == null) refuseHiddenMenu("chat menu action enum is missing")

    val open = HiddenChatMenuOpenFingerprint.method
    val openList = open.parameterTypes.indexOfFirst { it.toString() == "Ljava/util/List;" }
    val openMenu = open.parameterTypes.indexOfFirst { it.toString() == HiddenChatMenuLabelsFingerprint.classDef.type }
    if (openList < 0 || openMenu < 0) refuseHiddenMenu("chat menu builder parameters changed")
    val mutableOpen = mutableClassDefBy(open.definingClass).methods.single {
        it.name == open.name && it.parameterTypes.map(CharSequence::toString) == open.parameterTypes.map(CharSequence::toString)
    }
    mutableOpen.requireLocals(LOCK_PATCH, 2)

    val action = HiddenChatMenuActionFingerprint.method
    val params = action.parameterTypes.map(CharSequence::toString)
    val actionButton = params.indexOf(enum)
    val actionKey = params.indexOf(THREAD_KEY)
    if (actionButton < 0 || params.count { it == enum } != 1 || actionKey < 0 || params.count { it == THREAD_KEY } != 1) {
        refuseHiddenMenu("chat menu action handler parameters changed")
    }
    if (AccessFlags.STATIC.isSet(action.accessFlags) || action.returnType != "V") {
        refuseHiddenMenu("chat menu action handler shape changed")
    }
    val actionClass = classDefByOrNull(action.definingClass)
        ?: refuseHiddenMenu("chat menu action handler class is missing")
    val session = actionClass.fields.filter {
        !AccessFlags.STATIC.isSet(it.accessFlags) && it.type == USER_SESSION
    }.singleOrNull() ?: refuseHiddenMenu("chat menu action handler no longer keeps its account session")
    val mutableAction = mutableClassDefBy(action.definingClass).methods.single {
        it.name == action.name && it.parameterTypes.map(CharSequence::toString) == params
    }
    mutableAction.requireLocals(LOCK_PATCH, 4)
    return HiddenChatMenuTargets(mutableOpen, openList, openMenu, mutableAction, actionButton, actionKey, session)
}

/** Routes hide/show entries through HushGram and leaves Instagram's other menu actions untouched. */
internal fun hookHiddenChatMenu(found: HiddenChatMenuTargets) {
    found.open.addInstructions(
        0,
        """
            move-object/from16 v0, p${found.openList}
            move-object/from16 v1, p${found.openMenu}
            invoke-static {v0, v1}, $MENU_OPEN
            move-result-object v0
            move-object/from16 p${found.openList}, v0
        """.trimIndent(),
    )
    found.action.addInstructionsWithLabels(
        0,
        """
            move-object/from16 v0, p0
            iget-object v1, v0, ${found.session}
            move-object/from16 v2, p${found.actionButton + 1}
            move-object/from16 v3, p${found.actionKey + 1}
            invoke-static {v1, v2, v3}, $MENU_ACTION
            move-result v0
            if-eqz v0, :instagram_menu
            return-void
        """.trimIndent(),
        ExternalLabel("instagram_menu", found.action.getInstruction(0)),
    )
}
