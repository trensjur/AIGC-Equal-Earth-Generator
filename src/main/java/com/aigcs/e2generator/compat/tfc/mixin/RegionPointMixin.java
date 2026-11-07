package com.aigcs.e2generator.compat.tfc.mixin;

import com.aigcs.e2generator.compat.tfc.TfcPointAccess;
import com.aigcs.e2generator.compat.tfc.TfcShapeBridge;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在地图外取消 TFC 放置陆地/岛屿/山脉/火山/屏障岛的标记，避免大陆与结构
 * 越过 Equal Earth 边界。
 * <p>
 * 目标类以字符串形式声明，编译期不需要 TFC。
 */
@Mixin(targets = "net.dries007.tfc.world.region.Region$Point")
public abstract class RegionPointMixin implements TfcPointAccess {

    @Shadow
    public int x;

    @Shadow
    public int z;

    @Override
    public int spherical$gridX() {
        return this.x;
    }

    @Override
    public int spherical$gridZ() {
        return this.z;
    }

    @Inject(method = {
            "setLand",
            "setIsland",
            "setMountain",
            "setCoastalMountain",
            "setVolcanic",
            "setBarrierIsland"
    }, at = @At("HEAD"), cancellable = true)
    private void spherical$cancelOutsideMap(CallbackInfo ci) {
        if (TfcShapeBridge.shouldCancelPoint(this.x, this.z)) {
            ci.cancel();
        }
    }
}
