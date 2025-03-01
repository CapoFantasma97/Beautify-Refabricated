package com.github.suel_ki.beautify.compat.jade.provider;

import com.github.suel_ki.beautify.common.block.HangingPot;
import com.github.suel_ki.beautify.common.block.OakTrellis;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IDisplayHelper;
import snownee.jade.api.ui.IElementHelper;

public enum TrellisProvider implements IBlockComponentProvider {

    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (accessor.getBlock() instanceof OakTrellis trellis) {
            BlockState state = accessor.getBlockState();
            // if there is a plant
            if (state.getValue(OakTrellis.FLOWERS) != 0) {
                ItemStack iconFlower = new ItemStack(trellis.getValidFlowers().get(state.getValue(OakTrellis.FLOWERS)));
                if (iconFlower.isEmpty())
                    return;
                tooltip.add(IElementHelper.get().smallItem(iconFlower));
                tooltip.append(IDisplayHelper.get().stripColor(iconFlower.getHoverName()));
            }
        }
    }

    @Override
    public ResourceLocation getUid() {
        return new ResourceLocation("trellis.pot_plant");
    }
}
