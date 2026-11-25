package app.morphe.patches.instagram.layout.liquidglass

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.intOption
import app.morphe.patcher.patch.PatchException
import app.morphe.patches.instagram.misc.extension.requireStatusMethod
import app.morphe.patches.instagram.misc.extension.enableStatus
import app.morphe.patches.instagram.misc.glass.giveTheCameraTheNavBar
import app.morphe.patches.instagram.reels.fullscreen.barHeightsPatch
import app.morphe.patches.instagram.reels.fullscreen.findBarHeights
import app.morphe.patches.instagram.reels.fullscreen.barHeightStores
import app.morphe.patches.instagram.misc.extension.instagramExtensionPatch
import app.morphe.patches.instagram.misc.settings.settingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.returnEarly

@Suppress("unused")
val liquidGlassNavBarPatch = bytecodePatch(
    name = "Liquid Glass navigation bar",
    description = "Turns the bottom tab bar into a floating iOS 26 style Liquid Glass pill " +
            "with live background blur and edge refraction. " +
            "Refraction needs Android 13+, blur needs Android 12+, older versions get a tinted pill. " +
            "Configure in HushGram settings or profile menu.",
    default = true
) {
    category("Interface")
    compatibleWith(*AppCompatibilities.instagram())

    dependsOn(settingsPatch, instagramExtensionPatch, barHeightsPatch)

    val blurRadius by intOption(
        key = "blurRadius",
        default = 4,
        values = mapOf(
            "Clear" to 2,
            "Default" to 4,
            "Frosted" to 12,
        ),
        title = "Blur radius",
        description = "Blur applied to content behind the bar, in dp.",
        required = true,
    ) { it!! in 0..40 }

    val refractionStrength by intOption(
        key = "refractionStrength",
        default = 60,
        values = mapOf(
            "Off" to 0,
            "Subtle" to 30,
            "Default" to 60,
            "Strong" to 100,
        ),
        title = "Refraction strength",
        description = "How strongly the glass edge bends the content behind it, from 0 to 100.",
        required = true,
    ) { it!! in 0..100 }

    val extendContent by booleanOption(
        key = "extendContentBehindBar",
        default = true,
        title = "Extend content behind bar",
        description = "Lets feeds scroll underneath the bar so there is something to see through the glass. " +
                "Turn off if the last item of a list ends up hidden.",
        required = true,
    )

    execute {
        requireStatusMethod("liquidGlassNavBar")
        val navHeight = findBarHeights().barHeightStores().getOrNull(1)?.field
            ?: throw PatchException("Liquid Glass navigation bar: Instagram's navigation bar height was not found")
        giveTheCameraTheNavBar(navHeight)

        val extensionClass = mutableClassDefBy(EXTENSION_CLASS)

        extensionClass.methods.singleOrNull { it.name == "blurRadiusDp" }?.addInstructions(
            0,
            """
                const/16 v0, $blurRadius
                return v0
            """
        )

        extensionClass.methods.singleOrNull { it.name == "refractionPercent" }?.addInstructions(
            0,
            """
                const/16 v0, $refractionStrength
                return v0
            """
        )

        extensionClass.methods.singleOrNull { it.name == "extendContentBehindBar" }?.addInstructions(
            0,
            """
                const/4 v0, ${if (extendContent == true) 1 else 0}
                return v0
            """
        )

        // Mark patch included: isIncluded() -> true
        extensionClass.methods.singleOrNull { it.name == "isIncluded" }?.returnEarly(true)

        // Feed videos play in SurfaceViews, separate layers the glass can't record: over a
        // video the bar couldn't blur. Every answer of the player's SurfaceView check goes
        // through the extension, which can pick Instagram's own TextureView path instead.
        // Optional: if a build reshapes the player, the glass still patches, just without this.
        runCatching {
            UseSurfaceViewFingerprint.matchAllOrNull()?.firstOrNull()?.let { match ->
                val method = match.method
                val returns = method.instructions.withIndex()
                    .filter { it.value.opcode == Opcode.RETURN }
                    .map { it.index }
                check(returns.isNotEmpty()) { "${method.name} returns nothing" }
                returns.asReversed().forEach { index ->
                    val register = (method.instructions[index] as OneRegisterInstruction).registerA
                    method.replaceInstruction(index, "invoke-static { v$register }, $EXTENSION_CLASS->useSurfaceView(Z)Z")
                    method.addInstructions(
                        index + 1,
                        """
                            move-result v$register
                            return v$register
                        """
                    )
                }
            }
        }
        enableStatus("liquidGlassNavBar")
    }
}
