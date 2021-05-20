/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.forge.mixin.core.client.main;

import net.minecraft.client.main.Main;
import net.minecraftforge.fml.loading.FMLLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.applaunch.config.core.SpongeConfigs;
import org.spongepowered.common.launch.Launch;
import org.spongepowered.forge.launch.ForgeLaunch;
import org.spongepowered.forge.launch.plugin.ForgePluginEngine;
import org.spongepowered.plugin.Blackboard;
import org.spongepowered.plugin.PluginEnvironment;
import org.spongepowered.plugin.PluginKeys;

@Mixin(Main.class)
public class MainMixin_Forge {

    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void forge$initLaunch(final CallbackInfo ci) {
        // this is wrong, we need to get set up from within a non-transformable class that is on the TransformingClassLoader
        // otherwise configs die
        final ForgeLaunch launch = new ForgeLaunch(new ForgePluginEngine(new PluginEnvironment()));
        final Blackboard blackboard = launch.getPluginEngine().getPluginEnvironment().blackboard();
        final String implementationVersion = PluginEnvironment.class.getPackage().getImplementationVersion();

        blackboard.getOrCreate(PluginKeys.VERSION, () -> implementationVersion == null ? "dev" : implementationVersion);
        blackboard.getOrCreate(SpongeConfigs.IS_VANILLA_PLATFORM, () -> false);
        blackboard.getOrCreate(PluginKeys.BASE_DIRECTORY, FMLLoader::getGamePath);
        // todo: do this properly, break it out to work somewhere on server?

        Launch.setInstance(launch);
        SpongeConfigs.initialize(launch.getPluginEngine().getPluginEnvironment());
    }

}