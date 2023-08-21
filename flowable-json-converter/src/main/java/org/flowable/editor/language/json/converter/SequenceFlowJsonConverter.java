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

import static org.flowable.editor.language.json.converter.util.JsonConverterUtil.getProperty;
import static org.flowable.editor.language.json.converter.util.JsonConverterUtil.getPropertyValueAsString;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExclusiveGateway;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowElementsContainer;
import org.flowable.bpmn.model.GraphicInfo;
import org.flowable.bpmn.model.InclusiveGateway;
import org.flowable.bpmn.model.SequenceFlow;

/**
 * @author Tijs Rademakers
 */
public class SequenceFlowJsonConverter extends BaseBpmnJsonConverter {

    public static void fillTypes(
            final Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap,
            final Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {

        fillJsonTypes(convertersToBpmnMap);
        fillBpmnTypes(convertersToJsonMap);
    }

    public static void fillJsonTypes(final Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap) {
        convertersToBpmnMap.put(STENCIL_SEQUENCE_FLOW, SequenceFlowJsonConverter.class);
    }

    public static void fillBpmnTypes(
            final Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {

        convertersToJsonMap.put(SequenceFlow.class, SequenceFlowJsonConverter.class);
    }

    @Override
    protected String getStencilId(final BaseElement baseElement) {
        return STENCIL_SEQUENCE_FLOW;
    }

    @Override
    public void convertToJson(
            final BpmnJsonConverterContext converterContext,
            final BaseElement baseElement,
            final ActivityProcessor processor,
            final BpmnModel model,
            final FlowElementsContainer container,
            final ArrayNode shapesArrayNode,
            final double subProcessX,
            final double subProcessY) {

        SequenceFlow sequenceFlow = (SequenceFlow) baseElement;
        ObjectNode flowNode = BpmnJsonConverterUtil.createChildShape(
                sequenceFlow.getId(), STENCIL_SEQUENCE_FLOW, 172, 212, 128, 212);
        ArrayNode dockersArrayNode = JSON_MAPPER.createArrayNode();
        ObjectNode dockNode = JSON_MAPPER.createObjectNode();
        dockNode.put(EDITOR_BOUNDS_X, model.getGraphicInfo(sequenceFlow.getSourceRef()).getWidth() / 2.0);
        dockNode.put(EDITOR_BOUNDS_Y, model.getGraphicInfo(sequenceFlow.getSourceRef()).getHeight() / 2.0);
        dockersArrayNode.add(dockNode);

        if (model.getFlowLocationGraphicInfo(sequenceFlow.getId()).size() > 2) {
            for (int i = 1; i < model.getFlowLocationGraphicInfo(sequenceFlow.getId()).size() - 1; i++) {
                GraphicInfo graphicInfo = model.getFlowLocationGraphicInfo(sequenceFlow.getId()).get(i);
                dockNode = JSON_MAPPER.createObjectNode();
                dockNode.put(EDITOR_BOUNDS_X, graphicInfo.getX());
                dockNode.put(EDITOR_BOUNDS_Y, graphicInfo.getY());
                dockersArrayNode.add(dockNode);
            }
        }

        dockNode = JSON_MAPPER.createObjectNode();
        dockNode.put(EDITOR_BOUNDS_X, model.getGraphicInfo(sequenceFlow.getTargetRef()).getWidth() / 2.0);
        dockNode.put(EDITOR_BOUNDS_Y, model.getGraphicInfo(sequenceFlow.getTargetRef()).getHeight() / 2.0);
        dockersArrayNode.add(dockNode);
        flowNode.set("dockers", dockersArrayNode);
        ArrayNode outgoingArrayNode = JSON_MAPPER.createArrayNode();
        outgoingArrayNode.add(BpmnJsonConverterUtil.createResourceNode(sequenceFlow.getTargetRef()));
        flowNode.set("outgoing", outgoingArrayNode);
        flowNode.set("target", BpmnJsonConverterUtil.createResourceNode(sequenceFlow.getTargetRef()));

        ObjectNode propertiesNode = JSON_MAPPER.createObjectNode();
        propertiesNode.put(PROPERTY_OVERRIDE_ID, sequenceFlow.getId());
        if (StringUtils.isNotEmpty(sequenceFlow.getName())) {
            propertiesNode.put(PROPERTY_NAME, sequenceFlow.getName());
        }

        if (StringUtils.isNotEmpty(sequenceFlow.getDocumentation())) {
            propertiesNode.put(PROPERTY_DOCUMENTATION, sequenceFlow.getDocumentation());
        }

        if (StringUtils.isNotEmpty(sequenceFlow.getConditionExpression())) {
            propertiesNode.put(PROPERTY_SEQUENCEFLOW_CONDITION, sequenceFlow.getConditionExpression());
        }

        if (StringUtils.isNotEmpty(sequenceFlow.getSourceRef())) {
            FlowElement sourceFlowElement = container.getFlowElement(sequenceFlow.getSourceRef());
            if (sourceFlowElement != null) {
                String defaultFlowId = null;
                if (sourceFlowElement instanceof ExclusiveGateway parentExclusiveGateway) {
                    defaultFlowId = parentExclusiveGateway.getDefaultFlow();
                } else if (sourceFlowElement instanceof InclusiveGateway parentInclusiveGateway) {
                    defaultFlowId = parentInclusiveGateway.getDefaultFlow();
                } else if (sourceFlowElement instanceof Activity parentActivity) {
                    defaultFlowId = parentActivity.getDefaultFlow();
                }

                if (defaultFlowId != null && defaultFlowId.equals(sequenceFlow.getId())) {
                    propertiesNode.put(PROPERTY_SEQUENCEFLOW_DEFAULT, true);
                }
            }
        }

        setPropertyValue(PROPERTY_SKIP_EXPRESSION, sequenceFlow.getSkipExpression(), propertiesNode);

        if (!sequenceFlow.getExecutionListeners().isEmpty()) {
            BpmnJsonConverterUtil.convertListenersToJson(sequenceFlow.getExecutionListeners(), true, propertiesNode);
        }

        flowNode.set(EDITOR_SHAPE_PROPERTIES, propertiesNode);
        shapesArrayNode.add(flowNode);
    }

    @Override
    protected void convertElementToJson(
            final ObjectNode propertiesNode,
            final BaseElement baseElement,
            final BpmnJsonConverterContext converterContext) {

        // nothing to do
    }

    @Override
    protected FlowElement convertJsonToElement(
            final JsonNode elementNode,
            final JsonNode modelNode,
            final Map<String, JsonNode> shapeMap,
            final BpmnJsonConverterContext converterContext) {

        SequenceFlow flow = new SequenceFlow();

        String sourceRef = BpmnJsonConverterUtil.lookForSourceRef(
                elementNode.get(EDITOR_SHAPE_ID).asText(), modelNode.get(EDITOR_CHILD_SHAPES));
        if (sourceRef != null) {
            flow.setSourceRef(sourceRef);
            JsonNode targetNode = elementNode.get("target");
            if (targetNode != null && !targetNode.isNull()) {
                String targetId = targetNode.get(EDITOR_SHAPE_ID).asText();
                if (shapeMap.get(targetId) != null) {
                    flow.setTargetRef(BpmnJsonConverterUtil.getElementId(shapeMap.get(targetId)));
                }
            }
        }

        JsonNode conditionNode = getProperty(PROPERTY_SEQUENCEFLOW_CONDITION, elementNode);
        if (conditionNode != null) {
            if (conditionNode.isTextual() && !conditionNode.isNull()) {
                flow.setConditionExpression(conditionNode.asText());
            } else if (conditionNode.get("expression") != null) {
                JsonNode expressionNode = conditionNode.get("expression");
                if (expressionNode.get("type") != null) {
                    String expressionType = expressionNode.get("type").asText();

                    if ("variables".equalsIgnoreCase(expressionType) && expressionNode.get("fieldType") != null) {
                        String fieldType = expressionNode.get("fieldType").asText();

                        if ("field".equalsIgnoreCase(fieldType)) {
                            setFieldConditionExpression(flow, expressionNode);

                        } else if ("outcome".equalsIgnoreCase(fieldType)) {
                            setOutcomeConditionExpression(flow, expressionNode);
                        }
                    } else if (expressionNode.get("staticValue") != null
                            && !expressionNode.get("staticValue").isNull()) {

                        flow.setConditionExpression(expressionNode.get("staticValue").asText());
                    }
                }
            }
        }

        flow.setSkipExpression(getPropertyValueAsString(PROPERTY_SKIP_EXPRESSION, elementNode));

        return flow;
    }

    protected static void setFieldConditionExpression(final SequenceFlow flow, final JsonNode expressionNode) {
        String fieldId = null;
        if (expressionNode.get("fieldId") != null && !expressionNode.get("fieldId").isNull()) {
            fieldId = expressionNode.get("fieldId").asText();
        }

        String operator = null;
        if (expressionNode.get("operator") != null && !expressionNode.get("operator").isNull()) {
            operator = expressionNode.get("operator").asText();
        }

        String value = null;
        if (expressionNode.get("value") != null && !expressionNode.get("value").isNull()) {
            value = expressionNode.get("value").asText();
        }

        if (fieldId != null && operator != null && value != null) {
            flow.setConditionExpression("${" + fieldId + " " + operator + " " + value + "}");
            addExtensionElement("conditionFieldId", fieldId, flow);
            addExtensionElement("conditionOperator", operator, flow);
            addExtensionElement("conditionValue", value, flow);
        }
    }

    protected static void setOutcomeConditionExpression(final SequenceFlow flow, final JsonNode expressionNode) {
        Long formId = null;
        if (expressionNode.get("outcomeFormId") != null && !expressionNode.get("outcomeFormId").isNull()) {
            formId = expressionNode.get("outcomeFormId").asLong();
        }

        String operator = null;
        if (expressionNode.get("operator") != null && !expressionNode.get("operator").isNull()) {
            operator = expressionNode.get("operator").asText();
        }

        String outcomeName = null;
        if (expressionNode.get("outcomeName") != null && !expressionNode.get("outcomeName").isNull()) {
            outcomeName = expressionNode.get("outcomeName").asText();
        }

        if (formId != null && operator != null && outcomeName != null) {
            flow.setConditionExpression("${form" + formId + "outcome " + operator + " " + outcomeName + "}");
            addExtensionElement("conditionFormId", String.valueOf(formId), flow);
            addExtensionElement("conditionOperator", operator, flow);
            addExtensionElement("conditionOutcomeName", outcomeName, flow);
        }
    }

    protected static void addExtensionElement(final String name, final String value, final SequenceFlow flow) {
        ExtensionElement extensionElement = new ExtensionElement();
        extensionElement.setNamespace(BpmnJsonConverter.MODELER_NAMESPACE);
        extensionElement.setNamespacePrefix("modeler");
        extensionElement.setName(name);
        extensionElement.setElementText(value);
        flow.addExtensionElement(extensionElement);
    }
}
