package app.morphe.patches.instagram.misc.developeroptions

import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File

class OpenDeveloperOptionsHookTest {
    @Test
    fun verifiesFragmentNavigatorMethods() {
        val dexDir = File("build/hushgram-454-dex")
        if (!dexDir.exists()) return
        val dexFiles = dexDir.listFiles { _, name -> name.endsWith(".dex") } ?: return
        var navClass: com.android.tools.smali.dexlib2.iface.ClassDef? = null
        for (f in dexFiles) {
            val dex = DexFileFactory.loadDexFile(f, Opcodes.getDefault())
            for (cls in dex.classes) {
                if (cls.type == "LX/08Sa;") {
                    navClass = cls
                    break
                }
            }
            if (navClass != null) break
        }
        assertNotNull("LX/08Sa; should be found", navClass)
        val fragmentClass = navClass!!

        val fragmentConstructor = fragmentClass.methods.firstOrNull { method ->
            method.name == "<init>" && method.parameterTypes.size == 2 &&
                method.parameterTypes[0].toString() == "Landroidx/fragment/app/FragmentActivity;"
        }
        assertNotNull("Constructor should be found", fragmentConstructor)
        assertEquals("LX/01yx;", fragmentConstructor!!.parameterTypes[1].toString())

        val fragmentSetMethod = fragmentClass.methods.firstOrNull { method ->
            !AccessFlags.STATIC.isSet(method.accessFlags) && method.returnType == "V" &&
                method.parameterTypes.map(Any::toString) == listOf("Landroidx/fragment/app/Fragment;") &&
                method.implementation?.instructions?.any { inst ->
                    ((inst as? ReferenceInstruction)?.reference as? MethodReference)?.let { mr ->
                        mr.parameterTypes.map(Any::toString) == listOf("Landroid/os/Bundle;", "Landroidx/fragment/app/Fragment;")
                    } ?: false
                } == true
        }
        assertNotNull("SetFragment method should be found", fragmentSetMethod)
        assertEquals("A0F", fragmentSetMethod!!.name)

        val fragmentCallMethod = fragmentClass.methods.lastOrNull { method ->
            !AccessFlags.STATIC.isSet(method.accessFlags) && method.returnType == "V" &&
                method.parameterTypes.isEmpty() &&
                method.implementation?.instructions?.any { it.opcode.name == "SGET_OBJECT" } == true &&
                method.implementation?.instructions?.any { inst ->
                    ((inst as? ReferenceInstruction)?.reference as? MethodReference)?.let { mr ->
                        mr.parameterTypes.map(Any::toString) == listOf("Ljava/lang/Integer;")
                    } ?: false
                } == true
        }
        assertNotNull("Commit method should be found", fragmentCallMethod)
        assertEquals("A06", fragmentCallMethod!!.name)
    }
}
