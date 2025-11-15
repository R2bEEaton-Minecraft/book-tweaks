package cc.spea.booktweaks.mixin.client;

import cc.spea.booktweaks.accessor.MultilineTextFieldAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.gui.components.Whence;

@Mixin(MultilineTextField.class)
public abstract class MultilineTextFieldMixin implements MultilineTextFieldAccessor {
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

    @Invoker("seekCursor")
    public abstract void bookTweaks$seekCursor(Whence whence, int i);

    @Invoker("getLineAtCursor")
    public abstract int bookTweaks$getLineAtCursor();

    @Override
    public String bookTweaks$getValue() {
        return value;
    }

    @Override
    public int bookTweaks$getCursor() {
        return cursor;
    }

    @Override
    public int bookTweaks$getSelectCursor() {
        return selectCursor;
    }
}
