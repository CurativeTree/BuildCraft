package ct.buildcraft;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent.Pre;

public class BCFactorySprites {

    public static final ResourceLocation PUMPTUBE = new ResourceLocation("buildcraftfactory:blocks/pump/tube");
    public static final ResourceLocation MININGTUBE = new ResourceLocation("buildcraftfactory:blocks/mining_well/tube");
    
    public static final ResourceLocation DISTILLER_POWER_A = new ResourceLocation("buildcraftfactory:blocks/distiller/power_sprite_a");
    public static final ResourceLocation DISTILLER_POWER_B = new ResourceLocation("buildcraftfactory:blocks/distiller/power_sprite_b");
    public static final ResourceLocation DISTILLER_POWER_C = new ResourceLocation("buildcraftfactory:blocks/distiller/power_sprite_c");
    public static final ResourceLocation DISTILLER_POWER_D = new ResourceLocation("buildcraftfactory:blocks/distiller/power_sprite_d");
    
    public static final ResourceLocation AUTO_BENCH_GUI = new ResourceLocation("buildcraftfactory:textures/gui/autobench_item.png");
    
    
    public static void registrtTexture(Pre e){
    	e.addSprite(PUMPTUBE);
    	e.addSprite(MININGTUBE);
		e.addSprite(DISTILLER_POWER_A);
		e.addSprite(DISTILLER_POWER_B);
		e.addSprite(DISTILLER_POWER_C);
		e.addSprite(DISTILLER_POWER_D);
	}

}
