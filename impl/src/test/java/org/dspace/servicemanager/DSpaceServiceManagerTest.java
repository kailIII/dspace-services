/**
 * $Id: DSpaceServiceManagerTest.java 3409 2009-01-30 12:04:43Z azeckoski $
 * $URL: https://scm.dspace.org/svn/repo/dspace2/core/trunk/impl/src/test/java/org/dspace/servicemanager/DSpaceServiceManagerTest.java $
 * DSpaceServiceManagerTest.java - DSpace2 - Oct 6, 2008 4:00:53 AM - azeckoski
 **************************************************************************
 * Copyright (c) 2008 Aaron Zeckoski
 * Licensed under the Apache License, Version 2.0
 * 
 * A copy of the Apache License has been included in this 
 * distribution and is available at: http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Aaron Zeckoski (azeckoski @ gmail.com) (aaronz @ vt.edu) (aaron @ caret.cam.ac.uk)
 */

package org.dspace.servicemanager;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dspace.kernel.mixins.InitializedService;
import org.dspace.kernel.mixins.ShutdownService;
import org.dspace.servicemanager.config.DSpaceConfigurationService;
import org.dspace.servicemanager.example.ConcreteExample;
import org.dspace.servicemanager.fakeservices.FakeService1;
import org.dspace.servicemanager.fakeservices.FakeService2;
import org.dspace.servicemanager.spring.SpringAnnotationBean;
import org.dspace.servicemanager.spring.TestSpringServiceManager;
import org.dspace.services.ConfigurationService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * testing the main dspace service manager
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class DSpaceServiceManagerTest {

    DSpaceServiceManager dsm;
    DSpaceConfigurationService configurationService;

    @Before
    public void init() {
        configurationService = new DSpaceConfigurationService();
        configurationService.loadConfig("testName@" + SampleAnnotationBean.class.getName(), "beckyz");
        configurationService.loadConfig("fakeParam@fakeBean", "beckyz");

        dsm = new DSpaceServiceManager(configurationService, TestSpringServiceManager.SPRING_TEST_CONFIG_FILE);
    }

    @After
    public void shutdown() {
        if (dsm != null) {
            dsm.shutdown();
        }
        dsm = null;
        configurationService = null;
    }

    /**
     * Test method for {@link org.dspace.servicemanager.DSpaceServiceManager#shutdown()}.
     */
    @Test
    public void testShutdown() {
        dsm.startup();
        dsm.shutdown();
    }

    /**
     * Test method for {@link org.dspace.servicemanager.DSpaceServiceManager#startup(java.util.List, ConfigurationService)}.
     */
    @Test
    public void testStartup() {
        // testing we can start this up with cleared config
        configurationService.clear();
        dsm.startup();
    }

    @Test
    public void testStartupWithConfig() {
        // testing we can start this up a real config
        dsm.startup();
    }

    /**
     * Test method for {@link org.dspace.servicemanager.DSpaceServiceManager#registerService(java.lang.String, java.lang.Object)}.
     */
    @Test
    public void testRegisterService() {
        dsm.startup();

        String name = "myNewService";
        dsm.registerService(name, "AZ");
        String service = dsm.getServiceByName(name, String.class);
        assertNotNull(service);
        assertEquals("AZ", service);

        try {
            dsm.registerService("fakey", (Object)null);
            fail("should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }
    }

    /**
     * Test method for {@link org.dspace.servicemanager.DSpaceServiceManager#registerServiceClass(java.lang.String, java.lang.Class)}.
     */
    @Test
    public void testRegisterServiceClass() {
        dsm.startup();

        int currentSize = dsm.getServicesByType(SampleAnnotationBean.class).size();

        SampleAnnotationBean sab = dsm.registerServiceClass("newAnnote", SampleAnnotationBean.class);
        assertNotNull(sab);

        List<SampleAnnotationBean> l = dsm.getServicesByType(SampleAnnotationBean.class);
        assertNotNull(l);
        assertEquals(currentSize+1, l.size());

        try {
            dsm.registerService("fakey", (Class<?>)null);
            fail("should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }
    }

    /**
     * Test method for {@link org.dspace.servicemanager.DSpaceServiceManager#unregisterService(java.lang.String)}.
     */
    @Test
    public void testUnregisterService() {
        dsm.startup();

        String name = "myNewService";
        dsm.registerService(name, "AZ");
        String service = dsm.getServiceByName(name, String.class);
        assertNotNull(service);
        assertEquals("AZ", service);

        dsm.unregisterService(name);
    }

    /**
     * Test method for {@link org.dspace.servicemanager.DSpaceServiceManager#getServiceByName(java.lang.String, java.lang.Class)}.
     */
    @Test
    public void testGetServiceByName() {
        configurationService.clear();
        dsm.startup();

        ConcreteExample concrete = dsm.getServiceByName(ConcreteExample.class.getName(), ConcreteExample.class);
        assertNotNull(concrete);
        assertEquals("azeckoski", concrete.getName());

        SampleAnnotationBean sab = dsm.getServiceByName(SampleAnnotationBean.class.getName(), SampleAnnotationBean.class);
        assertNotNull(sab);
        assertEquals(null, sab.getSampleValue());
    }

    @Test
    public void testGetServiceByNameConfig() {
        dsm.startup();

        ConcreteExample concrete = dsm.getServiceByName(ConcreteExample.class.getName(), ConcreteExample.class);
        assertNotNull(concrete);
        assertEquals("azeckoski", concrete.getName());

        SampleAnnotationBean sab = dsm.getServiceByName(SampleAnnotationBean.class.getName(), SampleAnnotationBean.class);
        assertNotNull(sab);
        assertEquals("beckyz", sab.getSampleValue());
    }

    /**
     * Test method for {@link org.dspace.servicemanager.DSpaceServiceManager#getServicesByType(java.lang.Class)}.
     */
    @Test
    public void testGetServicesByType() {
        dsm.startup();

        int currentSize = dsm.getServicesByType(ConcreteExample.class).size();
        assertTrue(currentSize > 0);

        List<ConcreteExample> l = dsm.getServicesByType(ConcreteExample.class);
        assertNotNull(l);
        assertEquals("azeckoski", l.get(0).getName());

        List<SampleAnnotationBean> l2 = dsm.getServicesByType(SampleAnnotationBean.class);
        assertNotNull(l2);
        assertTrue(l2.size() >= 1);

        List<ServiceConfig> l3 = dsm.getServicesByType(ServiceConfig.class);
        assertNotNull(l3);
        assertEquals(0, l3.size());
    }

    /**
     * Test method for {@link org.dspace.servicemanager.DSpaceServiceManager#getServicesNames()}.
     */
    @Test
    public void testGetServicesNames() {
        dsm.startup();

        List<String> names = dsm.getServicesNames();
        assertNotNull(names);
        assertTrue(names.size() >= 3);
    }

    /**
     * Test method for {@link org.dspace.servicemanager.DSpaceServiceManager#isServiceExists(java.lang.String)}.
     */
    @Test
    public void testIsServiceExists() {
        dsm.startup();

        String name = ConcreteExample.class.getName();
        boolean exists = dsm.isServiceExists(name);
        assertTrue(exists);

        name = SampleAnnotationBean.class.getName();
        exists = dsm.isServiceExists(name);
        assertTrue(exists);

        name = SpringAnnotationBean.class.getName();
        exists = dsm.isServiceExists(name);
        assertTrue(exists);

        exists = dsm.isServiceExists("XXXXXXXXXXXXXXX");
        assertFalse(exists);
    }

    @Test
    public void testGetServices() {
        dsm.startup();

        Map<String, Object> services = dsm.getServices();
        assertNotNull(services);
        assertTrue(services.size() > 3);
    }

    @Test
    public void testPushConfig() {
        dsm.startup();

        Map<String, String> properties = new HashMap<String, String>();
        properties.put("some.test.thing", "A value");
        dsm.pushConfig(properties);

        // TODO need to do a better test here
    }

    @Test
    public void testInitAndShutdown() {
        dsm.startup();

        SampleAnnotationBean sab = dsm.getServiceByName(SampleAnnotationBean.class.getName(), SampleAnnotationBean.class);
        assertNotNull(sab);
        assertEquals(1, sab.initCounter);

        TestService ts = new TestService();
        assertEquals(0, ts.value);
        dsm.registerService(TestService.class.getName(), ts);
        assertEquals(1, ts.value);
        dsm.unregisterService(TestService.class.getName());
        assertEquals(2, ts.value);
    }

    @Test
    public void testRegisterProviderLifecycle() {
        dsm.startup();

        // this tests to see if the lifecycle of a provider is working
        String serviceName = "azeckoski.FakeService1";
        FakeService1 service = new FakeService1();
        assertEquals(0, service.getTriggers());

        // now we register it and the init should be called
        dsm.registerService(serviceName, service);
        assertNotNull(service.getConfigurationService());
        assertEquals("init", service.getSomething());
        assertEquals(1, service.getTriggers());

        // now we do a config change
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("azeckoski.FakeService1.something", "THING");
        dsm.pushConfig(properties);
        assertEquals("config:THING", service.getSomething());
        assertEquals(2, service.getTriggers());

        // now we do a service registration
        dsm.registerService("fake2", new FakeService2());
        assertEquals("registered:fake2", service.getSomething());
        assertEquals(3, service.getTriggers());

        // now we unregister
        dsm.unregisterService("fake2");
        assertEquals("unregistered:fake2", service.getSomething());
        assertEquals(4, service.getTriggers());

        // now we unregister
        dsm.unregisterService(serviceName);
        assertEquals("shutdown", service.getSomething());
        assertEquals(5, service.getTriggers());
    }


    public static class TestService implements InitializedService, ShutdownService {

        public int value = 0;
        public void init() {
            value++;
        }
        public void shutdown() {
            value++;
        }

    }

}