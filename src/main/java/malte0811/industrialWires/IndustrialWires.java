/*
 * This file is part of Industrial Wires.
 * Copyright (C) 2016-2017 malte0811
 *
 * Industrial Wires is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Industrial Wires is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Industrial Wires.  If not, see <http://www.gnu.org/licenses/>.
 */
package malte0811.industrialWires;

import malte0811.industrialWires.blocks.BlockIWBase;
import malte0811.industrialWires.blocks.BlockJacobsLadder;
import malte0811.industrialWires.blocks.TileEntityJacobsLadder;
import malte0811.industrialWires.blocks.controlpanel.*;
import malte0811.industrialWires.blocks.converter.BlockMechanicalConverter;
import malte0811.industrialWires.blocks.converter.TileEntityIEMotor;
import malte0811.industrialWires.blocks.converter.TileEntityMechICtoIE;
import malte0811.industrialWires.blocks.converter.TileEntityMechIEtoIC;
import malte0811.industrialWires.blocks.wire.*;
import malte0811.industrialWires.controlpanel.PanelUtils;
import malte0811.industrialWires.crafting.Recipes;
import malte0811.industrialWires.items.ItemIC2Coil;
import malte0811.industrialWires.items.ItemKey;
import malte0811.industrialWires.items.ItemPanelComponent;
import malte0811.industrialWires.network.MessageGUIInteract;
import malte0811.industrialWires.network.MessageItemSync;
import malte0811.industrialWires.network.MessagePanelInteract;
import malte0811.industrialWires.network.MessageTileSyncIW;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

@Mod(modid = IndustrialWires.MODID, version = IndustrialWires.VERSION, dependencies = "required-after:immersiveengineering@[0.10-58,);after:ic2",
		certificateFingerprint = "7e11c175d1e24007afec7498a1616bef0000027d")
@Mod.EventBusSubscriber
public class IndustrialWires {
	public static final String MODID = "industrialwires";
	public static final String VERSION = "${version}";

	public static final List<BlockIWBase> blocks = new ArrayList<>();
	public static final List<Item> items = new ArrayList<>();

	@GameRegistry.ObjectHolder(MODID+":"+BlockIC2Connector.NAME)
	public static BlockIC2Connector ic2conn = null;
	@GameRegistry.ObjectHolder(MODID+":"+BlockMechanicalConverter.NAME)
	public static BlockMechanicalConverter mechConv = null;
	@GameRegistry.ObjectHolder(MODID+":"+BlockJacobsLadder.NAME)
	public static BlockJacobsLadder jacobsLadder = null;
	@GameRegistry.ObjectHolder(MODID+":"+BlockPanel.NAME)
	public static BlockPanel panel = null;

	@GameRegistry.ObjectHolder(MODID+":"+ItemIC2Coil.NAME)
	public static ItemIC2Coil coil = null;
	@GameRegistry.ObjectHolder(MODID+":"+ItemPanelComponent.NAME)
	public static ItemPanelComponent panelComponent = null;
	@GameRegistry.ObjectHolder(MODID+":"+ItemKey.ITEM_NAME)
	public static ItemKey key = null;
	public static final SimpleNetworkWrapper packetHandler = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);

	public static Logger logger;
	@Mod.Instance(MODID)
	public static IndustrialWires instance = new IndustrialWires();
	public static CreativeTabs creativeTab = new CreativeTabs(MODID) {

		@Override
		public ItemStack getTabIconItem() {
			if (coil!=null) {
				return new ItemStack(coil, 1, 2);
			} else {
				return new ItemStack(panel, 1, 3);
			}
		}
	};
	@SidedProxy(clientSide = "malte0811.industrialWires.client.ClientProxy", serverSide = "malte0811.industrialWires.CommonProxy")
	public static CommonProxy proxy;
	public static boolean hasIC2;
	public static boolean hasTechReborn;

	@EventHandler
	public void preInit(FMLPreInitializationEvent e) {
		hasIC2 = Loader.isModLoaded("ic2");
		hasTechReborn = Loader.isModLoaded("techreborn");
		logger = e.getModLog();
		new IWConfig();

		if (hasIC2) {
			GameRegistry.registerTileEntity(TileEntityIC2ConnectorTin.class, MODID + ":ic2ConnectorTin");
			GameRegistry.registerTileEntity(TileEntityIC2ConnectorCopper.class, MODID + ":ic2ConnectorCopper");
			GameRegistry.registerTileEntity(TileEntityIC2ConnectorGold.class, MODID + ":ic2ConnectorGold");
			GameRegistry.registerTileEntity(TileEntityIC2ConnectorHV.class, MODID + ":ic2ConnectorHV");
			GameRegistry.registerTileEntity(TileEntityIC2ConnectorGlass.class, MODID + ":ic2ConnectorGlass");
			// Dummy TE's with bad names used to update old TE's to the proper names
			GameRegistry.registerTileEntity(DummyTEs.TinDummy.class, MODID + "ic2ConnectorTin");
			GameRegistry.registerTileEntity(DummyTEs.CopperDummy.class, MODID + "ic2ConnectorCopper");
			GameRegistry.registerTileEntity(DummyTEs.GoldDummy.class, MODID + "ic2ConnectorGold");
			GameRegistry.registerTileEntity(DummyTEs.HVDummy.class, MODID + "ic2ConnectorHV");
			GameRegistry.registerTileEntity(DummyTEs.GlassDummy.class, MODID + "ic2ConnectorGlass");

			if (IWConfig.enableConversion) {
				GameRegistry.registerTileEntity(TileEntityIEMotor.class, MODID + ":ieMotor");
				GameRegistry.registerTileEntity(TileEntityMechICtoIE.class, MODID + ":mechIcToIe");
				GameRegistry.registerTileEntity(TileEntityMechIEtoIC.class, MODID + ":mechIeToIc");
			}
		}
		GameRegistry.registerTileEntity(TileEntityJacobsLadder.class, MODID + ":jacobsLadder");
		GameRegistry.registerTileEntity(TileEntityPanel.class, MODID + ":control_panel");
		GameRegistry.registerTileEntity(TileEntityRSPanelConn.class, MODID + ":control_panel_rs");
		GameRegistry.registerTileEntity(TileEntityPanelCreator.class, MODID + ":panel_creator");
		GameRegistry.registerTileEntity(TileEntityUnfinishedPanel.class, MODID + ":unfinished_panel");
		GameRegistry.registerTileEntity(TileEntityComponentPanel.class, MODID + ":single_component_panel");
		proxy.preInit();
	}

	@SubscribeEvent
	public static void registerBlocks(RegistryEvent.Register<Block> event) {

		if (IWConfig.enableConversion&&hasIC2) {
			event.getRegistry().register(new BlockMechanicalConverter());
		}
		if (hasIC2/*||hasTechReborn TODO talk to modmuss*/) {
			event.getRegistry().register(new BlockIC2Connector());
		}
		event.getRegistry().register(new BlockJacobsLadder());
		event.getRegistry().register(new BlockPanel());
	}

	@SubscribeEvent
	public static void registerItems(RegistryEvent.Register<Item> event) {
		for (BlockIWBase b:blocks) {
			event.getRegistry().register(b.createItemBlock());
		}

		if (hasIC2/*||hasTechReborn TODO talk to modmuss*/) {
			event.getRegistry().register(new ItemIC2Coil());
		}
		event.getRegistry().register(new ItemPanelComponent());
		event.getRegistry().register(new ItemKey());
	}

	@SubscribeEvent
	public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
		Recipes.addRecipes(event.getRegistry());
	}

	@EventHandler
	public void init(FMLInitializationEvent e) {

		if (hasIC2) {
			ExtraIC2Compat.addToolConmpat();
		}

		packetHandler.registerMessage(MessageTileSyncIW.HandlerClient.class, MessageTileSyncIW.class, 0, Side.CLIENT);
		packetHandler.registerMessage(MessagePanelInteract.HandlerServer.class, MessagePanelInteract.class, 1, Side.SERVER);
		packetHandler.registerMessage(MessageGUIInteract.HandlerServer.class, MessageGUIInteract.class, 2, Side.SERVER);
		packetHandler.registerMessage(MessageItemSync.HandlerServer.class, MessageItemSync.class, 3, Side.SERVER);

		NetworkRegistry.INSTANCE.registerGuiHandler(instance, proxy);
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent e) {
		proxy.postInit();
		PanelUtils.PANEL_ITEM = Item.getItemFromBlock(panel);
	}
}
