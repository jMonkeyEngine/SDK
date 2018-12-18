/*
 *  Copyright (c) 2009-2010 jMonkeyEngine
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
package com.jme3.gde.core.filters;

import com.jme3.gde.core.filters.actions.EnableFiterAction;
import com.jme3.gde.core.icons.IconList;
import com.jme3.gde.core.properties.SceneExplorerProperty;
import com.jme3.gde.core.properties.ScenePropertyChangeListener;
import com.jme3.gde.core.scene.SceneSyncListener;
import com.jme3.gde.core.sceneexplorer.nodes.AbstractSceneExplorerNode;
import com.jme3.gde.core.util.PropertyUtils;
import com.jme3.post.Filter;
import java.awt.Image;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.Action;
import org.openide.actions.DeleteAction;
import org.openide.actions.MoveDownAction;
import org.openide.actions.MoveUpAction;
import org.openide.awt.Actions;
import org.openide.loaders.DataObject;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.Sheet;
import org.openide.util.Exceptions;
import org.openide.util.actions.SystemAction;

/**
 *
 * @author normenhansen
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class AbstractFilterNode extends AbstractNode implements FilterNode, ScenePropertyChangeListener, SceneSyncListener {

    protected boolean readOnly = false;
    protected DataObject dataObject;
    protected Filter filter;
    private static Image icon;

    @Override
    public Image getIcon(int type) {
        return icon;

    }

    @Override
    public Image getOpenedIcon(int type) {
        return icon;
    }

    public void toggleIcon(boolean enabled) {
        if (enabled) {
            icon = IconList.eyeOpen.getImage();

        } else {
            icon = IconList.eyeCrossed.getImage();

        }
        fireIconChange();
    }

    public AbstractFilterNode() {
        super(Children.LEAF);
    }

    public AbstractFilterNode(Filter filter) {
        super(Children.LEAF);
        this.filter = filter;
        setName(filter.getName());
        icon = IconList.eyeOpen.getImage();
//        setIconBaseWithExtension(IconList.eyeOpen.);
    }

    @Override
    public Action[] getActions(boolean context) {
        return new Action[]{
                    Actions.alwaysEnabled(new EnableFiterAction(this), "Toggle enabled", "", false),
                    SystemAction.get(MoveUpAction.class),
                    SystemAction.get(MoveDownAction.class),
                    null,
                    SystemAction.get(DeleteAction.class),
                    
                };
    }
    
    public void syncSceneData(float tpf) {
        //TODO: precache structure to avoid locks? Do it backwards, sending the actual bean value?
        for (PropertySet propertySet : getPropertySets()) {
            for (Property<?> property : propertySet.getProperties()) {
                if(property instanceof SceneExplorerProperty){
                    SceneExplorerProperty prop = (SceneExplorerProperty)property;
                    prop.syncValue();
                }
            }
        }
    }
    
    @Override
    public Action getPreferredAction() {
        return Actions.alwaysEnabled(new EnableFiterAction(this), "Toggle enabled", "", false);
    }

    @Override
    public boolean canDestroy() {
        return true;
    }

    @Override
    public void destroy() throws IOException {
        super.destroy();
        FilterPostProcessorNode nod = (FilterPostProcessorNode) getParentNode();
        nod.removeFilter(filter);
        fireSave(true);
    }

    protected void fireSave(boolean modified) {
        if (dataObject != null) {
            dataObject.setModified(true);
        }
    }

    /**
     * returns the PropertySet with the given name (mostly Class.name)
     * @param name
     * @return The PropertySet or null if no PropertySet by that name exists
     */
    public PropertySet getPropertySet(String name) {
        for (int i = 0; i < getPropertySets().length; i++) {
            PropertySet propertySet = getPropertySets()[i];
            if (propertySet.getName().equals(name)) {
                return propertySet;
            }
        }
        return null;
    }

    @Override
    protected Sheet createSheet() {
        Sheet sheet = super.createSheet();
        Sheet.Set set = Sheet.createPropertiesSet();
        set.setDisplayName("Filter");
        set.setName("Filter");
        Filter obj = filter;
        if (obj == null) {
            return sheet;
        }
        createFields(Filter.class, set, obj);
        sheet.put(set);
        return sheet;

    }

      /**
     * @param saveCookie the saveCookie to set
     */
    public AbstractFilterNode setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        return this;
    }

    public void refreshProperties() {
        setSheet(createSheet());
    }

    protected Property<?> makeProperty(Object obj, Class returntype, String method, String name) {
        Property<?> prop = null;
        try {
            prop = new SceneExplorerProperty(getExplorerObjectClass().cast(obj), returntype, method, null);
            prop.setName(name);
        } catch (NoSuchMethodException ex) {
            Exceptions.printStackTrace(ex);
        }
        return prop;
    }

    protected Property<?> makeProperty(Object obj, Class returntype, String method, String setter, String name) {
        Property<?> prop = null;
        try {
            if (readOnly) {
                prop = new SceneExplorerProperty(getExplorerObjectClass().cast(obj), returntype, method, null);
            } else {
                prop = new SceneExplorerProperty(getExplorerObjectClass().cast(obj), returntype, method, setter, this);
            }
            prop.setName(name);

        } catch (NoSuchMethodException ex) {
            Exceptions.printStackTrace(ex);
        }
        return prop;
    }

    /**
     * Scans the passed object for all it's fields and adds them to the Property
     * Sheet Set.
     * 
     * @param c The Class of obj
     * @param set The Property Sheet Set to add values to
     * @param obj The Object to inspect
     * @return The Methods which have been added/discovered. See
     * {@link #createMethods(java.lang.Class, org.openide.nodes.Sheet.Set,
     * java.lang.Object, java.lang.reflect.Method[]) }
     * @throws SecurityException When Inspecting using Reflection failed
     * @see AbstractSceneExplorerNode#createFields(java.lang.Class, org.openide.nodes.Sheet.Set, java.lang.Object) 
     */
    protected Method[] createFields(Class c, Sheet.Set set, Object obj) throws SecurityException {
        ArrayList<Method> methodList = new ArrayList<Method>(c.getDeclaredFields().length);
        for (Field field : c.getDeclaredFields()) {
            PropertyDescriptor prop = PropertyUtils.getPropertyDescriptor(c, field);
            if (prop != null) {
                methodList.add(prop.getReadMethod());
                methodList.add(prop.getWriteMethod());
                set.put(
                    makeProperty(
                            obj, prop.getPropertyType(),
                            prop.getReadMethod().getName(),
                            prop.getWriteMethod().getName(),
                            prop.getDisplayName()
                    )
                );
            }
        }
        
        return methodList.toArray(new Method[methodList.size()]);
    }
    
    /**
     * Scans the passed object for all it's methods and adds them to the
     * PropertySheet Set. Excludes the methods passed in, which are typically
     * already found by {@link #createFields(java.lang.Class,
     * org.openide.nodes.Sheet.Set, java.lang.Object) } and should not
     * be added twice to the Properties.
     * 
     * @param c The Class of obj
     * @param set The Property Sheet Set to add values to
     * @param obj The Object to inspect
     * @param ignoreMethods The Methods to ignore
     * @throws SecurityException When Inspecting using Reflection failed
     */
    protected void createMethods(Class c, Sheet.Set set, Object obj, Method[] ignoreMethods) throws SecurityException {
        List<Method> ignoreMethodList = new ArrayList<Method>(Arrays.asList(ignoreMethods));
        
        for (Method m : c.getDeclaredMethods()) {
            // Ignore Methods which were already discovered by the fields.
            if (!ignoreMethodList.contains(m)) {
                PropertyDescriptor prop = PropertyUtils.getPropertyDescriptor(c, m);
                if (prop != null) {
                    /* add the setter/getter to the ignoreMethodsList, to
                     * prevent double discovery of the same internal field
                     */
                    ignoreMethodList.add(prop.getReadMethod());
                    ignoreMethodList.add(prop.getWriteMethod());
                    
                    set.put(
                        makeProperty(
                            obj, prop.getPropertyType(),
                            prop.getReadMethod().getName(),
                            prop.getWriteMethod().getName(),
                            prop.getDisplayName().substring(
                                // Remove "Is " "Set "
                                prop.getDisplayName().indexOf(" ") + 1
                            )
                        )
                    );
                }
            }
        }
    }

    @Override
    public void propertyChange(final String type, final String name, final Object before, final Object after) {
        if (name.equals("Enabled")) {
            toggleIcon((Boolean) after);
        }
        if (name.equals("Name")) {
            setName((String)after);
        }
        firePropertyChange(name, before, after);
        if (!SceneExplorerProperty.PROP_INIT_CHANGE.equals(type)) {
            fireSave(true);
        }
    }

    public Filter getFilter() {
        return filter;
    }

    @Override
    public abstract Class<?> getExplorerObjectClass();

    @Override
    public abstract Node[] createNodes(Object key, DataObject dataObject, boolean readOnly);
}
