package github.kasuminova.novaeng.common.container.slot;

import github.kasuminova.novaeng.common.hypernet.proc.server.module.ModuleRegistry;
import github.kasuminova.novaeng.common.hypernet.proc.server.module.base.ModuleCPUExtBase;
import github.kasuminova.novaeng.common.util.ServerModuleInv;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;

public class SlotCPUExtItemHandler extends SlotConditionItemHandler {

    public SlotCPUExtItemHandler(final int index, final ServerModuleInv inventoryIn) {
        super(inventoryIn, index, -1, 0, 0);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public String getSlotDescription() {
        return I18n.format("gui.modular_server_assembler.assembly.cpu_ext.name");
    }

    @Override
    public boolean isItemValid(@Nonnull final ItemStack stack) {
        return ModuleRegistry.getModule(stack) instanceof ModuleCPUExtBase;
    }
}