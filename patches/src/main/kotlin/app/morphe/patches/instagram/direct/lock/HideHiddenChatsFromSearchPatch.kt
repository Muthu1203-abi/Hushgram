/*
 * Copyright 2026 HushGram contributors
 * https://github.com/SysAdminDoc/HushGram
 */
package app.morphe.patches.instagram.direct.lock

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.instagram.explore.findSearchSuggestions
import app.morphe.patches.instagram.misc.extension.EXTENSION_PACKAGE
import app.morphe.patches.instagram.misc.extension.classesLoadingString
import app.morphe.patches.instagram.misc.extension.enableStatus
import app.morphe.patches.instagram.misc.extension.instagramExtensionPatch
import app.morphe.patches.instagram.misc.extension.loadsString
import app.morphe.patches.instagram.misc.extension.requireStatusMethod
import app.morphe.patches.instagram.misc.settings.settingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

private const val PATCH = "Hide hidden chats from search"
private const val SEARCH_USERS = "user_search_results"
private const val THREAD_RESULTS = "thread_search_results"
private const val SUGGESTED_USERS = "SUGGESTED_USERS"
private const val USER_TYPE = "Lcom/instagram/user/model/User;"
private const val EXTENSION = "$EXTENSION_PACKAGE/explore/SearchSuggestions;"
private const val FILTER_USERS = "$EXTENSION->hiddenChatUsers(Ljava/util/List;)Ljava/util/List;"
private const val USER_GETTER = "username"
private const val STRING = "Ljava/lang/String;"
private const val LIST = "Ljava/util/List;"

@Suppress("unused")
val hideHiddenChatsFromSearchPatch = bytecodePatch(
    name = PATCH,
    description = "Leaves accounts whose direct chats are hidden out of people search and the search screen's Suggested for you list.",
    default = true,
) {
    category("Messages")
    dependsOn(settingsPatch, instagramExtensionPatch, lockMessagesPatch)
    compatibleWith(*AppCompatibilities.instagram())

    execute {
        requireStatusMethod("hiddenChatSearch")
        val userNameGetter = findUsernameGetter()
        val searchUsers = findSearchUsersParser()
        val suggestions = findSearchSuggestions()
        val suggestionSite = findSuggestedUsersList(suggestions)

        searchUsers.method.addInstructions(
            searchUsers.index,
            """
                invoke-static/range { v${searchUsers.listRegister} .. v${searchUsers.listRegister} }, $FILTER_USERS
                move-result-object v${searchUsers.listRegister}
            """.trimIndent(),
        )

        val builder = mutableClassDefBy(suggestions.builderType).methods.single {
            it.name == suggestions.builderName && it.parameterTypes.map(CharSequence::toString) == suggestions.builderParameters
        }
        builder.addInstructions(
            suggestionSite.index + 1,
            """
                invoke-static/range { v${suggestionSite.register} .. v${suggestionSite.register} }, $FILTER_USERS
                move-result-object v${suggestionSite.register}
            """.trimIndent(),
        )

        val usernameStub = mutableClassDefBy(EXTENSION).methods.single {
            it.name == "username" && AccessFlags.STATIC.isSet(it.accessFlags) && it.returnType == STRING &&
                it.parameterTypes.map(CharSequence::toString) == listOf("Ljava/lang/Object;")
        }
        usernameStub.addInstructions(
            0,
            """
                if-eqz p0, :none
                check-cast p0, $USER_TYPE
                invoke-virtual { p0 }, $USER_TYPE->${userNameGetter.name}()${userNameGetter.returnType}
                move-result-object p0
                return-object p0
                :none
                const/4 p0, 0x0
                return-object p0
            """.trimIndent(),
        )
        enableStatus("hiddenChatSearch")
    }
}

private data class SearchUsersSite(val method: MutableMethod, val index: Int, val listRegister: Int)
private data class SuggestedUsersSite(val index: Int, val register: Int)

private fun BytecodePatchContext.findUsernameGetter(): Method {
    val user = classDefByOrNull(USER_TYPE) ?: refuse("Instagram's User model is missing")
    val getters = user.methods.filter {
        it.name == USER_GETTER && it.returnType == STRING && it.parameterTypes.isEmpty() &&
            AccessFlags.PUBLIC.isSet(it.accessFlags) && !AccessFlags.STATIC.isSet(it.accessFlags)
    }
    return getters.singleOrNull() ?: refuse("expected one public User.username() getter, found ${getters.size}")
}

/** Finds the exact list Instagram fills for the users/search endpoint, before it stores that list. */
private fun BytecodePatchContext.findSearchUsersParser(): SearchUsersSite {
    val methods = classesLoadingString(SEARCH_USERS).flatMap { owner ->
        owner.methods.filter { loadsString(it, SEARCH_USERS) && loadsString(it, THREAD_RESULTS) }
    }
    val parser = methods.singleOrNull() ?: refuse("expected one user search response parser, found ${methods.size}")
    val code = parser.implementation?.instructions?.toList() ?: refuse("user search response parser has no code")
    val usersAt = code.indices.singleOrNull { code.stringAt(it) == SEARCH_USERS }
        ?: refuse("user search response parser's users section moved")
    val threadsAt = code.indices.singleOrNull { it > usersAt && code.stringAt(it) == THREAD_RESULTS }
        ?: refuse("user search response parser's thread section moved")
    val writes = (usersAt + 1 until threadsAt).filter { at ->
        val instruction = code[at]
        if (instruction.opcode != Opcode.IPUT_OBJECT) return@filter false
        val field = (instruction as? ReferenceInstruction)?.reference as? FieldReference ?: return@filter false
        field.type == LIST
    }
    val write = writes.singleOrNull() ?: refuse("expected one user-results list write, found ${writes.size}")
    val register = (code[write] as TwoRegisterInstruction).registerA
    if (register > 255) refuse("the user-results list is in v$register, past v255")
    val mutable = mutableClassDefBy(parser.definingClass).methods.single {
        it.name == parser.name && it.parameterTypes.map(CharSequence::toString) == parser.parameterTypes.map(CharSequence::toString)
    }
    return SearchUsersSite(mutable, write, register)
}

/** Locates the suggested-users payload list inside the search screen's existing typed section builder. */
private fun BytecodePatchContext.findSuggestedUsersList(
    sites: app.morphe.patches.instagram.explore.SearchSuggestionSites,
): SuggestedUsersSite {
    val builder = mutableClassDefBy(sites.builderType).methods.single {
        it.name == sites.builderName && it.parameterTypes.map(CharSequence::toString) == sites.builderParameters
    }
    val code = builder.implementation?.instructions?.toList() ?: refuse("search suggestion builder has no code")
    val markers = code.indices.filter { code.stringAt(it) == SUGGESTED_USERS }
    val reads = markers.flatMap { marker ->
        (marker + 1 until minOf(code.size, marker + 18)).filter { at ->
            val instruction = code[at]
            if (instruction.opcode != Opcode.IGET_OBJECT) return@filter false
            val field = (instruction as? ReferenceInstruction)?.reference as? FieldReference ?: return@filter false
            field.type == LIST && classDefByOrNull(field.definingClass)?.fields?.any { it.type == STRING } == true
        }
    }.distinct()
    val read = reads.singleOrNull() ?: refuse("expected one suggested-account user list, found ${reads.size}")
    val register = (code[read] as TwoRegisterInstruction).registerA
    if (register > 255) refuse("the suggested-account user list is in v$register, past v255")
    return SuggestedUsersSite(read, register)
}

private fun List<com.android.tools.smali.dexlib2.iface.instruction.Instruction>.stringAt(index: Int): String? =
    ((this[index] as? ReferenceInstruction)?.reference as? StringReference)?.string

private fun BytecodePatchContext.refuse(reason: String): Nothing = throw PatchException("$PATCH: $reason")
