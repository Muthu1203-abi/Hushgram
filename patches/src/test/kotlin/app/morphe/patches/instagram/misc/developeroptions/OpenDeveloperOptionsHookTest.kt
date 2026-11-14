package app.morphe.patches.instagram.misc.developeroptions

import app.morphe.patches.instagram.FixtureDex
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.Opcodes
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer

class OpenDeveloperOptionsHookTest {
    @Test
    fun dumpHomeIconListener() {
        val dexDir = File("build/hushgram-454-dex")
        if (!dexDir.exists()) {
            println("dexDir does not exist")
            return
        }
        val dexFiles = dexDir.listFiles { _, name -> name.endsWith(".dex") } ?: return
        for (f in dexFiles) {
            val dex = DexBackedDexFile(Opcodes.getDefault(), ByteBuffer.wrap(f.readBytes()))
            for (cls in dex.classes) {
                for (m in cls.methods) {
                    if (m.name == "onLongClick") {
                        val strings = m.implementation?.instructions?.mapNotNull {
                            (it as? com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction)?.reference as? com.android.tools.smali.dexlib2.iface.reference.StringReference
                        }?.map { it.string } ?: emptyList()
                        if (strings.contains("click") && strings.contains("activity")) {
                            println("Found onLongClick in class: ${cls.type} from file ${f.name}")
                            println("Registers: ${m.implementation?.registerCount}")
                            m.implementation?.instructions?.forEachIndexed { index, inst ->
                                println("  [$index] ${inst.opcode} ${inst.javaClass.simpleName}")
                                if (inst is com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction) {
                                    println("       ref: ${inst.reference}")
                                }
                                if (inst is com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction) {
                                    println("       regA: v${inst.registerA}")
                                }
                                if (inst is com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction) {
                                    println("       regA: v${inst.registerA}, regB: v${inst.registerB}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
