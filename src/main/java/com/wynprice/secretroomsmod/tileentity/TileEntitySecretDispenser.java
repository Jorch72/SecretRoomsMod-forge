package com.wynprice.secretroomsmod.tileentity;

import javax.annotation.Nullable;

import com.wynprice.secretroomsmod.base.interfaces.ISecretBlock;
import com.wynprice.secretroomsmod.base.interfaces.ISecretTileEntity;
import com.wynprice.secretroomsmod.handler.ParticleHandler;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

public class TileEntitySecretDispenser extends TileEntityDispenser implements ISecretTileEntity
{
	protected IBlockState mirrorState;
	
	private boolean locked;
	
	@Override
	public void readFromNBT(NBTTagCompound compound) {
		super.readFromNBT(compound);
		locked = getTileData().getBoolean("locked");
		Block testBlock = Block.REGISTRY.getObject(new ResourceLocation(getTileData().getString("MirrorBlock")));
		if(testBlock != Blocks.AIR)
			mirrorState = testBlock.getStateFromMeta(getTileData().getInteger("MirrorMeta"));
		if(mirrorState != null && mirrorState.getBlock() instanceof ISecretBlock)
			mirrorState = null;
		if(!ISecretBlock.ALL_SECRET_TILE_ENTITIES.contains(this))
			ISecretBlock.ALL_SECRET_TILE_ENTITIES.add(this);
	}
	
	@Override
	public void update() {
		if(mirrorState != null)
			ParticleHandler.BLOCKBRAKERENDERMAP.put(pos, mirrorState.getBlock().getStateFromMeta(mirrorState.getBlock().getMetaFromState(mirrorState)));
	}
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		if(mirrorState != null)
		{
			getTileData().setString("MirrorBlock", mirrorState.getBlock().getRegistryName().toString());
			getTileData().setInteger("MirrorMeta", mirrorState.getBlock().getMetaFromState(mirrorState));
		}
		getTileData().setBoolean("locked", locked);
		return super.writeToNBT(compound);
	}
	
	public IBlockState getMirrorState() {
		if(mirrorState == null && ParticleHandler.BLOCKBRAKERENDERMAP.containsKey(pos))
			mirrorState = ParticleHandler.BLOCKBRAKERENDERMAP.get(pos);
		if(mirrorState == null && FORCED_RENDER_MAP.containsKey(pos))
			mirrorState = FORCED_RENDER_MAP.get(pos);
		return mirrorState;
	}
	
	public void setMirrorState(IBlockState mirrorState, @Nullable BlockPos pos)
	{
		if(!locked)
			setMirrorStateForcable(mirrorState, pos);
		locked = true;
	}
	
	public void setMirrorStateForcable(IBlockState mirrorState, @Nullable BlockPos pos)
	{
		if(mirrorState.getBlock() instanceof ISecretBlock && ((ISecretBlock)mirrorState.getBlock()).getState(world, pos) != null)
			mirrorState = ((ISecretBlock)mirrorState.getBlock()).getState(world, pos);
		this.mirrorState = mirrorState.getBlock().getStateFromMeta(mirrorState.getBlock().getMetaFromState(mirrorState));
	}
	
	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		NBTTagCompound nbt = new NBTTagCompound();
		this.writeToNBT(nbt);
		int metadata = getBlockMetadata();
		return new SPacketUpdateTileEntity(this.pos, metadata, nbt);
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		this.readFromNBT(pkt.getNbtCompound());
	}

	@Override
	public NBTTagCompound getUpdateTag() {
		NBTTagCompound nbt = new NBTTagCompound();
		this.writeToNBT(nbt);
		return nbt;
	}

	@Override
	public void handleUpdateTag(NBTTagCompound tag) {
		this.readFromNBT(tag);
	}
}
