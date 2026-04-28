package dlindustries.vigillant.system.mixin;

import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WorldRenderer.class)
public interface WorldRendererAccessor {
	@Accessor("chunks")
	BuiltChunkStorage getChunks();

	@Accessor("capturedFrustum")
	Frustum getFrustum();

	@Accessor("capturedFrustum")
	void setFrustum(Frustum frustum);
}
