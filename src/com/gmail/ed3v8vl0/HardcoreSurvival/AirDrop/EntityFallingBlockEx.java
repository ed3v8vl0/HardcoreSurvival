package com.gmail.ed3v8vl0.HardcoreSurvival.AirDrop;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.block.Chest;
import org.bukkit.craftbukkit.libs.jline.internal.Nullable;
import org.bukkit.craftbukkit.v1_12_R1.event.CraftEventFactory;
import org.bukkit.inventory.Inventory;

import net.minecraft.server.v1_12_R1.*;

public class EntityFallingBlockEx extends EntityFallingBlock {
    private IBlockData block;
    private boolean f;
    public NBTTagCompound tileEntityData;
    protected static final DataWatcherObject<BlockPosition> d = DataWatcher.a(EntityFallingBlock.class, DataWatcherRegistry.j);
    
    private List<EntityFallingBlockEx> fallings;
    private int id;
    private ParticleData particleData;
    private boolean force;
    
    private ChestPage chestPage = null;
    
    public EntityFallingBlockEx(World world) {
        super(world);
    }

    public EntityFallingBlockEx(List<EntityFallingBlockEx> fallings, int id, ParticleData particleData, World world, double d0, double d1, double d2, IBlockData iblockdata) {
        super(world, d0, d1, d2, iblockdata);
        this.fallings = fallings;
        this.id = id;
        this.particleData = particleData;
        
        this.block = iblockdata;
        this.i = true;
        this.setSize(0.98F, 0.98F);
        this.setPosition(d0, d1 + (double) ((1.0F - this.length) / 2.0F), d2);
        this.motX = 0.0D;
        this.motY = 0.0D;
        this.motZ = 0.0D;
        this.lastX = d0;
        this.lastY = d1;
        this.lastZ = d2;
        this.a(new BlockPosition(this));
    }

    public void setItems(ChestPage chestPage) {
    	this.chestPage = chestPage;
    }
    
    public boolean bd() {
        return false;
    }

    public void a(BlockPosition blockposition) {
        this.datawatcher.set(EntityFallingBlock.d, blockposition);
    }

    protected boolean playStepSound() {
        return false;
    }

    protected void i() {
        this.datawatcher.register(EntityFallingBlock.d, BlockPosition.ZERO);
    }

    public boolean isInteractable() {
        return !this.dead;
    }
    
    @Override
    public boolean isNoGravity() {
    	return true;
    }
    
    public void B_() {
        Block block = this.block.getBlock();
        
        if (this.block.getMaterial() == Material.AIR) {
            this.die();
        } else {
            this.lastX = this.locX;
            this.lastY = this.locY;
            this.lastZ = this.locZ;
            BlockPosition blockposition;
            
            this.world.getWorld().spawnParticle(this.particleData.particle, this.locX + this.particleData.xPos, this.locY + this.particleData.yPos, this.locZ + this.particleData.zPos, this.particleData.amount, this.particleData.offsetX, this.particleData.offsetY, this.particleData.offsetZ);
            
            
            this.move(EnumMoveType.SELF, this.motX, this.motY, this.motZ);
            
            if (!this.world.isClientSide) {
                blockposition = new BlockPosition(this);
                boolean flag = this.block.getBlock() == Blocks.dS;
                boolean flag1 = flag && this.world.getType(blockposition).getMaterial() == Material.WATER;
                double d0 = this.motX * this.motX + this.motY * this.motY + this.motZ * this.motZ;

                if (flag && d0 > 1.0D) {
                    MovingObjectPosition movingobjectposition = this.world.rayTrace(new Vec3D(this.lastX, this.lastY, this.lastZ), new Vec3D(this.locX, this.locY, this.locZ), true);

                    if (movingobjectposition != null && this.world.getType(movingobjectposition.a()).getMaterial() == Material.WATER) {
                        blockposition = movingobjectposition.a();
                        flag1 = true;
                    }
                }
                
                if ((!this.onGround && !flag1) && !this.force) {
                    if (this.ticksLived > 100 && !this.world.isClientSide && (blockposition.getY() < 1 || blockposition.getY() > 256) || this.ticksLived > 600) {
                        if (this.dropItem && this.world.getGameRules().getBoolean("doEntityDrops")) {
                            this.a(new ItemStack(block, 1, block.getDropData(this.block)), 0.0F);
                        }

                        this.die();
                    }
                } else {
                    IBlockData iblockdata = this.world.getType(blockposition);

                    if (!flag1 && BlockFalling.x(this.world.getType(new BlockPosition(this.locX, this.locY - 0.009999999776482582D, this.locZ)))) {
                        this.onGround = false;
                        // return; // CraftBukkit
                    }
                    
                    this.motX *= 0.699999988079071D;
                    this.motZ *= 0.699999988079071D;
                    this.motY *= -0.5D;
                    if (iblockdata.getBlock() != Blocks.PISTON_EXTENSION) {
                        this.die();
                        if (!this.f) {
                            if (CraftEventFactory.callEntityChangeBlockEvent(this, blockposition, this.block.getBlock(), this.block.getBlock().toLegacyData(this.block)).isCancelled()) {
							    return;
							}
							
                            if (this.chestPage == null) {
                            	this.world.setTypeAndData(blockposition, this.block, 3);
                            } else {
                            	org.bukkit.block.Block bukkitBlock = this.world.getWorld().getBlockAt(blockposition.getX(), blockposition.getY(), blockposition.getZ());
                            	
                            	bukkitBlock.setType(org.bukkit.Material.CHEST);
                            	
                            	Chest chest = (Chest) bukkitBlock.getState();
                            	Inventory inventory = chest.getInventory();
                            	HashMap<Integer, org.bukkit.inventory.ItemStack> page = this.chestPage.getPages();
								
                            	chest.setCustomName("에어드랍§r");
                            	chest.update();
                            	
								if (page != null) {
									for (Entry<Integer, org.bukkit.inventory.ItemStack> entry : page.entrySet()) {
										System.out.println(entry.getValue());
										inventory.setItem(entry.getKey(), entry.getValue());
									}
								}
                            }
                            
							// CraftBukkit end
							if (block instanceof BlockFalling) {
							    ((BlockFalling) block).a(this.world, blockposition, this.block, iblockdata);
							}

							if (this.tileEntityData != null && block instanceof ITileEntity) {
							    TileEntity tileentity = this.world.getTileEntity(blockposition);

							    if (tileentity != null) {
							        NBTTagCompound nbttagcompound = tileentity.save(new NBTTagCompound());
							        Iterator<String> iterator = this.tileEntityData.c().iterator();

							        while (iterator.hasNext()) {
							            String s = (String) iterator.next();
							            NBTBase nbtbase = this.tileEntityData.get(s);

							            if (!"x".equals(s) && !"y".equals(s) && !"z".equals(s)) {
							                nbttagcompound.set(s, nbtbase.clone());
							            }
							        }

							        tileentity.load(nbttagcompound);
							        tileentity.update();
							    }
							}
							
							for (int i = 0; i < fallings.size(); i++) {
								EntityFallingBlockEx entityFallingBlock =  (EntityFallingBlockEx) fallings.get(i);
								
								if (!entityFallingBlock.force) {
									if (entityFallingBlock.id < this.id) {
										entityFallingBlock.setLocation(entityFallingBlock.locX, entityFallingBlock.lastY, entityFallingBlock.locZ, 0, 0);
									}
									
									entityFallingBlock.force = true;
									entityFallingBlock.motY = 0;
								}
							}
                        } else if (block instanceof BlockFalling) {
                            ((BlockFalling) block).a_(this.world, blockposition);
                        }
                    }
                }
            }

            this.motX *= 0.9800000190734863D;
            this.motY *= 0.9800000190734863D;
            this.motZ *= 0.9800000190734863D;
            this.velocityChanged = true;
        }
    }

    public void e(float f, float f1) {

    }

    public static void a(DataConverterManager dataconvertermanager) {}

    protected void b(NBTTagCompound nbttagcompound) {
        Block block = this.block != null ? this.block.getBlock() : Blocks.AIR;
        MinecraftKey minecraftkey = (MinecraftKey) Block.REGISTRY.b(block);

        nbttagcompound.setString("Block", minecraftkey == null ? "" : minecraftkey.toString());
        nbttagcompound.setByte("Data", (byte) block.toLegacyData(this.block));
        nbttagcompound.setInt("Time", this.ticksLived);
        nbttagcompound.setBoolean("DropItem", this.dropItem);
        nbttagcompound.setBoolean("HurtEntities", this.hurtEntities);
        if (this.tileEntityData != null) {
            nbttagcompound.set("TileEntityData", this.tileEntityData);
        }

    }

    @SuppressWarnings("deprecation")
	protected void a(NBTTagCompound nbttagcompound) {
        int i = nbttagcompound.getByte("Data") & 255;

        if (nbttagcompound.hasKeyOfType("Block", 8)) {
            this.block = Block.getByName(nbttagcompound.getString("Block")).fromLegacyData(i);
        } else if (nbttagcompound.hasKeyOfType("TileID", 99)) {
            this.block = Block.getById(nbttagcompound.getInt("TileID")).fromLegacyData(i);
        } else {
            this.block = Block.getById(nbttagcompound.getByte("Tile") & 255).fromLegacyData(i);
        }

        this.ticksLived = nbttagcompound.getInt("Time");
        Block block = this.block.getBlock();

        if (nbttagcompound.hasKeyOfType("HurtEntities", 99)) {
            this.hurtEntities = nbttagcompound.getBoolean("HurtEntities");
        } else if (block == Blocks.ANVIL) {
            this.hurtEntities = true;
        }

        if (nbttagcompound.hasKeyOfType("DropItem", 99)) {
            this.dropItem = nbttagcompound.getBoolean("DropItem");
        }

        if (nbttagcompound.hasKeyOfType("TileEntityData", 10)) {
            this.tileEntityData = nbttagcompound.getCompound("TileEntityData");
        }

        if (block == null || block.getBlockData().getMaterial() == Material.AIR) {
            this.block = Blocks.SAND.getBlockData();
        }

    }

    public void a(boolean flag) {
        this.hurtEntities = flag;
    }

    public void appendEntityCrashDetails(CrashReportSystemDetails crashreportsystemdetails) {
        super.appendEntityCrashDetails(crashreportsystemdetails);
        if (this.block != null) {
            Block block = this.block.getBlock();

            crashreportsystemdetails.a("Immitating block ID", (Object) Integer.valueOf(Block.getId(block)));
            crashreportsystemdetails.a("Immitating block data", (Object) Integer.valueOf(block.toLegacyData(this.block)));
        }

    }

    @Nullable
    public IBlockData getBlock() {
        return this.block;
    }

    public boolean bC() {
        return true;
    }
}
