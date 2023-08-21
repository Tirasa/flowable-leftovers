/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.editor.language.json.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.flowable.editor.constants.EditorJsonConstants;
import org.flowable.editor.constants.StencilConstants;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.BooleanDataObject;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.DateDataObject;
import org.flowable.bpmn.model.DoubleDataObject;
import org.flowable.bpmn.model.Escalation;
import org.flowable.bpmn.model.EventListener;
import org.flowable.bpmn.model.FieldExtension;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowableListener;
import org.flowable.bpmn.model.ImplementationType;
import org.flowable.bpmn.model.IntegerDataObject;
import org.flowable.bpmn.model.ItemDefinition;
import org.flowable.bpmn.model.LongDataObject;
import org.flowable.bpmn.model.Message;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.Signal;
import org.flowable.bpmn.model.StringDataObject;
import org.flowable.bpmn.model.UserTask;
import org.flowable.bpmn.model.ValuedDataObject;
import org.flowable.editor.language.json.converter.util.CollectionUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 */
public final class BpmnJsonConverterUtil implements EditorJsonConstants, StencilConstants {

    private static final Logger LOG = LoggerFactory.getLogger(BpmnJsonConverterUtil.class);

    private static final DateTimeFormatter DATE_TIME_FORMATTER = ISODateTimeFormat.dateTimeParser();

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().findAndAddModules().build();

    public static ObjectNode createChildShape(
            final String id,
            final String type,
            final double lowerRightX,
            final double lowerRightY,
            final double upperLeftX, final double upperLeftY) {

        ObjectNode shapeNode = JSON_MAPPER.createObjectNode();
        shapeNode.set(EDITOR_BOUNDS, createBoundsNode(lowerRightX, lowerRightY, upperLeftX, upperLeftY));
        shapeNode.put(EDITOR_SHAPE_ID, id);
        ArrayNode shapesArrayNode = JSON_MAPPER.createArrayNode();
        shapeNode.set(EDITOR_CHILD_SHAPES, shapesArrayNode);
        ObjectNode stencilNode = JSON_MAPPER.createObjectNode();
        stencilNode.put(EDITOR_STENCIL_ID, type);
        shapeNode.set(EDITOR_STENCIL, stencilNode);
        return shapeNode;
    }

    public static ObjectNode createBoundsNode(
            final double lowerRightX,
            final double lowerRightY,
            final double upperLeftX,
            final double upperLeftY) {

        ObjectNode boundsNode = JSON_MAPPER.createObjectNode();
        boundsNode.set(EDITOR_BOUNDS_LOWER_RIGHT, createPositionNode(lowerRightX, lowerRightY));
        boundsNode.set(EDITOR_BOUNDS_UPPER_LEFT, createPositionNode(upperLeftX, upperLeftY));
        return boundsNode;
    }

    public static ObjectNode createPositionNode(final double x, final double y) {
        ObjectNode positionNode = JSON_MAPPER.createObjectNode();
        positionNode.put(EDITOR_BOUNDS_X, x);
        positionNode.put(EDITOR_BOUNDS_Y, y);
        return positionNode;
    }

    public static ObjectNode createResourceNode(final String id) {
        ObjectNode resourceNode = JSON_MAPPER.createObjectNode();
        resourceNode.put(EDITOR_SHAPE_ID, id);
        return resourceNode;
    }

    public static String getStencilId(final JsonNode objectNode) {
        String stencilId = null;
        JsonNode stencilNode = objectNode.get(EDITOR_STENCIL);
        if (stencilNode != null && stencilNode.get(EDITOR_STENCIL_ID) != null) {
            stencilId = stencilNode.get(EDITOR_STENCIL_ID).asText();
        }
        return stencilId;
    }

    public static String getElementId(final JsonNode objectNode) {
        String elementId;
        if (StringUtils.isNotEmpty(getPropertyValueAsString(PROPERTY_OVERRIDE_ID, objectNode))) {
            elementId = getPropertyValueAsString(PROPERTY_OVERRIDE_ID, objectNode).trim();
        } else {
            elementId = objectNode.get(EDITOR_SHAPE_ID).asText();
        }

        return elementId;
    }

    public static void convertMessagesToJson(final Collection<Message> messages, final ObjectNode propertiesNode) {
        String propertyName = "messages";

        ArrayNode messagesNode = JSON_MAPPER.createArrayNode();
        for (Message message : messages) {
            ObjectNode propertyItemNode = JSON_MAPPER.createObjectNode();

            propertyItemNode.put(PROPERTY_MESSAGE_ID, message.getId());
            propertyItemNode.put(PROPERTY_MESSAGE_NAME, message.getName());
            propertyItemNode.put(PROPERTY_MESSAGE_ITEM_REF, message.getItemRef());

            messagesNode.add(propertyItemNode);
        }

        propertiesNode.set(propertyName, messagesNode);
    }

    public static void convertListenersToJson(
            final List<FlowableListener> listeners,
            final boolean isExecutionListener,
            final ObjectNode propertiesNode) {

        String propertyName;
        String valueName;
        if (isExecutionListener) {
            propertyName = PROPERTY_EXECUTION_LISTENERS;
            valueName = "executionListeners";
        } else {
            propertyName = PROPERTY_TASK_LISTENERS;
            valueName = "taskListeners";
        }

        ObjectNode listenersNode = JSON_MAPPER.createObjectNode();
        ArrayNode itemsNode = JSON_MAPPER.createArrayNode();
        for (FlowableListener listener : listeners) {
            ObjectNode propertyItemNode = JSON_MAPPER.createObjectNode();

            propertyItemNode.put(PROPERTY_LISTENER_EVENT, listener.getEvent());

            if (ImplementationType.IMPLEMENTATION_TYPE_CLASS.equals(listener.getImplementationType())) {
                propertyItemNode.put(PROPERTY_LISTENER_CLASS_NAME, listener.getImplementation());
            } else if (ImplementationType.IMPLEMENTATION_TYPE_EXPRESSION.equals(listener.getImplementationType())) {
                propertyItemNode.put(PROPERTY_LISTENER_EXPRESSION, listener.getImplementation());
            } else if (ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION.
                    equals(listener.getImplementationType())) {

                propertyItemNode.put(PROPERTY_LISTENER_DELEGATE_EXPRESSION, listener.getImplementation());
            }

            if (CollectionUtils.isNotEmpty(listener.getFieldExtensions())) {
                ArrayNode fieldsArray = JSON_MAPPER.createArrayNode();
                for (FieldExtension fieldExtension : listener.getFieldExtensions()) {
                    ObjectNode fieldNode = JSON_MAPPER.createObjectNode();
                    fieldNode.put(PROPERTY_FIELD_NAME, fieldExtension.getFieldName());
                    if (StringUtils.isNotEmpty(fieldExtension.getStringValue())) {
                        fieldNode.put(PROPERTY_FIELD_STRING_VALUE, fieldExtension.getStringValue());
                    }
                    if (StringUtils.isNotEmpty(fieldExtension.getExpression())) {
                        fieldNode.put(PROPERTY_FIELD_EXPRESSION, fieldExtension.getExpression());
                    }
                    fieldsArray.add(fieldNode);
                }
                propertyItemNode.set(PROPERTY_LISTENER_FIELDS, fieldsArray);
            }

            itemsNode.add(propertyItemNode);
        }

        listenersNode.set(valueName, itemsNode);
        propertiesNode.set(propertyName, listenersNode);
    }

    public static void convertEventListenersToJson(
            final List<EventListener> listeners, final ObjectNode propertiesNode) {

        ObjectNode listenersNode = JSON_MAPPER.createObjectNode();
        ArrayNode itemsNode = JSON_MAPPER.createArrayNode();
        for (EventListener listener : listeners) {
            ObjectNode propertyItemNode = JSON_MAPPER.createObjectNode();

            if (StringUtils.isNotEmpty(listener.getEvents())) {
                ArrayNode eventArrayNode = JSON_MAPPER.createArrayNode();
                String[] eventArray = listener.getEvents().split(",");
                for (String eventValue : eventArray) {
                    if (StringUtils.isNotEmpty(eventValue.trim())) {
                        ObjectNode eventNode = JSON_MAPPER.createObjectNode();
                        eventNode.put(PROPERTY_EVENTLISTENER_EVENT, eventValue.trim());
                        eventArrayNode.add(eventNode);
                    }
                }
                propertyItemNode.put(PROPERTY_EVENTLISTENER_EVENT, listener.getEvents());
                propertyItemNode.set(PROPERTY_EVENTLISTENER_EVENTS, eventArrayNode);
            }

            String implementationText = null;
            if (ImplementationType.IMPLEMENTATION_TYPE_CLASS.equals(listener.getImplementationType())) {
                propertyItemNode.put(PROPERTY_EVENTLISTENER_CLASS_NAME, listener.getImplementation());
                implementationText = listener.getImplementation();
            } else if (ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION.
                    equals(listener.getImplementationType())) {
                propertyItemNode.put(PROPERTY_EVENTLISTENER_DELEGATE_EXPRESSION, listener.getImplementation());
                implementationText = listener.getImplementation();
            } else if (ImplementationType.IMPLEMENTATION_TYPE_THROW_ERROR_EVENT.equals(
                    listener.getImplementationType())) {

                propertyItemNode.put(PROPERTY_EVENTLISTENER_RETHROW_EVENT, true);
                propertyItemNode.put(PROPERTY_EVENTLISTENER_RETHROW_TYPE, "error");
                propertyItemNode.put(PROPERTY_EVENTLISTENER_ERROR_CODE, listener.getImplementation());
                implementationText = "Rethrow as error " + listener.getImplementation();
            } else if (ImplementationType.IMPLEMENTATION_TYPE_THROW_MESSAGE_EVENT.equals(listener.
                    getImplementationType())) {

                propertyItemNode.put(PROPERTY_EVENTLISTENER_RETHROW_EVENT, true);
                propertyItemNode.put(PROPERTY_EVENTLISTENER_RETHROW_TYPE, "message");
                propertyItemNode.put(PROPERTY_EVENTLISTENER_MESSAGE_NAME, listener.getImplementation());
                implementationText = "Rethrow as message " + listener.getImplementation();
            } else if (ImplementationType.IMPLEMENTATION_TYPE_THROW_SIGNAL_EVENT.equals(
                    listener.getImplementationType())) {

                propertyItemNode.put(PROPERTY_EVENTLISTENER_RETHROW_EVENT, true);
                propertyItemNode.put(PROPERTY_EVENTLISTENER_RETHROW_TYPE, "signal");
                propertyItemNode.put(PROPERTY_EVENTLISTENER_SIGNAL_NAME, listener.getImplementation());
                implementationText = "Rethrow as signal " + listener.getImplementation();
            } else if (ImplementationType.IMPLEMENTATION_TYPE_THROW_GLOBAL_SIGNAL_EVENT.equals(
                    listener.getImplementationType())) {

                propertyItemNode.put(PROPERTY_EVENTLISTENER_RETHROW_EVENT, true);
                propertyItemNode.put(PROPERTY_EVENTLISTENER_RETHROW_TYPE, "globalSignal");
                propertyItemNode.put(PROPERTY_EVENTLISTENER_SIGNAL_NAME, listener.getImplementation());
                implementationText = "Rethrow as signal " + listener.getImplementation();
            }

            if (StringUtils.isNotEmpty(implementationText)) {
                propertyItemNode.put(PROPERTY_EVENTLISTENER_IMPLEMENTATION, implementationText);
            }

            if (StringUtils.isNotEmpty(listener.getEntityType())) {
                propertyItemNode.put(PROPERTY_EVENTLISTENER_ENTITY_TYPE, listener.getEntityType());
            }

            itemsNode.add(propertyItemNode);
        }

        listenersNode.set(PROPERTY_EVENTLISTENER_VALUE, itemsNode);
        propertiesNode.set(PROPERTY_EVENT_LISTENERS, listenersNode);
    }

    public static void convertSignalDefinitionsToJson(final BpmnModel bpmnModel, final ObjectNode propertiesNode) {
        if (bpmnModel.getSignals() == null) {
            return;
        }

        ArrayNode signalDefinitions = JSON_MAPPER.createArrayNode();
        for (Signal signal : bpmnModel.getSignals()) {
            ObjectNode signalNode = signalDefinitions.addObject();
            signalNode.put(PROPERTY_SIGNAL_DEFINITION_ID, signal.getId());
            signalNode.put(PROPERTY_SIGNAL_DEFINITION_NAME, signal.getName());
            signalNode.put(PROPERTY_SIGNAL_DEFINITION_SCOPE, signal.getScope());
        }
        propertiesNode.set(PROPERTY_SIGNAL_DEFINITIONS, signalDefinitions);
    }

    public static void convertMessagesToJson(final BpmnModel bpmnModel, final ObjectNode propertiesNode) {
        if (bpmnModel.getMessages() == null) {
            return;
        }

        ArrayNode messageDefinitions = JSON_MAPPER.createArrayNode();
        for (Message message : bpmnModel.getMessages()) {
            ObjectNode messageNode = messageDefinitions.addObject();
            messageNode.put(PROPERTY_MESSAGE_DEFINITION_ID, message.getId());
            messageNode.put(PROPERTY_MESSAGE_DEFINITION_NAME, message.getName());
        }
        propertiesNode.set(PROPERTY_MESSAGE_DEFINITIONS, messageDefinitions);
    }

    public static void convertEscalationDefinitionsToJson(final BpmnModel bpmnModel, final ObjectNode propertiesNode) {
        if (bpmnModel.getEscalations() == null) {
            return;
        }

        ArrayNode escalationDefinitions = JSON_MAPPER.createArrayNode();
        for (Escalation escalation : bpmnModel.getEscalations()) {
            ObjectNode escalationNode = escalationDefinitions.addObject();
            escalationNode.put(PROPERTY_ESCALATION_DEFINITION_ID, escalation.getEscalationCode());
            if (StringUtils.isNotEmpty(escalation.getName())) {
                escalationNode.put(PROPERTY_ESCALATION_DEFINITION_NAME, escalation.getName());
            }
        }
        propertiesNode.set(PROPERTY_ESCALATION_DEFINITIONS, escalationDefinitions);
    }

    public static void convertJsonToListeners(final JsonNode objectNode, final BaseElement element) {
        JsonNode executionListenersNode = getProperty(PROPERTY_EXECUTION_LISTENERS, objectNode);
        if (executionListenersNode != null) {
            executionListenersNode = validateIfNodeIsTextual(executionListenersNode);
            JsonNode listenersNode = executionListenersNode.get("executionListeners");
            parseListeners(listenersNode, element, false);
        }

        if (element instanceof UserTask) {
            JsonNode taskListenersNode = getProperty(PROPERTY_TASK_LISTENERS, objectNode);
            if (taskListenersNode != null) {
                taskListenersNode = validateIfNodeIsTextual(taskListenersNode);
                JsonNode listenersNode = taskListenersNode.get("taskListeners");
                parseListeners(listenersNode, element, true);
            }
        }
    }

    public static void convertJsonToMessages(final JsonNode objectNode, final BpmnModel element) {
        JsonNode messagesNode = getProperty(PROPERTY_MESSAGE_DEFINITIONS, objectNode);
        if (messagesNode != null) {
            messagesNode = validateIfNodeIsTextual(messagesNode);
            parseMessages(messagesNode, element);
        }
    }

    protected static void parseListeners(
            final JsonNode listenersNode,
            final BaseElement element,
            final boolean isTaskListener) {

        if (listenersNode == null) {
            return;
        }

        for (JsonNode listenerNode : validateIfNodeIsTextual(listenersNode)) {
            listenerNode = validateIfNodeIsTextual(listenerNode);
            JsonNode eventNode = listenerNode.get(PROPERTY_LISTENER_EVENT);
            if (eventNode != null && !eventNode.isNull() && StringUtils.isNotEmpty(eventNode.asText())) {
                FlowableListener listener = new FlowableListener();
                listener.setEvent(eventNode.asText());
                if (StringUtils.isNotEmpty(getValueAsString(PROPERTY_LISTENER_CLASS_NAME, listenerNode))) {
                    listener.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);
                    listener.setImplementation(getValueAsString(PROPERTY_LISTENER_CLASS_NAME, listenerNode));
                } else if (StringUtils.isNotEmpty(getValueAsString(PROPERTY_LISTENER_EXPRESSION, listenerNode))) {
                    listener.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_EXPRESSION);
                    listener.setImplementation(getValueAsString(PROPERTY_LISTENER_EXPRESSION, listenerNode));
                } else if (StringUtils.isNotEmpty(
                        getValueAsString(PROPERTY_LISTENER_DELEGATE_EXPRESSION, listenerNode))) {

                    listener.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION);
                    listener.setImplementation(getValueAsString(PROPERTY_LISTENER_DELEGATE_EXPRESSION, listenerNode));
                }

                JsonNode fieldsNode = listenerNode.get(PROPERTY_LISTENER_FIELDS);
                if (fieldsNode != null) {
                    for (JsonNode fieldNode : fieldsNode) {
                        JsonNode nameNode = fieldNode.get(PROPERTY_FIELD_NAME);
                        if (nameNode != null && !nameNode.isNull() && StringUtils.isNotEmpty(nameNode.asText())) {
                            FieldExtension fieldExtension = new FieldExtension();
                            fieldExtension.setFieldName(nameNode.asText());
                            fieldExtension.setStringValue(getValueAsString(PROPERTY_FIELD_STRING_VALUE, fieldNode));
                            if (StringUtils.isEmpty(fieldExtension.getStringValue())) {
                                fieldExtension.setStringValue(getValueAsString(PROPERTY_FIELD_STRING, fieldNode));
                            }
                            if (StringUtils.isEmpty(fieldExtension.getStringValue())) {
                                fieldExtension.setExpression(getValueAsString(PROPERTY_FIELD_EXPRESSION, fieldNode));
                            }
                            listener.getFieldExtensions().add(fieldExtension);
                        }
                    }
                }

                if (element instanceof Process process) {
                    process.getExecutionListeners().add(listener);
                } else if (element instanceof SequenceFlow sequenceFlow) {
                    sequenceFlow.getExecutionListeners().add(listener);
                } else if (element instanceof UserTask userTask) {
                    if (isTaskListener) {
                        userTask.getTaskListeners().add(listener);
                    } else {
                        userTask.getExecutionListeners().add(listener);
                    }
                } else if (element instanceof FlowElement flowElement) {
                    flowElement.getExecutionListeners().add(listener);
                }
            }
        }
    }

    protected static void parseMessages(final JsonNode messagesNode, final BpmnModel element) {
        if (messagesNode == null) {
            return;
        }

        for (JsonNode messageNode : messagesNode) {
            Message message = new Message();

            String messageId = getValueAsString(PROPERTY_MESSAGE_DEFINITION_ID, messageNode);
            if (StringUtils.isNotEmpty(messageId)) {
                message.setId(messageId);
            }
            String messageName = getValueAsString(PROPERTY_MESSAGE_DEFINITION_NAME, messageNode);
            if (StringUtils.isNotEmpty(messageName)) {
                message.setName(messageName);
            }
            String messageItemRef = getValueAsString(PROPERTY_MESSAGE_DEFINITION_ITEM_REF, messageNode);
            if (StringUtils.isNotEmpty(messageItemRef)) {
                message.setItemRef(messageItemRef);
            }

            if (StringUtils.isNotEmpty(messageId)) {
                element.addMessage(message);
            }
        }
    }

    public static void parseEventListeners(final JsonNode listenersNode, final Process process) {
        if (listenersNode == null) {
            return;
        }

        for (JsonNode listenerNode : validateIfNodeIsTextual(listenersNode)) {
            JsonNode eventsNode = listenerNode.get(PROPERTY_EVENTLISTENER_EVENTS);
            if (eventsNode != null && eventsNode.isArray() && eventsNode.size() > 0) {
                EventListener listener = new EventListener();
                StringBuilder eventsBuilder = new StringBuilder();
                for (JsonNode eventNode : eventsNode) {
                    JsonNode eventValueNode = eventNode.get(PROPERTY_EVENTLISTENER_EVENT);
                    if (eventValueNode != null
                            && !eventValueNode.isNull() && StringUtils.isNotEmpty(eventValueNode.asText())) {

                        if (eventsBuilder.length() > 0) {
                            eventsBuilder.append(",");
                        }
                        eventsBuilder.append(eventValueNode.asText());
                    }
                }

                if (eventsBuilder.length() == 0) {
                    continue;
                }

                listener.setEvents(eventsBuilder.toString());

                JsonNode rethrowEventNode = listenerNode.get("rethrowEvent");
                if (rethrowEventNode != null && rethrowEventNode.asBoolean()) {
                    JsonNode rethrowTypeNode = listenerNode.get("rethrowType");
                    if (rethrowTypeNode != null) {
                        if ("error".equalsIgnoreCase(rethrowTypeNode.asText())) {
                            String errorCode = getValueAsString("errorcode", listenerNode);
                            if (StringUtils.isNotEmpty(errorCode)) {
                                listener.setImplementationType(
                                        ImplementationType.IMPLEMENTATION_TYPE_THROW_ERROR_EVENT);
                                listener.setImplementation(errorCode);
                            }
                        } else if ("message".equalsIgnoreCase(rethrowTypeNode.asText())) {
                            String messageName = getValueAsString("messagename", listenerNode);
                            if (StringUtils.isNotEmpty(messageName)) {
                                listener.setImplementationType(
                                        ImplementationType.IMPLEMENTATION_TYPE_THROW_MESSAGE_EVENT);
                                listener.setImplementation(messageName);
                            }
                        } else if ("signal".equalsIgnoreCase(rethrowTypeNode.asText())) {
                            String signalName = getValueAsString("signalname", listenerNode);
                            if (StringUtils.isNotEmpty(signalName)) {
                                listener.setImplementationType(
                                        ImplementationType.IMPLEMENTATION_TYPE_THROW_SIGNAL_EVENT);
                                listener.setImplementation(signalName);
                            }
                        } else if ("globalSignal".equalsIgnoreCase(rethrowTypeNode.asText())) {
                            String signalName = getValueAsString("signalname", listenerNode);
                            if (StringUtils.isNotEmpty(signalName)) {
                                listener.setImplementationType(
                                        ImplementationType.IMPLEMENTATION_TYPE_THROW_GLOBAL_SIGNAL_EVENT);
                                listener.setImplementation(signalName);
                            }
                        }
                    }

                    if (StringUtils.isEmpty(listener.getImplementation())) {
                        continue;
                    }
                } else {
                    if (StringUtils.isNotEmpty(getValueAsString(PROPERTY_EVENTLISTENER_CLASS_NAME, listenerNode))) {
                        listener.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);
                        listener.setImplementation(getValueAsString(PROPERTY_EVENTLISTENER_CLASS_NAME, listenerNode));
                    } else if (StringUtils.isNotEmpty(
                            getValueAsString(PROPERTY_EVENTLISTENER_DELEGATE_EXPRESSION, listenerNode))) {

                        listener.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION);
                        listener.setImplementation(
                                getValueAsString(PROPERTY_EVENTLISTENER_DELEGATE_EXPRESSION, listenerNode));
                    }

                    if (StringUtils.isNotEmpty(getValueAsString(PROPERTY_EVENTLISTENER_ENTITY_TYPE, listenerNode))) {
                        listener.setEntityType(getValueAsString(PROPERTY_EVENTLISTENER_ENTITY_TYPE, listenerNode));
                    }

                    if (StringUtils.isEmpty(listener.getImplementation())) {
                        continue;
                    }
                }

                process.getEventListeners().add(listener);
            }
        }
    }

    public static String lookForSourceRef(final String flowId, final JsonNode childShapesNode) {
        if (childShapesNode == null) {
            return null;
        }

        String sourceRef = null;
        for (JsonNode childNode : childShapesNode) {
            JsonNode outgoingNode = childNode.get("outgoing");
            if (outgoingNode != null && outgoingNode.size() > 0) {
                for (JsonNode outgoingChildNode : outgoingNode) {
                    JsonNode resourceNode = outgoingChildNode.get(EDITOR_SHAPE_ID);
                    if (resourceNode != null && flowId.equals(resourceNode.asText())) {
                        sourceRef = BpmnJsonConverterUtil.getElementId(childNode);
                        break;
                    }
                }

                if (sourceRef != null) {
                    break;
                }
            }
            sourceRef = lookForSourceRef(flowId, childNode.get(EDITOR_CHILD_SHAPES));
            if (sourceRef != null) {
                break;
            }
        }

        return sourceRef;
    }

    public static List<ValuedDataObject> convertJsonToDataProperties(
            final JsonNode objectNode, final BaseElement element) {

        List<ValuedDataObject> dataObjects = new ArrayList<>();

        if (objectNode == null) {
            return dataObjects;
        }

        JsonNode inner = objectNode;
        if (objectNode.isValueNode() && StringUtils.isNotEmpty(objectNode.asText())) {
            try {
                inner = JSON_MAPPER.readTree(objectNode.asText());
            } catch (Exception e) {
                LOG.info("Data properties node cannot be read", e);
            }
        }

        JsonNode itemsArrayNode = inner.get(EDITOR_PROPERTIES_GENERAL_ITEMS);
        if (itemsArrayNode != null) {
            for (JsonNode dataNode : itemsArrayNode) {

                JsonNode dataIdNode = dataNode.get(PROPERTY_DATA_ID);
                if (dataIdNode != null && StringUtils.isNotEmpty(dataIdNode.asText())) {
                    ValuedDataObject dataObject = null;
                    ItemDefinition itemSubjectRef = new ItemDefinition();
                    String dataType = dataNode.get(PROPERTY_DATA_TYPE).asText();

                    if ("string".equals(dataType)) {
                        dataObject = new StringDataObject();
                    } else if ("int".equals(dataType)) {
                        dataObject = new IntegerDataObject();
                    } else if ("long".equals(dataType)) {
                        dataObject = new LongDataObject();
                    } else if ("double".equals(dataType)) {
                        dataObject = new DoubleDataObject();
                    } else if ("boolean".equals(dataType)) {
                        dataObject = new BooleanDataObject();
                    } else if ("datetime".equals(dataType)) {
                        dataObject = new DateDataObject();
                    } else {
                        LOG.error("Error converting {}", dataIdNode.asText());
                    }

                    if (null != dataObject) {
                        dataObject.setId(dataIdNode.asText());
                        dataObject.setName(dataNode.get(PROPERTY_DATA_NAME).asText());

                        itemSubjectRef.setStructureRef("xsd:" + dataType);
                        dataObject.setItemSubjectRef(itemSubjectRef);

                        JsonNode valueNode = dataNode.get(PROPERTY_DATA_VALUE);
                        if (valueNode != null) {
                            String dateValue = valueNode.asText();
                            if (dataObject instanceof DateDataObject) {
                                try {
                                    if (!StringUtils.isEmpty(dateValue.trim())) {
                                        dataObject.setValue(DATE_TIME_FORMATTER.parseDateTime(dateValue).toDate());
                                    }
                                } catch (Exception e) {
                                    LOG.error("Error converting {}", dataObject.getName(), e);
                                }
                            } else {
                                dataObject.setValue(dateValue);
                            }
                        }

                        dataObjects.add(dataObject);
                    }
                }
            }
        }
        return dataObjects;
    }

    public static void convertDataPropertiesToJson(
            final List<ValuedDataObject> dataObjects,
            final ObjectNode propertiesNode) {

        ObjectNode dataPropertiesNode = JSON_MAPPER.createObjectNode();
        ArrayNode itemsNode = JSON_MAPPER.createArrayNode();

        for (ValuedDataObject dObj : dataObjects) {
            ObjectNode propertyItemNode = JSON_MAPPER.createObjectNode();
            propertyItemNode.put(PROPERTY_DATA_ID, dObj.getId());
            propertyItemNode.put(PROPERTY_DATA_NAME, dObj.getName());

            String itemSubjectRefQName = dObj.getItemSubjectRef().getStructureRef();
            // remove namespace prefix
            String dataType = itemSubjectRefQName.substring(itemSubjectRefQName.indexOf(':') + 1);
            propertyItemNode.put(PROPERTY_DATA_TYPE, dataType);

            Object dObjValue = dObj.getValue();
            if (null == dObjValue) {
                propertyItemNode.put(PROPERTY_DATA_VALUE, "");
            } else {
                String value;
                if ("datetime".equals(dataType)) {
                    value = new DateTime(dObjValue).toString("yyyy-MM-dd'T'hh:mm:ss");
                } else {
                    value = dObjValue.toString();
                }
                propertyItemNode.put(PROPERTY_DATA_VALUE, value);
            }

            itemsNode.add(propertyItemNode);
        }

        dataPropertiesNode.set(EDITOR_PROPERTIES_GENERAL_ITEMS, itemsNode);
        propertiesNode.set(PROPERTY_DATA_PROPERTIES, dataPropertiesNode);
    }

    public static JsonNode validateIfNodeIsTextual(final JsonNode node) {
        if (node != null && !node.isNull() && node.isTextual() && StringUtils.isNotEmpty(node.asText())) {
            try {
                return validateIfNodeIsTextual(JSON_MAPPER.readTree(node.asText()));
            } catch (Exception e) {
                LOG.error("Error converting textual node", e);
            }
        }
        return node;
    }

    public static String getValueAsString(final String name, final JsonNode objectNode) {
        String propertyValue = null;
        JsonNode propertyNode = objectNode.get(name);
        if (propertyNode != null && !propertyNode.isNull()) {
            propertyValue = propertyNode.asText();
        }
        return propertyValue;
    }

    public static String getPropertyValueAsString(final String name, final JsonNode objectNode) {
        String propertyValue = null;
        JsonNode propertyNode = getProperty(name, objectNode);
        if (propertyNode != null && !propertyNode.isNull()) {
            propertyValue = propertyNode.asText();
        }
        return propertyValue;
    }

    public static JsonNode getProperty(final String name, final JsonNode objectNode) {
        JsonNode propertyNode = null;
        if (objectNode.get(EDITOR_SHAPE_PROPERTIES) != null) {
            JsonNode propertiesNode = objectNode.get(EDITOR_SHAPE_PROPERTIES);
            propertyNode = propertiesNode.get(name);
        }
        return propertyNode;
    }

    private BpmnJsonConverterUtil() {
        // private constructor for static utility class
    }
}
