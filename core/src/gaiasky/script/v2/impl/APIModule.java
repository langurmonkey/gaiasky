/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2.impl;

import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.util.Logger;
import gaiasky.util.TextUtils;

import static gaiasky.util.Logger.getLogger;

/**
 * Abstract class which contains attributes and methods common to all API modules.
 */
public abstract class APIModule {
    protected final Logger.Log logger;

    /** Reference to self. **/
    protected final APIModule me;
    /** Reference to event manager. **/
    protected final EventManager em;
    /** Reference to API object. **/
    protected final APIv2 api;
    /** Module name. **/
    protected final String name;

    /**
     * Create a new module with the given attributes.
     *
     * @param api  Reference to the API class.
     * @param name Name of the module.
     */
    public APIModule(EventManager em, APIv2 api, String name) {
        super();
        this.em = em;
        this.api = api;
        this.name = name;

        this.logger = getLogger(TextUtils.capitalise(name) + "Module");

        this.me = this;
    }

    /**
     * Method called whenever the module is disposed. To be overwritten if necessary.
     */
    public void dispose() {
        // Remove subscriptions if needed.
        if (me instanceof IObserver observer)
            em.removeAllSubscriptions(observer);
    }
}
