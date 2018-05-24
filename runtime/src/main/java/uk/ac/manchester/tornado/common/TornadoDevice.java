/*
 * This file is part of Tornado: A heterogeneous programming framework: 
 * https://github.com/beehive-lab/tornado
 *
 * Copyright (c) 2013-2018, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Authors: James Clarkson
 *
 */
package uk.ac.manchester.tornado.common;

import uk.ac.manchester.tornado.api.Event;
import uk.ac.manchester.tornado.api.enums.TornadoSchedulingStrategy;

public interface TornadoDevice {

    public TornadoSchedulingStrategy getPreferedSchedule();

    public boolean isDistibutedMemory();

    public void ensureLoaded();

    public CallStack createStack(int numArgs);

    public int ensureAllocated(Object object, DeviceObjectState state);

    public int ensurePresent(Object object, DeviceObjectState objectState);

    public int ensurePresent(Object object, DeviceObjectState objectState, int[] events);

    public int streamIn(Object object, DeviceObjectState objectState);

    public int streamIn(Object object, DeviceObjectState objectState, int[] events);

    public int streamOut(Object object, DeviceObjectState objectState);

    public int streamOut(Object object, DeviceObjectState objectState,
            int[] list);

    public void streamOutBlocking(Object object, DeviceObjectState objectState);

    public void streamOutBlocking(Object object, DeviceObjectState objectState,
            int[] list);

    public TornadoInstalledCode installCode(SchedulableTask task);

    public Event resolveEvent(int event);

    public void markEvent();

    public void flushEvents();

    public int enqueueBarrier();

    public int enqueueBarrier(int[] events);

    public int enqueueMarker();

    public int enqueueMarker(int[] events);

    public void sync();

    public void flush();

    public String getDeviceName();

    public String getDescription();

    public TornadoMemoryProvider getMemoryProvider();

    public void reset();

    public void dumpEvents();

    public void dumpMemory(String file);
    
    public String getPlatformName();

}