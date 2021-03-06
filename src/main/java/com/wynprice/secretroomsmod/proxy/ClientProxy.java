package com.wynprice.secretroomsmod.proxy;

import java.awt.Color;
import java.lang.reflect.Field;

import com.wynprice.secretroomsmod.SecretItems;
import com.wynprice.secretroomsmod.base.interfaces.ISecretBlock;
import com.wynprice.secretroomsmod.base.interfaces.ISecretTileEntity;
import com.wynprice.secretroomsmod.gui.GuiProgrammableSwitchProbe;
import com.wynprice.secretroomsmod.handler.HandlerUpdateChecker;
import com.wynprice.secretroomsmod.handler.ProbeSwitchRenderHander;
import com.wynprice.secretroomsmod.handler.ReloadTrueSightModelsHandler;
import com.wynprice.secretroomsmod.handler.SecretKeyBindings;
import com.wynprice.secretroomsmod.render.FakeChunkRenderFactory;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.IRenderChunkFactory;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.client.renderer.color.IItemColor;
import net.minecraft.client.renderer.color.ItemColors;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.items.ItemStackHandler;

public class ClientProxy extends CommonProxy 
{	
	@Override
	public void displayGui(int guiID, Object... objects) 
	{
		GuiScreen gui = null;
		switch (guiID) 
		{
		case 0:
			gui = new GuiProgrammableSwitchProbe((ItemStack) objects[0]);
			break;

		default:
			break;
		}
		
		Minecraft.getMinecraft().displayGuiScreen(gui);
	}
	
	@Override
	public void init(FMLInitializationEvent event) {
		super.init(event);

		try
		{
			for(Field field : RenderGlobal.class.getDeclaredFields())
				if(field.getType() == IRenderChunkFactory.class)
				{
					field.setAccessible(true);
					field.set(Minecraft.getMinecraft().renderGlobal, new FakeChunkRenderFactory((IRenderChunkFactory) field.get(Minecraft.getMinecraft().renderGlobal)));
					field.setAccessible(false);
				}
		}
		catch (Throwable e) {
			e.printStackTrace();
		}
		    	
    	ItemColors itemColors = Minecraft.getMinecraft().getItemColors();
    	BlockColors blockColors = Minecraft.getMinecraft().getBlockColors();
    	
        itemColors.registerItemColorHandler((stack, tintIndex) -> 
        {
	        if(stack.hasTagCompound() && GuiScreen.isAltKeyDown())
	    	{
	        	ItemStackHandler handler = new ItemStackHandler(1);
		    	handler.deserializeNBT(stack.getTagCompound().getCompoundTag("hit_itemstack"));
		    	return itemColors.colorMultiplier(handler.getStackInSlot(0), tintIndex);
	    	}
            return 0xFFFFFF;
        }, SecretItems.SWITCH_PROBE);
        
        itemColors.registerItemColorHandler((stack, tintIndex) -> 
        {
	        if(stack.hasTagCompound() && GuiScreen.isAltKeyDown())
	    	{
	        	ItemStackHandler handler = new ItemStackHandler(1);
		    	handler.deserializeNBT(stack.getTagCompound().getCompoundTag("hit_itemstack"));
		    	return itemColors.colorMultiplier(handler.getStackInSlot(0), tintIndex);
	    	}
            return 0xFFFFFF;
        }, SecretItems.PROGRAMMABLE_SWITCH_PROBE);
        
        itemColors.registerItemColorHandler(new IItemColor() 
        {
        	private int currentID = 0;
        	private long previousCurrentMills = System.currentTimeMillis();
        	
			@Override
			public int colorMultiplier(ItemStack stack, int tintIndex) 
			{
				return stack.getMetadata() == 1? color(stack.hasTagCompound() && stack.getTagCompound().hasKey("hit_color", 99) ? stack.getTagCompound().getInteger("hit_color") == 0 ? 0xFFFFFF : stack.getTagCompound().getInteger("hit_color") : 0xFFFFFF) : -1;
			}
			
			private int color(int stackcolor)
			{
				if(System.currentTimeMillis() - previousCurrentMills > 0)//Prevent color moving faster if more instances of item
				{
					previousCurrentMills = System.currentTimeMillis();
					currentID += 10;
				}
				Color color1 = new Color(Color.HSBtoRGB((currentID % 4000) / 4000f, 1f, 1f));
				Color color2 = new Color(stackcolor);
				double totalAlpha = color1.getAlpha() + color2.getAlpha();
			    double weight0 = 0.25f;
			    double weight1 = 0.75f;
			    double r = weight0 * color1.getRed() + weight1 * color2.getRed();
			    double g = weight0 * color1.getGreen() + weight1 * color2.getGreen();
			    double b = weight0 * color1.getBlue() + weight1 * color2.getBlue();
			    double a = Math.max(color1.getAlpha(), color2.getAlpha());
			    return new Color((int) r, (int) g, (int) b, (int) a).getRGB();
			}
		}, SecretItems.CAMOUFLAGE_PASTE);
        for(Block block : ForgeRegistries.BLOCKS.getValues())
        	if(block instanceof ISecretBlock)
		        blockColors.registerBlockColorHandler(new IBlockColor() {
					
					@Override
					public int colorMultiplier(IBlockState state, IBlockAccess worldIn, BlockPos pos, int tintIndex) 
					{
						if(state == null || worldIn == null)
							return -1;
						TileEntity tileEntity = worldIn.getTileEntity(pos);
						if(tileEntity instanceof ISecretTileEntity && ((ISecretTileEntity)tileEntity).getMirrorStateSafely() != null)
							return blockColors.colorMultiplier(((ISecretTileEntity)tileEntity).getMirrorStateSafely(), worldIn, pos, tintIndex);
						return -1;
					}
				}, block);
	}
	
	@Override
	public EntityPlayer getPlayer() {
		return Minecraft.getMinecraft().player;
	}
}
