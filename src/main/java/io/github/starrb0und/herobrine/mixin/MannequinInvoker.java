package io.github.starrb0und.herobrine.mixin;

import net.minecraft.world.entity.decoration.Mannequin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Mannequin.class)
public interface MannequinInvoker {
	/** Hides the "NPC" label mannequins show under their name. */
	@Invoker("setHideDescription")
	void herobrine$setHideDescription(boolean hide);
}
