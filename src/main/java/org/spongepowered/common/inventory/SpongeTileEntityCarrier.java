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
package org.spongepowered.common.inventory;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.Container;
import org.spongepowered.api.item.inventory.type.CarriedInventory;
import org.spongepowered.api.world.ServerLocation;
import org.spongepowered.api.world.World;

public class SpongeTileEntityCarrier implements DefaultSingleBlockCarrier {

    private final Container container;
    private final TileEntity inventory;

    public SpongeTileEntityCarrier(Container container, TileEntity inventory) {

        this.container = container;
        this.inventory = inventory;
    }

    @Override
    public ServerLocation getLocation() {
        final BlockPos pos = this.inventory.getPos();
        return ServerLocation.of(((World<?>) this.inventory.getWorld()), pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    @SuppressWarnings("unchecked")
    public CarriedInventory<? extends Carrier> getInventory() {
        return (CarriedInventory) this.container;

    }
}
