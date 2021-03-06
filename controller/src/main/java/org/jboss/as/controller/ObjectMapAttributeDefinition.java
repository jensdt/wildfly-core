/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016, Red Hat, Inc., and individual contributors as indicated
 * by the @authors tag.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.controller;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.MinMaxValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link MapAttributeDefinition} for maps with keys of {@link ModelType#STRING} and values of type {@link ObjectTypeAttributeDefinition}
 *
 * @author Tomaz Cerar
 * @since 11.0
 */
public class ObjectMapAttributeDefinition extends MapAttributeDefinition {
    private final ObjectTypeAttributeDefinition valueType;

    private ObjectMapAttributeDefinition(final Builder builder) {
        super(builder);
        this.valueType = builder.valueType;
    }

    @Override
    public void addCapabilityRequirements(OperationContext context, Resource resource, ModelNode attributeValue) {
        if (attributeValue.isDefined()) {
            valueType.addCapabilityRequirements(context, resource, attributeValue);

        }
    }

    @Override
    public void removeCapabilityRequirements(OperationContext context, Resource resource, ModelNode attributeValue) {
        if (attributeValue.isDefined()) {
            valueType.removeCapabilityRequirements(context, resource, attributeValue);

        }
    }


    @Override
    protected void addOperationParameterValueTypeDescription(ModelNode node, String operationName, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
        addValueTypeDescription(node, getName(), bundle, true, resolver, locale);
    }

    @Override
    protected void addValueTypeDescription(final ModelNode node, final ResourceBundle bundle) {
        addValueTypeDescription(node, valueType.getName(), bundle, false, null, null);
    }

    @Override
    protected void addAttributeValueTypeDescription(final ModelNode node, final ResourceDescriptionResolver resolver, final Locale locale, final ResourceBundle bundle) {
        addValueTypeDescription(node, getName(), bundle, false, resolver, locale);
    }


    protected void addValueTypeDescription(final ModelNode node, final String prefix, final ResourceBundle bundle,
                                           boolean forOperation, final ResourceDescriptionResolver resolver, Locale locale) {
        node.get(ModelDescriptionConstants.DESCRIPTION); // placeholder
        node.get(ModelDescriptionConstants.EXPRESSIONS_ALLOWED).set(valueType.isAllowExpression());
        if (forOperation) {
            node.get(ModelDescriptionConstants.REQUIRED).set(!valueType.isAllowNull());
        }
        node.get(ModelDescriptionConstants.NILLABLE).set(isAllowNull());
        final ModelNode defaultValue = valueType.getDefaultValue();
        if (!forOperation && defaultValue != null && defaultValue.isDefined()) {
            node.get(ModelDescriptionConstants.DEFAULT).set(defaultValue);
        }
        MeasurementUnit measurementUnit = valueType.getMeasurementUnit();
        if (measurementUnit != null && measurementUnit != MeasurementUnit.NONE) {
            node.get(ModelDescriptionConstants.UNIT).set(measurementUnit.getName());
        }
        final String[] alternatives = valueType.getAlternatives();
        if (alternatives != null) {
            for (final String alternative : alternatives) {
                node.get(ModelDescriptionConstants.ALTERNATIVES).add(alternative);
            }
        }
        final String[] requires = valueType.getRequires();
        if (requires != null) {
            for (final String required : requires) {
                node.get(ModelDescriptionConstants.REQUIRES).add(required);
            }
        }
        final ParameterValidator validator = valueType.getValidator();
        if (validator instanceof MinMaxValidator) {
            MinMaxValidator minMax = (MinMaxValidator) validator;
            Long min = minMax.getMin();
            if (min != null) {
                switch (valueType.getType()) {
                    case STRING:
                    case LIST:
                    case OBJECT:
                    case BYTES:
                        node.get(ModelDescriptionConstants.MIN_LENGTH).set(min);
                        break;
                    default:
                        node.get(ModelDescriptionConstants.MIN).set(min);
                }
            }
            Long max = minMax.getMax();
            if (max != null) {
                switch (valueType.getType()) {
                    case STRING:
                    case LIST:
                    case OBJECT:
                    case BYTES:
                        node.get(ModelDescriptionConstants.MAX_LENGTH).set(max);
                        break;
                    default:
                        node.get(ModelDescriptionConstants.MAX).set(max);
                }
            }
        }
        addAllowedValuesToDescription(node, validator);
        valueType.addValueTypeDescription(node, prefix, bundle, forOperation, resolver, locale);
    }

    ObjectTypeAttributeDefinition getValueType() {
        return valueType;
    }

    public static Builder create(final String name, final ObjectTypeAttributeDefinition valueType) {
        return new Builder(name, valueType);
    }

    public static final class Builder extends MapAttributeDefinition.Builder<Builder, ObjectMapAttributeDefinition> {
        private final ObjectTypeAttributeDefinition valueType;

        public Builder(final String name, final ObjectTypeAttributeDefinition valueType) {
            super(name);
            this.valueType = valueType;
            setElementValidator(valueType.getValidator());
            setAttributeParser(AttributeParsers.OBJECT_MAP_WRAPPED);
            setAttributeMarshaller(AttributeMarshaller.OBJECT_MAP_MARSHALLER);
        }

        public static ObjectMapAttributeDefinition.Builder of(final String name, final ObjectTypeAttributeDefinition valueType) {
            return new ObjectMapAttributeDefinition.Builder(name, valueType);
        }

        public ObjectMapAttributeDefinition build() {
            return new ObjectMapAttributeDefinition(this);
        }
    }


}
