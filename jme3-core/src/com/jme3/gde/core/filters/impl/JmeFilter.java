/*
 *  Copyright (c) 2009-2016 jMonkeyEngine
 *  All rights reserved.
 * 
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are
 *  met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 *  * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.gde.core.filters.impl;

import com.jme3.gde.core.filters.AbstractFilterNode;
import com.jme3.gde.core.filters.FilterNode;
import com.jme3.post.Filter;
import com.jme3.shadow.AbstractShadowFilter;
import java.lang.reflect.Method;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.nodes.Sheet;


/**
 *
 * @author dokthar
 */
@org.openide.util.lookup.ServiceProvider(service = FilterNode.class)
public class JmeFilter extends AbstractFilterNode {
    
    public JmeFilter() {
    }

    public JmeFilter(Filter filter, DataObject object, boolean readOnly) {
        super(filter);
        this.dataObject = object;
        this.readOnly = readOnly;
    }

    @Override
    protected Sheet createSheet() {
        Sheet sheet = super.createSheet();
        
        if (filter == null) {
            Sheet.Set set = Sheet.createPropertiesSet();
            return sheet;
        }
        
        filter.setName(filter.getClass().getSimpleName());
        Class<?> c = filter.getClass();
        
        do {
            // The Filter class is already processed in AbstractFilterNode#createSheet
            if (c.equals(Filter.class) || c.equals(Object.class)) {
                c = c.getSuperclass();
                continue;
            }
            
            Sheet.Set set = Sheet.createPropertiesSet();
            set.setName(c.getName()); // A set's name is it's unique identifier
            set.setDisplayName(c.getName());
            
            Method[] methods = createFields(c, set, filter);
            createMethods(c, set, filter, methods);
            
            sheet.put(set);
            
            c = c.getSuperclass();
        } while (c != null);
        
        return sheet;
    }

    @Override
    public Class<?> getExplorerObjectClass() {
        return filter.getClass();
    }

    @Override
    public Node[] createNodes(Object key, DataObject dataObject, boolean readOnly) {
        return new Node[]{new JmeFilter((Filter) key, dataObject, readOnly)};
    }
    
}
