/**
 * $Id: SampleAnnotationBean.java 3501 2009-02-26 14:04:46Z grahamtriggs $
 * $URL: https://scm.dspace.org/svn/repo/dspace2/core/trunk/impl/src/test/java/org/dspace/servicemanager/SampleAnnotationBean.java $
 * SampleAnnotationBean.java - DSpace2 - Oct 5, 2008 11:25:47 PM - azeckoski
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

import org.dspace.kernel.mixins.InitializedService;
import org.dspace.kernel.mixins.ShutdownService;
import org.dspace.servicemanager.example.ConcreteExample;
import org.dspace.servicemanager.example.ServiceExample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.stereotype.Service;

/**
 * This bean is a simple example of a bean which is annotated as a spring bean and should be found when the AC starts up
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
@Service
public class SampleAnnotationBean implements InitializedService, ShutdownService {

    public int initCounter = 0;
    public void init() {
        initCounter++;
    }
    public void shutdown() {
        initCounter++;
    }

    private ServiceExample serviceExample;
    @Autowired
    @Required
    public void setServiceExample(ServiceExample serviceExample) {
        this.serviceExample = serviceExample;
    }

    private ConcreteExample concreteExample;
    @Autowired
    @Required
    public void setConcreteExample(ConcreteExample concreteExample) {
        this.concreteExample = concreteExample;
    }

    public String getExampleName() {
        return serviceExample.getName();
    }

    public String getOtherName() {
        return serviceExample.getOtherName();
    }

    public String getConcreteName() {
        return concreteExample.getName();
    }

    private String value = null;
    public void setTestName(String value) {
        this.value = value;
    }

    public String getSampleValue() {
        return value;
    }

}