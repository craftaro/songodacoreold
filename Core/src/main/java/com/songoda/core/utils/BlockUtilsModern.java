package com.songoda.core.utils;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.AnaloguePowerable;
import org.bukkit.block.data.Powerable;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.Gate;
import org.bukkit.block.data.type.Switch;
import org.bukkit.block.data.type.TrapDoor;

public class BlockUtilsModern {

	protected static void _updatePressurePlateModern(Block plate, int power) {
		BlockData blockData = plate.getBlockData();
		boolean update = false;
		if (blockData instanceof AnaloguePowerable) {
			AnaloguePowerable a = (AnaloguePowerable) blockData;
			int toPower = Math.min(a.getMaximumPower(), power);
			if ((update = toPower != a.getPower())) {
				a.setPower(toPower);
				plate.setBlockData(a);
			}
		} else if (blockData instanceof Powerable) {
			Powerable p = (Powerable) blockData;
			if ((update = p.isPowered() != (power != 0))) {
				p.setPowered(power != 0);
				plate.setBlockData(p);
			}
		}
		if (update) {
			_updateRedstoneNeighbours(plate);
		}
	}

	protected static void _toggleLeverModern(Block lever) {
		BlockData blockData = lever.getBlockData();
		if (blockData instanceof Switch) {
			Switch s = (Switch) blockData;
			s.setPowered(!s.isPowered());
			lever.setBlockData(s);
			_updateRedstoneNeighbours(lever);
		}
	}

	protected static void _pressButtonModern(Block button) {
		BlockData blockData = button.getBlockData();
		if (blockData instanceof Switch) {
			Switch s = (Switch) blockData;
			s.setPowered(true);
			button.setBlockData(s);
			_updateRedstoneNeighbours(button);
		}
	}

	static void _releaseButtonModern(Block button) {
		BlockData blockData = button.getBlockData();
		if (blockData instanceof Switch) {
			Switch s = (Switch) blockData;
			s.setPowered(false);
			button.setBlockData(s);
			_updateRedstoneNeighbours(button);
		}
	}

	private static Class<?> clazzCraftWorld, clazzCraftBlock,
			clazzLeverBlock, clazzButtonBlock, clazzPressurePlateBlock;
	private static Method craftWorld_getHandle, craftBlock_getNMSBlock, craftBlock_getPostition, craftBlockData_getState,
			nmsLever_updateNeighbours, nmsButton_updateNeighbours, nmsPlate_updateNeighbours;

	static {
		try {
			// Cache reflection.

			String ver = Bukkit.getServer().getClass().getPackage().getName().substring(23);
			clazzCraftWorld = Class.forName("org.bukkit.craftbukkit." + ver + ".CraftWorld");
			clazzCraftBlock = Class.forName("org.bukkit.craftbukkit." + ver + ".block.CraftBlock");
			//clazzBlockPosition = Class.forName("net.minecraft.server." + ver + ".BlockPosition");

			craftWorld_getHandle = clazzCraftWorld.getMethod("getHandle");
			craftBlock_getNMSBlock = clazzCraftBlock.getDeclaredMethod("getNMSBlock");
			craftBlock_getNMSBlock.setAccessible(true);
			craftBlock_getPostition = clazzCraftBlock.getDeclaredMethod("getPosition");

			Class<?> clazzCraftBlockData = Class.forName("org.bukkit.craftbukkit." + ver + ".block.data.CraftBlockData");
			craftBlockData_getState = clazzCraftBlockData.getDeclaredMethod("getState");

			Class<?> clazzWorld = Class.forName("net.minecraft.server." + ver + ".World");
			Class<?> clazzBlockState = Class.forName("net.minecraft.server." + ver + ".IBlockData");
			Class<?> clazzBlockPos = Class.forName("net.minecraft.server." + ver + ".BlockPosition");
			clazzLeverBlock = Class.forName("net.minecraft.server." + ver + ".BlockLever");
			clazzButtonBlock = Class.forName("net.minecraft.server." + ver + ".BlockButtonAbstract");
			clazzPressurePlateBlock = Class.forName("net.minecraft.server." + ver + ".BlockPressurePlateAbstract");

			// nmsLever_updateNeighbours, nmsButton_updateNeighbours, nmsPlate_updateNeighbours
			nmsLever_updateNeighbours = clazzLeverBlock.getDeclaredMethod("e", clazzBlockState, clazzWorld, clazzBlockPos);
			nmsLever_updateNeighbours.setAccessible(true);

			nmsButton_updateNeighbours = clazzButtonBlock.getDeclaredMethod("f", clazzBlockState, clazzWorld, clazzBlockPos);
			nmsButton_updateNeighbours.setAccessible(true);

			nmsPlate_updateNeighbours = clazzPressurePlateBlock.getDeclaredMethod("a", clazzWorld, clazzBlockPos);
			nmsPlate_updateNeighbours.setAccessible(true);
		} catch (Throwable ex) {
			Logger.getLogger(BlockUtilsModern.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	static void _updateRedstoneNeighbours(Block block) {
		try {
			// spigot made some changes to how data updates work in 1.13+
			// updating the data value of a redstone power source
			// does NOT update attatched block power,
			// even if you update the block state. (Still broken last I checked in 1.15.2)
			// so now we're going to manually call the updateNeighbours block method

			// invoke and cast objects.
			Object cworld = clazzCraftWorld.cast(block.getWorld());
			Object mworld = craftWorld_getHandle.invoke(cworld);
			Object cblock = clazzCraftBlock.cast(block);
			Object mblock = craftBlock_getNMSBlock.invoke(cblock);
			Object mpos = craftBlock_getPostition.invoke(cblock);

			//System.out.println(mblock.getClass());
			// now for testing stuff
			if (clazzLeverBlock.isAssignableFrom(mblock.getClass())) {
				final Object mstate = craftBlockData_getState.invoke(block.getBlockData());
				nmsLever_updateNeighbours.invoke(mblock, mstate, mworld, mpos);
			} else if (clazzButtonBlock.isAssignableFrom(mblock.getClass())) {
				final Object mstate = craftBlockData_getState.invoke(block.getBlockData());
				nmsButton_updateNeighbours.invoke(mblock, mstate, mworld, mpos);
			} else if (clazzPressurePlateBlock.isAssignableFrom(mblock.getClass())) {
				nmsPlate_updateNeighbours.invoke(mblock, mworld, mpos);
			} else {
				System.out.println("Unknown redstone: " + mblock.getClass().getName());
			}
//			
//			if(mblock instanceof net.minecraft.server.v1_15_R1.BlockLever) {
//				Method updateNeighbours = net.minecraft.server.v1_15_R1.BlockLever.class.getDeclaredMethod("e", net.minecraft.server.v1_15_R1.IBlockData.class, net.minecraft.server.v1_15_R1.World.class, net.minecraft.server.v1_15_R1.BlockPosition.class);
//				updateNeighbours.setAccessible(true);
//				// IBlockData = block state after being powered
//				
//				updateNeighbours.invoke(mblock, 
//						((org.bukkit.craftbukkit.v1_15_R1.block.data.CraftBlockData) block.getBlockData()).getState(),
//						mworld,
//						mpos);
//			} else if(mblock instanceof net.minecraft.server.v1_15_R1.BlockButtonAbstract) {
//				Method updateNeighbours = net.minecraft.server.v1_15_R1.BlockButtonAbstract.class.getDeclaredMethod("f", net.minecraft.server.v1_15_R1.IBlockData.class, net.minecraft.server.v1_15_R1.World.class, net.minecraft.server.v1_15_R1.BlockPosition.class);
//				updateNeighbours.setAccessible(true);
//				// IBlockData = block state after being powered
//				
//				updateNeighbours.invoke(mblock, 
//						((org.bukkit.craftbukkit.v1_15_R1.block.data.CraftBlockData) block.getBlockData()).getState(),
//						mworld,
//						mpos);
//			} else if(mblock instanceof net.minecraft.server.v1_15_R1.BlockPressurePlateAbstract) {
//				Method updateNeighbours = net.minecraft.server.v1_15_R1.BlockPressurePlateAbstract.class.getDeclaredMethod("a", net.minecraft.server.v1_15_R1.World.class, net.minecraft.server.v1_15_R1.BlockPosition.class);
//				updateNeighbours.setAccessible(true);
//				// IBlockData = block state after being powered
//				
//				updateNeighbours.invoke(mblock, 
//						mworld,
//						mpos);
//			}
		} catch (Throwable ex) {
			Logger.getLogger(BlockUtilsModern.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

    protected static void _toggleDoorStatesModern(boolean allowDoorToOpen, Block... doors) {
        for (Block door : doors) {
            BlockData blockData;
            if (door == null || !((blockData = door.getBlockData()) instanceof Door)) {
                continue;
            }

            Door data = (Door) blockData;
            if (!allowDoorToOpen && !data.isOpen()) {
                continue;
            }

            // The lower half of the door contains the open/close state
            if (data.getHalf() == Bisected.Half.TOP) {
                Block lowerHalf = door.getRelative(BlockFace.DOWN);
                if (lowerHalf.getBlockData() instanceof Door) {
                    Door lowerData = (Door) lowerHalf.getBlockData();
                    lowerData.setOpen(!data.isOpen());
                    lowerHalf.setBlockData(lowerData);
                }
            } else {
                data.setOpen(!data.isOpen());
                door.setBlockData(data);
            }

            // Play the door open/close sound
            door.getWorld().playEffect(door.getLocation(), Effect.DOOR_TOGGLE, 0);
        }
    }

    protected static Block _getDoubleDoorModern(Block block) {
        BlockData bd = block.getBlockData();
        Block door = null;
        if (bd instanceof Door) {
            final Door d = (Door) bd;
            final BlockFace face = d.getFacing();
            if (face.getModX() == 0) {
                if (d.getHinge() == Door.Hinge.RIGHT) {
                    door = block.getRelative(face.getModZ(), 0, 0);
                } else {
                    door = block.getRelative(-face.getModZ(), 0, 0);
                }
            } else {
                if (d.getHinge() == Door.Hinge.RIGHT) {
                    door = block.getRelative(0, 0, -face.getModX());
                } else {
                    door = block.getRelative(0, 0, face.getModX());
                }
            }
        }
        return door != null && door.getBlockData() instanceof Door
                && ((Door) door.getBlockData()).getHinge() != ((Door) bd).getHinge() ? door : null;
    }

    protected static BlockFace _getDoorClosedDirectionModern(Block door) {
        if (BlockUtils.DOORS.contains(door.getType())) {
            BlockData bd = door.getBlockData();
            if (bd instanceof Door) {
                Door d = (Door) bd;

                // The lower half of the door contains the open/close state
                if (d.getHalf() == Bisected.Half.TOP) {
                    door = door.getRelative(BlockFace.DOWN);
                    if (door.getBlockData() instanceof Door) {
                        d = (Door) door.getBlockData();
                    } else {
                        return null;
                    }
                }

                final BlockFace face = d.getFacing();
                // now we /could/ also correct for the hinge (top block), it's not needed information
                if (face.getModX() == 0) {
                    return d.isOpen() ? BlockFace.EAST : BlockFace.SOUTH;
                } else {
                    return d.isOpen() ? BlockFace.SOUTH : BlockFace.EAST;
                }
            }
        } else if (BlockUtils.FENCE_GATES.contains(door.getType())) {
            BlockData bd = door.getBlockData();
            if (bd instanceof Gate) {
                Gate g = (Gate) bd;
                final BlockFace face = g.getFacing();
                if (face.getModX() == 0) {
                    return g.isOpen() ? BlockFace.EAST : BlockFace.SOUTH;
                } else {
                    return g.isOpen() ? BlockFace.SOUTH : BlockFace.EAST;
                }
            }
        } else if (BlockUtils.TRAP_DOORS.contains(door.getType())) {
            BlockData bd = door.getBlockData();
            if (bd instanceof TrapDoor) {
                TrapDoor t = (TrapDoor) bd;
                if (!t.isOpen()) {
                    return BlockFace.UP;
                } else {
                    return t.getFacing();
                }
            }
        }
        return null;
    }

    protected static boolean _isCropFullyGrown(Block block) {
        BlockData data = block.getBlockData();
        if(data instanceof Ageable) {
            return ((Ageable) data).getAge() == ((Ageable) data).getMaximumAge();
        }
        return false;
    }

    protected static int _getMaxGrowthStage(Block block) {
        BlockData data = block.getBlockData();
        if(data instanceof Ageable) {
            return ((Ageable) data).getMaximumAge();
        }
        return -1;
    }

    protected static int _getMaxGrowthStage(Material material) {
        BlockData data = material.createBlockData();
        if(data instanceof Ageable) {
            return ((Ageable) data).getMaximumAge();
        }
        return -1;
    }

    public static void _setGrowthStage(Block block, int stage) {
        BlockData data = block.getBlockData();
        if (data instanceof Ageable) {
            ((Ageable) data).setAge(Math.max(0, Math.min(stage, ((Ageable) data).getMaximumAge())));
            block.setBlockData(data);
        }
    }

    public static void _incrementGrowthStage(Block block) {
        BlockData data = block.getBlockData();
        if (data instanceof Ageable) {
            final int max = ((Ageable) data).getMaximumAge();
            final int age = ((Ageable) data).getAge();
            if (age < max) {
                ((Ageable) data).setAge(age + 1);
                block.setBlockData(data);
            }
        }
    }

    public static void _resetGrowthStage(Block block) {
        BlockData data = block.getBlockData();
        if (data instanceof Ageable) {
            ((Ageable) data).setAge(0);
            block.setBlockData(data);
        }
    }
}
