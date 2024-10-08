/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ct.buildcraft.core.marker;

import java.util.List;

import ct.buildcraft.core.BCCoreConfig;
import ct.buildcraft.core.client.BuildCraftLaserManager;
import ct.buildcraft.lib.client.render.laser.LaserData_BC8.LaserType;
import ct.buildcraft.lib.marker.MarkerCache;
import ct.buildcraft.lib.marker.MarkerSubCache;
import ct.buildcraft.lib.net.MessageMarker;
import com.google.common.collect.ImmutableList;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public class PathSubCache extends MarkerSubCache<PathConnection> {
    public PathSubCache(ServerLevel world) {
        super(world, MarkerCache.CACHES.indexOf(PathCache.INSTANCE));
        PathSavedData data = (PathSavedData) world.getDataStorage()
        		.computeIfAbsent(PathSavedData::new, PathSavedData::new, PathSavedData.NAME);
        data.loadInto(this);
    }

    @Override
    public boolean tryConnect(BlockPos from, BlockPos to) {
        PathConnection conFrom = getConnection(from);
        PathConnection conTo = getConnection(to);
        if (conFrom == null) {
            if (conTo == null) {
                return PathConnection.tryCreateConnection(this, from, to);
            } else {
                return conTo.addMarker(from, to);
            }
        } else {
            if (conTo == null) {
                return conFrom.addMarker(from, to);
            } else {
                return conFrom.mergeWith(conTo, from, to);
            }
        }
    }

    @Override
    public boolean canConnect(BlockPos from, BlockPos to) {
        PathConnection conFrom = getConnection(from);
        PathConnection conTo = getConnection(to);
        if (conFrom == null) {
            if (conTo == null) {
                return true;
            } else {
                return conTo.canAddMarker(from, to);
            }
        } else {
            if (conTo == null) {
                return conFrom.canAddMarker(from, to);
            } else {
                return conFrom.canMergeWith(conTo, from, to);
            }
        }
    }

    @Override
    public ImmutableList<BlockPos> getValidConnections(BlockPos from) {
        ImmutableList.Builder<BlockPos> list = ImmutableList.builder();
        final int maxLengthSquared = BCCoreConfig.markerMaxDistance * BCCoreConfig.markerMaxDistance;
        for (BlockPos pos : getAllMarkers()) {
            if (pos.equals(from)) {
                continue;
            }
            if (pos.distSqr(from) > maxLengthSquared) {
                continue;
            }
            if (canConnect(from, pos) || canConnect(pos, from)) {
                list.add(pos);
            }
        }
        return list.build();
    }

    @Override
    public LaserType getPossibleLaserType() {
        return null;//BuildCraftLaserManager.MARKER_PATH_POSSIBLE;
    }

    @Override
    protected boolean handleMessage(MessageMarker message) {
        List<BlockPos> positions = message.positions;
        if (message.connection) {
            if (message.add) {
                for (BlockPos p : positions) {
                    PathConnection existing = this.getConnection(p);
                    destroyConnection(existing);
                }
                PathConnection con = new PathConnection(this, positions);
                addConnection(con);
            } else { // removing from a connection
                for (BlockPos p : positions) {
                    PathConnection existing = this.getConnection(p);
                    if (existing != null) {
                        existing.removeMarker(p);
                        refreshConnection(existing);
                    }
                }
            }
        }
        return false;
    }
}
