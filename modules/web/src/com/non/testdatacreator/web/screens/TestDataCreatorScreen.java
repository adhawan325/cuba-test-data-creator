
package com.non.testdatacreator.web.screens;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.gui.ScreensHelper;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.xml.layout.ComponentsFactory;
import com.non.dta.service.DuplicateRuleService;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.springframework.util.ReflectionUtils;

import javax.inject.Inject;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class TestDataCreatorScreen extends AbstractWindow {
    @Inject
    private DuplicateRuleService duplicateRuleService;
    @Inject
    private DataManager dataManager;
    @Inject
    private LookupField entityField;
    @Inject
    private ComponentsFactory componentsFactory;
    @Inject
    private Metadata metadata;
    @Inject
    private TextField numEntities;
    @Inject
    private GridLayout grid;

    @Override
    public void init(Map<String, Object> params) {
        entityField.setOptionsList(duplicateRuleService.getAccessibleEntities());

        super.init(params);

    }

    protected Map<String, Object> getAttributes(MetaClass metaClass) {
        Map<String, Object> options = new TreeMap<>();

        for (MetaProperty metaProperty : metaClass.getProperties()) {
            options.put(metaProperty.getName(), metaProperty);
        }
        return options;
    }


    public void onGetFieldsClick() {
        Map<String, Object> fields = getAttributes(entityField.getValue());
        Iterator iterator = fields.keySet().iterator();
        while (iterator.hasNext()) {
            MetaProperty metaProperty = (MetaProperty) fields.get(iterator.next());
            if (metaProperty.getJavaType().equals(String.class)) {
                grid.add(createTextBox(metaProperty));
            } else if (metaProperty.getJavaType().equals(Date.class)) {
                grid.add(createDateField(metaProperty));
            } else if (metaProperty.getJavaType().equals(Boolean.class)) {
                grid.add(createCheckBox(metaProperty));
            } else if (metaProperty.getJavaType().equals(Integer.class))
            {
                grid.add(createTextBox(metaProperty));
            }
            else if (metaProperty.getJavaType().getSuperclass() != null && metaProperty.getJavaType().getSuperclass().equals(StandardEntity.class)) {
                grid.add(createPickerField(metaProperty));
            } else {
                if (metaProperty.isMandatory()) {
                    showNotification("Cannot render Required Field: " + metaProperty + ":" + metaProperty.getName() + "-" + metaProperty.getJavaType().getTypeName());
                }

            }
        }
    }

    private Component createPickerField(MetaProperty metaProperty) {
        PickerField field = componentsFactory.createComponent(PickerField.class);
        field.setMetaClass(metadata.getClass(metaProperty.getJavaType()));
        PickerField.LookupAction action = field.addLookupAction();

        action.setLookupScreen(metadata.getClass(metaProperty.getJavaType()) + ".lookup");
        field.setId(metaProperty.getName());
        field.setCaption(metadata.getClass(metaProperty.getJavaType()) + "");
        if (metaProperty.isMandatory()) {
            ((PickerField) field).setRequired(true);
        }
        return field;
    }

    private Component createTextBox(MetaProperty metaProperty) {
        Component newComponent = componentsFactory.createComponent(TextField.class);
        ((TextField) newComponent).setCaption(metaProperty.getName());
        newComponent.setId(metaProperty.getName());
        if (metaProperty.isMandatory()) {
            ((TextField) newComponent).setRequired(true);
        }
        return newComponent;
    }

    private Component createCheckBox(MetaProperty metaProperty) {
        Component newComponent = componentsFactory.createComponent(CheckBox.class);
        ((CheckBox) newComponent).setCaption(metaProperty.getName());
        newComponent.setId(metaProperty.getName());
        if (metaProperty.isMandatory()) {
            ((CheckBox) newComponent).setRequired(true);
        }
        return newComponent;
    }

    private Component createDateField(MetaProperty metaProperty) {
        Component newComponent = componentsFactory.createComponent(DateField.class);
        ((DateField) newComponent).setCaption(metaProperty.getName());
        newComponent.setId(metaProperty.getName());
        if (metaProperty.isMandatory()) {
            ((DateField) newComponent).setRequired(true);
        }
        return newComponent;
    }

    public void onCreateButtonClick() {
        Integer x = new Integer(numEntities.getValue());
        for (int i = 0; i < x.intValue(); i++) {
            createEntity();
        }
    }

    private void createEntity() {
        Entity e = metadata.create(entityField.getValue().toString());
        Map<String, Object> fields = getAttributes(entityField.getValue());
        Iterator iterator = fields.keySet().iterator();
        while (iterator.hasNext()) {
            MetaProperty metaProperty = (MetaProperty) fields.get(iterator.next());
            Component component = this.getComponent(metaProperty.getName());
            if (metaProperty.getJavaType().equals(String.class)) {
                e.setValue(metaProperty.getName(), ((TextField) component).getValue());
            }else if (metaProperty.getJavaType().equals(Date.class)) {
                e.setValue(metaProperty.getName(), ((DateField)component).getValue());
            } else if (metaProperty.getJavaType().equals(Boolean.class)) {
                e.setValue(metaProperty.getName(), ((CheckBox) component).getValue());
            } else if (metaProperty.getJavaType().equals(Integer.class))
            {
                Integer integer = Integer.valueOf(((TextField)component).getValue().toString());
                e.setValue(metaProperty.getName(), integer);
            }
            else if (metaProperty.getJavaType().getSuperclass() != null && metaProperty.getJavaType().getSuperclass().equals(StandardEntity.class)) {
                e.setValue(metaProperty.getName(), ((PickerField) component).getValue());
            }

        }
        dataManager.commit(e);
    }
}
