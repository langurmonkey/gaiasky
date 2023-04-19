/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gaia;

import gaiasky.util.LruCache;

import java.util.Date;
import java.util.Map;

public class AttitudeIntervalBean implements Comparable<AttitudeIntervalBean> {
    public String name;
    public Date activationTime;
    public String file;
    public BaseAttitudeDataServer attitude;

    public Map<Long, IAttitude> cache;
    public long hits = 0, misses = 0;

    public AttitudeIntervalBean(String name, Date activationTime, BaseAttitudeDataServer attitude, String file) {
        this.file = file;
        this.name = name;
        this.activationTime = activationTime;
        this.attitude = attitude;

        cache = new LruCache<>(10);
    }

    public synchronized IAttitude get(Date date) {
        Long time = date.getTime();
        if (!cache.containsKey(time)) {
            IAttitude att = attitude.getAttitude(date);
            cache.put(time, att);
            misses++;
            return att;
        } else {
            hits++;
        }
        return cache.get(time);
    }

    @Override
    public int compareTo(AttitudeIntervalBean o) {
        return this.activationTime.compareTo(o.activationTime);
    }

    @Override
    public String toString() {
        return name;
    }
}
