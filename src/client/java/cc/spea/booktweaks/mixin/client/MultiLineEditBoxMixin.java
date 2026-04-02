package cc.spea.booktweaks.mixin.client;

import cc.spea.booktweaks.accessor.MultiLineEditBoxAccessor;
#if MC_VER >= MC_1_21_6
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
#else
import net.minecraft.client.gui.components.MultilineTextField;

public abstract class MultiLineEditBoxMixin implements MultiLineEditBoxAccessor {
    @Override
    public MultilineTextField bookTweaks$getTextField() {
        throw new UnsupportedOperationException("MultiLineEditBoxMixin is only available on 1.21.6+");
    }
}
#endif
