package be.isach.ultracosmetics.cosmetics.gadgets;

import be.isach.ultracosmetics.UltraCosmetics;
import be.isach.ultracosmetics.config.MessageManager;
import be.isach.ultracosmetics.cosmetics.Updatable;
import be.isach.ultracosmetics.cosmetics.type.GadgetType;
import be.isach.ultracosmetics.player.UltraPlayer;
import be.isach.ultracosmetics.util.BlockUtils;
import be.isach.ultracosmetics.util.MathUtils;
import be.isach.ultracosmetics.util.Particles;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;

import com.cryptomorin.xseries.XSound;

import java.util.List;

/**
 * Represents an instance of a portal gun gadget summoned by a player.
 *
 * @author iSach
 * @since 08-07-2015
 */
public class GadgetPortalGun extends Gadget implements Updatable {

    private boolean teleported = false;

    private Location locBlue;
    private BlockFace blueBlockFace;

    private Location locRed;
    private BlockFace redBlockFace;

    public GadgetPortalGun(UltraPlayer owner, UltraCosmetics ultraCosmetics) {
        super(owner, GadgetType.valueOf("portalgun"), ultraCosmetics);
        displayCooldownMessage = false;
        useTwoInteractMethods = true;
    }

    @Override
    void onRightClick() {
        XSound.ENTITY_ENDERMAN_TELEPORT.play(getPlayer(), 0.2f, 1.5f);
        Particles.REDSTONE.drawParticleLine(getPlayer().getEyeLocation().add(getPlayer().getEyeLocation().getDirection().multiply(0.6)), getPlayer().getTargetBlock(null, 20).getLocation(), 100, 0, 0, 255);
        locBlue = getPlayer().getTargetBlock(null, 20).getLocation();
        List<Block> b = getPlayer().getLastTwoTargetBlocks(null, 20);
        blueBlockFace = getBlockFace(b.get(0), b.get(1));
        locBlue = locBlue.getBlock().getRelative(blueBlockFace).getLocation().add(0, -0.5, -1);
        if (blueBlockFace == BlockFace.UP || blueBlockFace == BlockFace.DOWN) {
            locBlue.add(0.5, 0.7, 0.5);
        } else if (blueBlockFace == BlockFace.WEST) {
            locBlue.add(0.6, 0.8, 0.5);
        } else if (blueBlockFace == BlockFace.EAST) {
            locBlue.add(.3, 0.8, 0.5);
        } else if (blueBlockFace == BlockFace.NORTH) {
            locBlue.add(0.4, 1.8, 1.75);
        } else if (blueBlockFace == BlockFace.SOUTH) {
            locBlue.add(0.4, 1.8, 1.2);
        }
    }

    @Override
    void onLeftClick() {
        XSound.ENTITY_ENDERMAN_TELEPORT.play(getPlayer(), 0.2f, 1.5f);
        Particles.REDSTONE.drawParticleLine(getPlayer().getEyeLocation().add(getPlayer().getEyeLocation().getDirection().multiply(0.6)), getPlayer().getTargetBlock(null, 20).getLocation(), 100, 255, 0, 0);
        locRed = getPlayer().getTargetBlock(null, 20).getLocation();
        List<Block> b = getPlayer().getLastTwoTargetBlocks(null, 20);
        redBlockFace = getBlockFace(b.get(0), b.get(1));
        locRed = locRed.clone().getBlock().getRelative(redBlockFace).getLocation().add(0, -0.5, -1);
        if (redBlockFace == BlockFace.UP || redBlockFace == BlockFace.DOWN) {
            locRed.add(0.5, 0.7, 0.5);
        } else if (redBlockFace == BlockFace.WEST) {
            locRed.add(0.6, 0.8, 0.5);
        } else if (redBlockFace == BlockFace.EAST) {
            locRed.add(.3, 0.8, 0.5);
        } else if (redBlockFace == BlockFace.NORTH) {
            locRed.add(0.4, 1.8, 1.75);
        } else if (redBlockFace == BlockFace.SOUTH) {
            locRed.add(0.4, 1.8, 1.2);
        }
    }

    public Vector getVectorFromBlockFace(BlockFace bf) {
        Vector v = new Vector(0, 0, 0);
        if (bf == BlockFace.UP) {
            v.add(new Vector(0, 0.3, 0));
        } else if (bf == BlockFace.DOWN) {
            v.add(new Vector(0, -0.3, 0));
        } else if (bf == BlockFace.WEST) {
            v.add(new Vector(-0.3, 0, 0));
        } else if (bf == BlockFace.EAST) {
            v.add(new Vector(0.3, 0, 0));
        } else if (bf == BlockFace.NORTH) {
            v.add(new Vector(-0.3, 0, 0));
        } else if (bf == BlockFace.SOUTH) {
            v.add(new Vector(0.3, 0, 0));
        }
        return v;
    }

    public float getPitch(BlockFace bf) {
        float pitch = 0;
        if (bf == BlockFace.UP) {
            pitch = -90;
        } else if (bf == BlockFace.DOWN) {
            pitch = 90;
        }
        return pitch;
    }

    public float getYaw(BlockFace bf) {
        float yaw = 90;
        if (bf == BlockFace.WEST) {
            yaw = 90;
        } else if (bf == BlockFace.EAST) {
            yaw = -90;
        } else if (bf == BlockFace.NORTH) {
            yaw = 180;
        } else if (bf == BlockFace.SOUTH) {
            yaw = 0;
        }
        return yaw;
    }

    public BlockFace getBlockFace(Block a, Block b) {
        for (BlockFace bf : BlockFace.values()) {
            if (a.getRelative(bf).getLocation().equals(b.getLocation())) {
                return bf.getOppositeFace();
            }
        }
        return null;
    }

    @Override
    public void onUpdate() {
        // TODO: clean this up
        try {
            if (locBlue != null) {
                Location portalCenter = locBlue.clone();
                if (locRed != null && !teleported) {
                    if (!locRed.getWorld().getName().equals(locBlue.getWorld().getName())) {
                        locRed = null;
                        locBlue = null;
                        getPlayer().sendMessage(MessageManager.getMessage("Gadgets.PortalGun.Different-Worlds"));
                        return;
                    }
                    Location toDistance;
                    if (blueBlockFace == BlockFace.DOWN) {
                        toDistance = getPlayer().getEyeLocation().clone();
                    } else if (blueBlockFace == BlockFace.UP) {
                        toDistance = getPlayer().getLocation().clone();
                    } else {
                        toDistance = getPlayer().getLocation().add(0, 1.03, 0);
                    }
                    if (blueBlockFace == BlockFace.UP || blueBlockFace == BlockFace.DOWN) {
                        portalCenter.add(0, 0, 1);
                    } else if (blueBlockFace == BlockFace.NORTH || blueBlockFace == BlockFace.SOUTH) {
                        portalCenter.add(0, -1, 0);
                    } else if (blueBlockFace == BlockFace.EAST || blueBlockFace == BlockFace.WEST) {
                        portalCenter.add(0, 0, 1);
                    }
                    if (toDistance.distance(locBlue) < 1.01) {
                        teleported = true;
                        teleport(getPlayer(), locRed);
                        Location loc = locRed.clone();
                        if (redBlockFace == BlockFace.UP || redBlockFace == BlockFace.DOWN) {
                            loc.setPitch(getPitch(redBlockFace));
                        } else {
                            loc.setYaw(getYaw(redBlockFace));
                        }
                        teleport(getPlayer(), loc);
                        getPlayer().setVelocity(getVectorFromBlockFace(redBlockFace));
                        Bukkit.getScheduler().runTaskLaterAsynchronously(getUltraCosmetics(), () -> teleported = false, 20);
                    }
                }
                Location loc = locBlue.clone();
                for (int i = 0; i < 25; i++) {
                    double inc = (2 * Math.PI) / 20;
                    double angle = i * inc;
                    Vector v = new Vector();
                    v.setX(Math.cos(angle) * 0.3);
                    v.setZ(Math.sin(angle) * 0.3);
                    double x = 0;
                    double z = 0;
                    if (blueBlockFace != BlockFace.UP && blueBlockFace != BlockFace.DOWN) {
                        if (blueBlockFace == BlockFace.EAST || blueBlockFace == BlockFace.WEST) {
                            x = 0;
                            z = 1.5;
                        } else if (blueBlockFace == BlockFace.NORTH || blueBlockFace == BlockFace.SOUTH) {
                            z = 0;
                            x = 1.5;
                        }
                    }
                    MathUtils.rotateVector(v, x, 0, z);
                    Particles.REDSTONE.display(31, 0, 127, loc.add(v));
                }
            }
            if (locRed != null) {
                if (locBlue != null && !teleported) {
                    if (!locRed.getWorld().getName().equals(locBlue.getWorld().getName())) {
                        locRed = null;
                        locBlue = null;
                        getPlayer().sendMessage(MessageManager.getMessage("Gadgets.PortalGun.Different-Worlds"));
                        return;
                    }
                    Location toDistance;
                    if (redBlockFace == BlockFace.DOWN) {
                        toDistance = getPlayer().getEyeLocation().clone();
                    } else if (redBlockFace == BlockFace.UP) {
                        toDistance = getPlayer().getLocation().clone();
                    } else {
                        toDistance = getPlayer().getLocation().add(0, 1.1, 0);
                    }
                    if (toDistance.distance(locRed) < 1.1) {
                        teleported = true;
                        teleport(getPlayer(), locBlue);
                        Location loc = locBlue.clone();
                        if (blueBlockFace == BlockFace.UP || blueBlockFace == BlockFace.DOWN) {
                            loc.setPitch(getPitch(blueBlockFace));
                        } else {
                            loc.setYaw(getYaw(blueBlockFace));
                        }
                        teleport(getPlayer(), loc);
                        getPlayer().setVelocity(getVectorFromBlockFace(blueBlockFace));
                        Bukkit.getScheduler().runTaskLaterAsynchronously(getUltraCosmetics(), () -> teleported = false, 20);
                    }
                }
                Location loc = locRed.clone();
                for (int i = 0; i < 25; i++) {
                    double inc = (2 * Math.PI) / 20;
                    double angle = i * inc;
                    Vector v = new Vector();
                    v.setX(Math.cos(angle) * 0.3);
                    v.setZ(Math.sin(angle) * 0.3);
                    double x = 0;
                    double z = 0;
                    if (redBlockFace != BlockFace.UP
                            && redBlockFace != BlockFace.DOWN) {
                        if (redBlockFace == BlockFace.EAST
                                || redBlockFace == BlockFace.WEST) {
                            x = 0;
                            z = 1.5;
                        } else if (redBlockFace == BlockFace.NORTH
                                || redBlockFace == BlockFace.SOUTH) {
                            z = 0;
                            x = 1.5;
                        }
                    }
                    MathUtils.rotateVector(v, x, 0, z);
                    Particles.REDSTONE.display(255, 0, 0, loc.add(v));
                }
            }
        } catch (IllegalArgumentException ex) {
            // Ignore. Other world.
            locBlue = null;
            locRed = null;
            blueBlockFace = null;
            redBlockFace = null;
        }
    }

    @Override
    protected boolean checkRequirements(PlayerInteractEvent event) {
        if (BlockUtils.isAir(getPlayer().getTargetBlock(null, 20).getType()) || getPlayer().getLastTwoTargetBlocks(null, 20).size() < 2) {
            getPlayer().sendMessage(MessageManager.getMessage("Gadgets.PortalGun.No-Block-Range"));
            return false;
        }
        return true;
    }

    public BlockFace[] axis = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
    public BlockFace[] radial = {BlockFace.NORTH, BlockFace.NORTH_EAST, BlockFace.EAST, BlockFace.SOUTH_EAST, BlockFace.SOUTH, BlockFace.SOUTH_WEST, BlockFace.WEST, BlockFace.NORTH_WEST};

    public BlockFace yawToFace(float yaw) {
        return yawToFace(yaw, true);
    }

    public BlockFace yawToFace(float yaw, boolean useSubCardinalDirections) {
        if (useSubCardinalDirections) {
            return radial[Math.round(yaw / 45f) & 0x7];
        } else {
            return axis[Math.round(yaw / 90f) & 0x3];
        }
    }

    private void teleport(final Entity entity, final Location location) {
        Bukkit.getScheduler().runTask(getUltraCosmetics(), () -> {
            entity.teleport(location);
            if (entity instanceof Player) {
                XSound.ENTITY_ENDERMAN_TELEPORT.play(entity);
            }
        });
    }

    @Override
    public void onClear() {
        locBlue = null;
        locRed = null;
    }
}
