package cc.spea.booktweaks.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultilineTextField;

@Mixin(MultiLineEditBox.class)
public abstract class MultiLineEditBoxMixin {
    @Shadow
    private MultilineTextField textField;

    @Unique
    public MultilineTextField bookTweaks$getTextField() {
        return this.textField;
    }
}
