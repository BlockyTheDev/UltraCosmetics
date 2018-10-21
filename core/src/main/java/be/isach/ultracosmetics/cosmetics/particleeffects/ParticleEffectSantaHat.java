package be.isach.ultracosmetics.cosmetics.particleeffects;

import be.isach.ultracosmetics.UltraCosmetics;
import be.isach.ultracosmetics.cosmetics.type.ParticleEffectType;
import be.isach.ultracosmetics.player.UltraPlayer;
import be.isach.ultracosmetics.util.MathUtils;
import be.isach.ultracosmetics.util.UtilParticles;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;

/**
 * Represents an instance of  particles summoned by a player.
 *
 * @author iSach
 * @since 11-28-2015
 */
public class ParticleEffectSantaHat extends ParticleEffect {

	public int particles = 12;

	public ParticleEffectSantaHat(UltraPlayer owner, UltraCosmetics ultraCosmetics) {
		super(ultraCosmetics, owner, ParticleEffectType.valueOf("santahat"));
		this.ignoreMove = true;
	}

	@Override
	public void onUpdate() {
		Location location = getPlayer().getEyeLocation().add(0, 0.3, 0);
		float radius = 0.25f;
		drawCircle(radius + 0.1f, -0.05f, location, false);
		for (int i = 0; i < 5; i++) {
			double x = MathUtils.randomDouble(-0.05, 0.05);
			double z = MathUtils.randomDouble(-0.05, 0.05);
			location.add(x, 0.46f, z);
			location.getWorld().spawnParticle(Particle.REDSTONE, location, 1, new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1));
			location.subtract(x, 0.46f, z);
		}
		for (float f = 0; f <= 0.4f; f += 0.1f) {
			if (radius >= 0) {
				drawCircle(radius, f, location, true);
				radius -= 0.09f;
			}
		}
	}

	private void drawCircle(float radius, float height, Location location, boolean red) {
		for (int i = 0; i < particles; i++) {
			double inc = (2 * Math.PI) / particles;
			float angle = (float) (i * inc);
			float x = MathUtils.cos(angle) * radius;
			float z = MathUtils.sin(angle) * radius;
			location.add(x, height, z);
			location.getWorld().spawnParticle(Particle.REDSTONE, location, 1, new Particle.DustOptions(Color.fromRGB(255, red ? 0 : 255, red ? 0 : 255), 1));
			location.subtract(x, height, z);
		}
	}
}
