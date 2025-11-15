package cc.spea.booktweaks.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.gui.components.MultilineTextField;

@Mixin(MultilineTextField.class)
public abstract class MultilineTextFieldMixin {
    @Shadow
    protected String value;

    @Shadow
    protected int cursor;

    @Shadow
    protected int selectCursor;

    @Invoker("overflowsLineLimit")
    public abstract boolean bookTweaks$overflowsLineLimit(String string);

    @Invoker("hasSelection")
    public abstract boolean bookTweaks$hasSelection();

    @Invoker("truncateInsertionText")
    public abstract String bookTweaks$truncateInsertionText(String string);
}
