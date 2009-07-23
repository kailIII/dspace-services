/**
 * $Id: SystemEventService.java 3523 2009-03-05 14:58:10Z azeckoski $
 * $URL: https://scm.dspace.org/svn/repo/dspace2/core/trunk/impl/src/main/java/org/dspace/services/events/SystemEventService.java $
 * TempEventService.java - DS2 - Nov 19, 2008 4:02:31 PM - azeckoski
 **************************************************************************
 * Copyright (c) 2008 Aaron Zeckoski
 * Licensed under the Apache License, Version 2.0
 * 
 * A copy of the Apache License has been included in this 
 * distribution and is available at: http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Aaron Zeckoski (azeckoski @ gmail.com) (aaronz @ vt.edu) (aaron @ caret.cam.ac.uk)
 */

package org.dspace.services.events;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.azeckoski.reflectutils.ArrayUtils;
import org.azeckoski.reflectutils.refmap.ReferenceMap;
import org.azeckoski.reflectutils.refmap.ReferenceType;
import org.dspace.kernel.mixins.ShutdownService;
import org.dspace.services.CachingService;
import org.dspace.services.EventService;
import org.dspace.services.RequestService;
import org.dspace.services.SessionService;
import org.dspace.services.model.Cache;
import org.dspace.services.model.CacheConfig;
import org.dspace.services.model.Event;
import org.dspace.services.model.EventListener;
import org.dspace.services.model.RequestInterceptor;
import org.dspace.services.model.Session;
import org.dspace.services.model.CacheConfig.CacheScope;
import org.dspace.services.model.Event.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This is a placeholder until we get a real event service going,
 * it does pretty much everything the service should do EXCEPT sending the events
 * across a cluster
 * 
 * @author Aaron Zeckoski (azeckoski@gmail.com) - azeckoski - 4:02:31 PM Nov 19, 2008
 */
public class SystemEventService implements EventService, ShutdownService {

    private final Logger log = LoggerFactory.getLogger(SystemEventService.class);

    private static final String QUEUE_CACHE_NAME = "eventQueueCache";
    
    /**
     * map for holding onto the listeners which is ClassLoader safe
     */
    private ReferenceMap<String, EventListener> listenersMap = new ReferenceMap<String, EventListener>(ReferenceType.STRONG, ReferenceType.WEAK);

    private final RequestService requestService;
    private final SessionService sessionService;
    private final CachingService cachingService;
    private EventRequestInterceptor requestInterceptor;
    private Cache queueCache;

    @Autowired(required=true)
    public SystemEventService(RequestService requestService, SessionService sessionService, CachingService cachingService) {
        if (requestService == null || cachingService == null || sessionService == null) {
            throw new IllegalArgumentException("requestService, cachingService, and all inputs must not be null");
        }
        this.requestService = requestService;
        this.sessionService = sessionService;
        this.cachingService = cachingService;

        // create the cache
        this.queueCache = this.cachingService.getCache(QUEUE_CACHE_NAME, new CacheConfig(CacheScope.REQUEST));

        // register interceptor
        this.requestInterceptor = new EventRequestInterceptor();
        this.requestService.registerRequestInterceptor(this.requestInterceptor);
    }

    /* (non-Javadoc)
     * @see org.dspace.kernel.mixins.ShutdownService#shutdown()
     */
    public void shutdown() {
        this.requestInterceptor = null; // clear the interceptor
        this.listenersMap.clear();
        this.queueCache.clear();
    }


    /* (non-Javadoc)
     * @see org.dspace.services.EventService#fireEvent(org.dspace.services.model.Event)
     */
    public void fireEvent(Event event) {
        validateEvent(event);
        // check scopes for this event
        Scope[] scopes = event.getScopes();
        boolean local = ArrayUtils.contains(scopes, Scope.LOCAL);
        if (local) {
            fireLocalEvent(event);
        }
        boolean cluster = ArrayUtils.contains(scopes, Scope.CLUSTER);
        if (cluster) {
            fireClusterEvent(event);
        }
        boolean external = ArrayUtils.contains(scopes, Scope.EXTERNAL);
        if (external) {
            fireExternalEvent(event);
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.services.EventService#queueEvent(org.dspace.services.model.Event)
     */
    public void queueEvent(Event event) {
        validateEvent(event);
        // put the event in the queue if this is in a request
        if (requestService.getCurrentRequestId() != null) {
            // create a key which is orderable and unique
            String key = System.currentTimeMillis() + ":" + this.queueCache.size() + ":" + event.getId();
            this.queueCache.put(key, event);
        } else {
            // no request so fire the event immediately
            log.info("No request to queue this event ("+event+") so firing immediately");
            fireEvent(event);
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.services.EventService#registerEventListener(org.dspace.services.model.EventListener)
     */
    public void registerEventListener(EventListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("event listener cannot be null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("Cannot register a listener that is null");
        }
        String key = listener.getClass().getName();
        this.listenersMap.put(key, listener);
    }


    /**
     * Fires off a local event immediately,
     * this is internal so the event should have already been validated
     * @param event a valid event
     */
    private void fireLocalEvent(Event event) {
        // send event to all interested listeners
        for (EventListener listener : listenersMap.values()) {
            if (listener != null) {
                // filter the event if the listener has filter rules
                if ( filterEvent(listener, event) ) {
                    // passed filters so send the event to this listener
                    try {
                        listener.receiveEvent(event);
                    } catch (Exception e) {
                        log.warn("Listener ("+listener+")["+listener.getClass().getName()+"] failed to recieve event ("+event+"): " + e.getMessage() + ":" + e.getCause());
                    }
                }
            }
        }
    }

    /**
     * Will eventually fire events to the entire cluster
     * TODO not implemented
     * 
     * @param event a validated event
     */
    private void fireClusterEvent(Event event) {
        log.warn("fireClusterEvent is not implemented yet, no support for cluster events yet, could not fire event to the cluster: " + event);
    }

    /**
     * Will eventually fire events to external systems
     * TODO not implemented
     * 
     * @param event a validated event
     */
    private void fireExternalEvent(Event event) {
        log.warn("fireExternalEvent is not implemented yet, no support for external events yet, could not fire event to external listeners: " + event);
    }

    /**
     * Fires all queued events for the current request
     * 
     * @return the number of events which were fired
     */
    protected int fireQueuedEvents() {
        int fired = 0;
        List<String> eventIds = this.queueCache.getKeys();
        Collections.sort(eventIds); // put it in the order they were added (hopefully)
        if (eventIds.size() > 0) {
            for (String eventId : eventIds) {
                Event event = (Event) this.queueCache.get(eventId);
                fireEvent(event);
                fired++;
            }
        }
        this.queueCache.clear();
        return fired;
    }

    /**
     * Clears all events for the current request
     * 
     * @return the number of events that were cleared
     */
    protected int clearQueuedEvents() {
        int cleared = this.queueCache.size();
        this.queueCache.clear();
        return cleared;
    }

    /**
     * This will validate the event object and set any values which are unset but can be figured out
     * 
     * @param event the event which is being sent into the system
     */
    private void validateEvent(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("Cannot fire null events");
        }
        if (event.getName() == null || "".equals(event.getName()) ) {
            throw new IllegalArgumentException("Event name must be set");
        }
        if (event.getId() == null || "".equals(event.getId()) ) {
            // generate an id then
            event.setId(makeEventId());
        }
        if (event.getUserId() == null || "".equals(event.getUserId()) ) {
            // set to the current user
            String userId = this.sessionService.getCurrentUserId();
            event.setUserId(userId);
        }
        if (event.getScopes() == null) {
            // set to local/cluster scope
            event.setScopes( new Event.Scope[] {Scope.LOCAL, Scope.CLUSTER});
        }
    }

    /**
     * Checks to see if the filtering in the given listener allows the event to be received
     * 
     * @param listener an event listener
     * @param event an event
     * @return true if the event should be received, false if the event is filtered out
     */
    private boolean filterEvent(EventListener listener, Event event) {
        if (listener == null || event == null) {
            return false;
        }
        // filter the event if the listener has filter rules
        boolean allowName = true;
        try {
            String[] namePrefixes = listener.getEventNamePrefixes();
            if (namePrefixes != null && namePrefixes.length > 0) {
                allowName = false;
                for (int i = 0; i < namePrefixes.length; i++) {
                    String namePrefix = namePrefixes[i];
                    String eventName = event.getName();
                    if (namePrefix != null && namePrefix.length() > 0) {
                        if (eventName.startsWith(namePrefix)) {
                            allowName = true;
                            break;
                        }
                    }
                }
            }
        } catch (Exception e1) {
            log.warn("Listener ("+listener+")["+listener.getClass().getName()+"] failure calling getEventNamePrefixes: " + e1.getMessage() + ":" + e1.getCause());
        }
        boolean allowResource = true;
        try {
            String resourcePrefix = listener.getResourcePrefix();
            if (resourcePrefix != null && resourcePrefix.length() > 0) {
                allowResource = false;
                String resRef = event.getResourceReference();
                if (resRef == null) {
                    // null references default to unfiltered
                    allowResource = true;
                } else {
                    if (resRef.startsWith(resourcePrefix)) {
                        allowResource = true;
                    }
                }
            }
        } catch (Exception e1) {
            log.warn("Listener ("+listener+")["+listener.getClass().getName()+"] failure calling getResourcePrefix: " + e1.getMessage() + ":" + e1.getCause());
        }
        boolean allowed = allowName && allowResource;
        return allowed;
    }

    private Random random = new Random();
    /**
     * @return a generated event Id used to identify and track this event uniquely
     */
    private String makeEventId() {
        return "event-" + random.nextInt(1000) + "-" + System.currentTimeMillis();
    }

    /**
     * The request interceptor for the event service,
     * this will take care of firing queued events at the end of the request
     * 
     * @author Aaron Zeckoski (azeckoski@gmail.com) - azeckoski - 10:24:58 AM Nov 20, 2008
     */
    public class EventRequestInterceptor implements RequestInterceptor {

        /* (non-Javadoc)
         * @see org.dspace.services.model.RequestInterceptor#onStart(java.lang.String, org.dspace.services.model.Session)
         */
        public void onStart(String requestId, Session session) {
            // nothing to really do here unless we decide we should purge out any existing events? -AZ
        }

        /* (non-Javadoc)
         * @see org.dspace.services.model.RequestInterceptor#onEnd(java.lang.String, org.dspace.services.model.Session, boolean, java.lang.Exception)
         */
        public void onEnd(String requestId, Session session, boolean succeeded, Exception failure) {
            if (succeeded) {
                int fired = fireQueuedEvents();
                log.info("Fired "+fired+" events at the end of the request ("+requestId+")");
            } else {
                int cleared = clearQueuedEvents();
                log.info("Cleared/cancelled "+cleared+" events at the end of the failed request ("+requestId+")");
            }
        }

        /* (non-Javadoc)
         * @see org.dspace.kernel.mixins.OrderedService#getOrder()
         */
        public int getOrder() {
            return 20; // this should fire pretty late
        }

    }

}