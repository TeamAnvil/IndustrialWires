/*
 * This file is part of Industrial Wires.
 * Copyright (C) 2016-2018 malte0811
 * Industrial Wires is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Industrial Wires is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with Industrial Wires.  If not, see <http://www.gnu.org/licenses/>.
 */

package malte0811.industrialWires.converter;

import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IPlayerInteraction;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IRedstoneOutput;
import blusunrize.immersiveengineering.common.util.ChatUtils;
import blusunrize.immersiveengineering.common.util.Utils;
import malte0811.industrialWires.IndustrialWires;
import malte0811.industrialWires.blocks.converter.MechanicalMBBlockType;
import malte0811.industrialWires.util.LocalSidedWorld;
import malte0811.industrialWires.util.NBTKeys;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;

import javax.annotation.Nonnull;

import static blusunrize.immersiveengineering.common.IEContent.blockMetalDecoration0;
import static blusunrize.immersiveengineering.common.blocks.metal.BlockTypes_MetalDecoration0.RS_ENGINEERING;
import static malte0811.industrialWires.blocks.converter.MechanicalMBBlockType.SPEEDOMETER;
import static net.minecraft.util.EnumFacing.Axis.X;
import static net.minecraft.util.EnumFacing.AxisDirection.POSITIVE;
import static net.minecraft.util.math.BlockPos.ORIGIN;

public class MechPartSpeedometer extends MechMBPart implements IPlayerInteraction, IRedstoneOutput {
	private double speedFor15RS = 2 * Waveform.EXTERNAL_SPEED;
	private int currentOutputLin = -1;
	private int currentOutputLog = -1;
	private double logFactor = 15 / Math.log(speedFor15RS + 1);
	private MechEnergy energy = null;

	@Override
	public void createMEnergy(MechEnergy e) {
	}

	@Override
	public double requestMEnergy(MechEnergy e) {
		energy = e;
		return 0;
	}

	@Override
	public void insertMEnergy(double added) {
		update(false);
	}

	private void update(boolean changedMax) {
		if (changedMax) {
			logFactor = 15 / Math.log(speedFor15RS + 1);
		}
		if (energy != null) {
			int newLin = roundHysteresis(currentOutputLin, 15 * energy.getSpeed() / speedFor15RS);
			int newLog = roundHysteresis(currentOutputLog, Math.log(energy.getSpeed() + 1) * logFactor);
			if (newLin != currentOutputLin || newLog != currentOutputLog) {
				currentOutputLin = newLin;
				currentOutputLog = newLog;
				world.markForUpdate(ORIGIN);
			}
		}
	}

	private int roundHysteresis(int old, double newExact) {
		if (old<newExact) {
			return (int) Math.floor(newExact);
		} else {
			return (int) Math.ceil(newExact);
		}
	}

	@Override
	public double getInertia() {
		return 60;//TODO
	}

	@Override
	public double getMaxSpeed() {
		return 2 * speedFor15RS;
	}

	@Override
	public void writeToNBT(NBTTagCompound out) {
		out.setDouble(NBTKeys.MAX_SPEED, speedFor15RS);
	}

	@Override
	public void readFromNBT(NBTTagCompound in) {
		speedFor15RS = in.getDouble(NBTKeys.MAX_SPEED);
		update(true);
	}

	@Override
	public ResourceLocation getRotatingBaseModel() {
		return new ResourceLocation(IndustrialWires.MODID, "block/mech_mb/shaft.obj");
	}

	@Override
	public boolean canForm(LocalSidedWorld w) {
		IBlockState state = w.getBlockState(ORIGIN);
		return state.getBlock() == blockMetalDecoration0 &&
				state.getValue(blockMetalDecoration0.property) == RS_ENGINEERING;
	}

	@Override
	public short getFormPattern() {
		return 0b000_010_000;
	}

	@Override
	public void disassemble(boolean failed, MechEnergy energy) {
		world.setBlockState(ORIGIN, blockMetalDecoration0
				.getStateFromMeta(RS_ENGINEERING.getMeta()));
	}

	@Override
	public MechanicalMBBlockType getType() {
		return SPEEDOMETER;
	}

	@Override
	public boolean interact(@Nonnull EnumFacing side, @Nonnull EntityPlayer player, @Nonnull EnumHand hand,
							@Nonnull ItemStack heldItem, float hitX, float hitY, float hitZ) {
		if (Utils.isHammer(heldItem)) {
			if (!world.isRemote) {
				if (player.isSneaking()) {
					if (speedFor15RS > 1) {
						speedFor15RS--;
					}
				} else {
					speedFor15RS++;
				}
				ChatUtils.sendServerNoSpamMessages(player,
						new TextComponentTranslation(IndustrialWires.MODID + ".chat.maxSpeed",
								speedFor15RS));
				update(true);
			}
			return true;
		}
		return false;
	}

	@Override
	public int getStrongRSOutput(@Nonnull IBlockState state, @Nonnull EnumFacing side) {
		if (side.getAxis() == X) {
			if (side.getAxisDirection() == POSITIVE) {
				return currentOutputLog;
			} else {
				return currentOutputLin;
			}
		}
		return 0;
	}

	@Override
	public boolean canConnectRedstone(@Nonnull IBlockState state, @Nonnull EnumFacing side) {
		return side.getAxis() == X;
	}

	@Override
	public AxisAlignedBB getBoundingBox(BlockPos offsetPart) {
		return new AxisAlignedBB(0, .1875, 0, 1, .8125, 1);
	}
}
