package cc.spea.booktweaks.mixin.client;

import cc.spea.booktweaks.accessor.MultiLineEditBoxAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultilineTextField;

@Mixin(MultiLineEditBox.class)
public abstract class MultiLineEditBoxMixin implements MultiLineEditBoxAccessor {
    @Shadow
    private MultilineTextField textField;

    @Override
    public MultilineTextField bookTweaks$getTextField() {
        return this.textField;
    }
}
