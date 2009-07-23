/**
 * $Id: ProviderStackTest.java 3491 2009-02-24 11:40:28Z azeckoski $
 * $URL: https://scm.dspace.org/svn/repo/dspace2/core/trunk/utils/src/test/java/org/dspace/utils/servicemanager/ProviderStackTest.java $
 * ProviderStackTest.java - DS2 - Feb 16, 2009 2:49:36 PM - azeckoski
 **************************************************************************
 * Copyright (c) 2008 Aaron Zeckoski
 * Licensed under the Apache License, Version 2.0
 * 
 * A copy of the Apache License has been included in this 
 * distribution and is available at: http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Aaron Zeckoski (azeckoski @ gmail.com) (aaronz @ vt.edu) (aaron @ caret.cam.ac.uk)
 */

package org.dspace.utils.servicemanager;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dspace.kernel.ServiceManager;
import org.dspace.kernel.mixins.OrderedService;
import org.junit.Test;


/**
 * Tests the usage of the provider stack
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class ProviderStackTest {

    // some stuff for testing
    public interface Provider {
        public String getPrefix();
    }
    public class UnorderedProvider implements Provider {
        protected String prefix;
        public UnorderedProvider(String prefix) {
            this.prefix = prefix;
        }
        public String getPrefix() {
            return prefix;
        }
    }
    public class OrderedProvider extends UnorderedProvider implements OrderedService {
        protected int order = 0;
        public OrderedProvider(String prefix, int order) {
            super(prefix);
            this.order = order;
        }
        public int getOrder() {
            return order;
        }
    }

    /**
     * Test method for {@link org.dspace.utils.servicemanager.ProviderStack#ProviderStack(org.dspace.kernel.ServiceManager, java.lang.Class)}.
     */
    @Test
    public void testProviderStackServiceManagerClassOfT() {
        // fake service manager for testing
        ServiceManager sm = new ServiceManager() {
            public <T> T getServiceByName(String name, Class<T> type) {
                return null;
            }
            public <T> List<T> getServicesByType(Class<T> type) {
                return new ArrayList<T>();
            }
            public List<String> getServicesNames() {
                return new ArrayList<String>();
            }
            public boolean isServiceExists(String name) {
                return false;
            }
            public void pushConfig(Map<String, String> settings) {
            }
            public void registerService(String name, Object service) {
            }
            public <T> T registerServiceClass(String name, Class<T> type) {
                return null;
            }
            public void unregisterService(String name) {
            }
        };
        ProviderStack<Provider> providers = new ProviderStack<Provider>(sm, Provider.class);
        assertNotNull(providers.hashCode());
        assertNotNull(providers.toString());
        assertEquals(0, providers.size());
        assertTrue(providers.getProviders().size() == 0);

        providers.clear();
    }

    /**
     * Test method for {@link org.dspace.utils.servicemanager.ProviderStack#ProviderStack(T[])}.
     */
    @Test
    public void testProviderStackTArray() {
        ProviderStack<Provider> providers = new ProviderStack<Provider>(new Provider[] {
                new UnorderedProvider("ccc"),
                new UnorderedProvider("ddd"),
                new OrderedProvider("bbb", 5),
                new OrderedProvider("aaa", 2)
        });
        assertNotNull(providers.hashCode());
        assertNotNull(providers.toString());
        assertEquals(4, providers.size());
        // check the order
        List<Provider> l = providers.getProviders();
        assertEquals("aaa", l.get(0).getPrefix());
        assertEquals("bbb", l.get(1).getPrefix());
        assertEquals("ccc", l.get(2).getPrefix());
        assertEquals("ddd", l.get(3).getPrefix());

        l = null;
        providers.clear();
    }

    /**
     * Test method for {@link org.dspace.utils.servicemanager.ProviderStack#addProvider(java.lang.Object)}.
     */
    @Test
    public void testAddProvider() {
        // preload
        ProviderStack<Provider> providers = new ProviderStack<Provider>(new Provider[] {
                new UnorderedProvider("ccc"),
                new UnorderedProvider("ddd"),
                new OrderedProvider("bbb", 5),
                new OrderedProvider("aaa", 2)
        });
        assertNotNull(providers.hashCode());
        assertNotNull(providers.toString());
        assertEquals(4, providers.size());
        // check the order
        List<Provider> l = providers.getProviders();
        assertEquals("aaa", l.get(0).getPrefix());
        assertEquals("bbb", l.get(1).getPrefix());
        assertEquals("ccc", l.get(2).getPrefix());
        assertEquals("ddd", l.get(3).getPrefix());

        // now do some adds

        // unordered should go to the end
        Provider p5 = new UnorderedProvider("eee");
        providers.addProvider( p5 );
        assertEquals(5, providers.size());
        l = providers.getProviders();
        assertEquals("aaa", l.get(0).getPrefix());
        assertEquals("bbb", l.get(1).getPrefix());
        assertEquals("ccc", l.get(2).getPrefix());
        assertEquals("ddd", l.get(3).getPrefix());
        assertEquals("eee", l.get(4).getPrefix());

        // ordered should go in order
        Provider p6 = new OrderedProvider("ab6", 3);
        providers.addProvider( p6 );
        assertEquals(6, providers.size());
        l = providers.getProviders();
        assertEquals("aaa", l.get(0).getPrefix());
        assertEquals("ab6", l.get(1).getPrefix());
        assertEquals("bbb", l.get(2).getPrefix());
        assertEquals("ccc", l.get(3).getPrefix());
        assertEquals("ddd", l.get(4).getPrefix());
        assertEquals("eee", l.get(5).getPrefix());

        Provider p7 = new OrderedProvider("bc7", 6);
        providers.addProvider( p7 );
        assertEquals(7, providers.size());
        l = providers.getProviders();
        assertEquals("aaa", l.get(0).getPrefix());
        assertEquals("ab6", l.get(1).getPrefix());
        assertEquals("bbb", l.get(2).getPrefix());
        assertEquals("bc7", l.get(3).getPrefix());
        assertEquals("ccc", l.get(4).getPrefix());
        assertEquals("ddd", l.get(5).getPrefix());
        assertEquals("eee", l.get(6).getPrefix());

        l = null;
        providers.clear();
    }

    /**
     * Test method for {@link org.dspace.utils.servicemanager.ProviderStack#removeProvider(int)}.
     */
    @Test
    public void testRemoveProvider() {
        // preload
        ProviderStack<Provider> providers = new ProviderStack<Provider>(new Provider[] {
                new UnorderedProvider("ccc"),
                new UnorderedProvider("ddd"),
                new OrderedProvider("bbb", 5),
                new OrderedProvider("aaa", 2)
        });
        assertNotNull(providers.hashCode());
        assertNotNull(providers.toString());
        assertEquals(4, providers.size());

        providers.removeProvider(1);
        assertEquals(3, providers.size());
        List<Provider> l = providers.getProviders();
        assertEquals("aaa", l.get(0).getPrefix());
        assertEquals("ccc", l.get(1).getPrefix());
        assertEquals("ddd", l.get(2).getPrefix());

        // test no failure for invalid remove
        providers.removeProvider(5);

        l = null;
        providers.clear();
    }

    /**
     * Test method for {@link org.dspace.utils.servicemanager.ProviderStack#getProviders()}.
     */
    @Test
    public void testGetProviders() {
        // preload
        ProviderStack<Provider> providers = new ProviderStack<Provider>(new Provider[] {
                new UnorderedProvider("ccc"),
                new UnorderedProvider("ddd"),
                new OrderedProvider("bbb", 5),
                new OrderedProvider("aaa", 2)
        });
        assertNotNull(providers.hashCode());
        assertNotNull(providers.toString());
        assertEquals(4, providers.size());

        List<Provider> l = providers.getProviders();
        assertNotNull(l);
        assertEquals(4, l.size());
        l = null;

        providers = new ProviderStack<Provider>();
        l = providers.getProviders();
        assertNotNull(l);
        assertEquals(0, l.size());

        l = null;
        providers.clear();
    }

    /**
     * Test method for {@link org.dspace.utils.servicemanager.ProviderStack#getIterator()}.
     */
    @Test
    public void testGetIterator() {
        // preload
        ProviderStack<Provider> providers = new ProviderStack<Provider>(new Provider[] {
                new UnorderedProvider("ccc"),
                new UnorderedProvider("ddd"),
                new OrderedProvider("bbb", 5),
                new OrderedProvider("aaa", 2)
        });
        assertNotNull(providers.hashCode());
        assertNotNull(providers.toString());
        assertEquals(4, providers.size());

        Iterator<Provider> it = providers.getIterator();
        assertNotNull(it);
        assertTrue(it.hasNext());
        assertNotNull(it.next());
        assertTrue(it.hasNext());
        assertNotNull(it.next());
        assertTrue(it.hasNext());
        assertNotNull(it.next());
        assertTrue(it.hasNext());
        assertNotNull(it.next());
        assertFalse(it.hasNext());

        providers = new ProviderStack<Provider>();
        it = providers.getIterator();
        assertNotNull(it);
        assertFalse(it.hasNext());

        it = null;
        providers.clear();
    }

    /**
     * Test method for {@link org.dspace.utils.servicemanager.ProviderStack#getProvider(int)}.
     */
    @Test
    public void testGetProvider() {
        // preload
        ProviderStack<Provider> providers = new ProviderStack<Provider>(new Provider[] {
                new UnorderedProvider("ccc"),
                new UnorderedProvider("ddd"),
                new OrderedProvider("bbb", 5),
                new OrderedProvider("aaa", 2)
        });
        assertNotNull(providers.hashCode());
        assertNotNull(providers.toString());
        assertEquals(4, providers.size());

        assertEquals("aaa", providers.getProvider(0).getPrefix());
        assertEquals("bbb", providers.getProvider(1).getPrefix());
        assertEquals("ccc", providers.getProvider(2).getPrefix());
        assertEquals("ddd", providers.getProvider(3).getPrefix());

        assertEquals(null, providers.getProvider(4));
        assertEquals(null, providers.getProvider(5));
        assertEquals(null, providers.getProvider(-1));

        providers.clear();
    }

    /**
     * Test method for {@link org.dspace.utils.servicemanager.ProviderStack#size()}.
     */
    @Test
    public void testSize() {
        // preload
        ProviderStack<Provider> providers = new ProviderStack<Provider>(new Provider[] {
                new UnorderedProvider("ccc"),
                new UnorderedProvider("ddd"),
                new OrderedProvider("bbb", 5),
                new OrderedProvider("aaa", 2)
        });
        assertNotNull(providers.hashCode());
        assertNotNull(providers.toString());
        assertEquals(4, providers.size());

        providers.clear();
    }

    /**
     * Test method for {@link org.dspace.utils.servicemanager.ProviderStack#clear()}.
     */
    @Test
    public void testClear() {
        // preload
        ProviderStack<Provider> providers = new ProviderStack<Provider>(new Provider[] {
                new UnorderedProvider("ccc"),
                new UnorderedProvider("ddd"),
                new OrderedProvider("bbb", 5),
                new OrderedProvider("aaa", 2)
        });
        assertNotNull(providers.hashCode());
        assertNotNull(providers.toString());
        assertEquals(4, providers.size());

        providers.clear();
        assertNotNull(providers.hashCode());
        assertNotNull(providers.toString());
        assertEquals(0, providers.size());

        providers.clear();
    }

    /**
     * Test method for {@link org.dspace.utils.servicemanager.ProviderStack#refresh()}.
     */
    @Test
    public void testRefresh() {
        Provider p1 = new OrderedProvider("aaa", 2);
        Provider p2 = new UnorderedProvider("ccc");
        ProviderStack<Provider> providers = new ProviderStack<Provider>(new Provider[] {
                p2,
                new UnorderedProvider("ddd"),
                new OrderedProvider("bbb", 5),
                p1
        });

        assertNotNull(providers.hashCode());
        assertNotNull(providers.toString());
        assertEquals(4, providers.size());

        providers.refresh();
        assertEquals(4, providers.size());

        System.gc(); // should wipe the 2 without a reference
        providers.refresh();
        assertEquals(2, providers.size());
        assertEquals(p1, providers.getProvider(0));
        assertEquals(p2, providers.getProvider(1));

        p1 = null;
        System.gc(); // should wipe the p1
        providers.refresh();
        assertEquals(1, providers.size());
        assertEquals(p2, providers.getProvider(0));

        providers.clear();
    }

}