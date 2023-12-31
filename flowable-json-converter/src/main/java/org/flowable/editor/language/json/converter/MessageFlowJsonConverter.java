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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElementsContainer;
import org.flowable.bpmn.model.GraphicInfo;
import org.flowable.bpmn.model.MessageFlow;

/**
 * @author Tijs Rademakers
 */
public class MessageFlowJsonConverter extends BaseBpmnJsonConverter {

    public static void fillTypes(
            final Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap,
            final Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {

        fillJsonTypes(convertersToBpmnMap);
        fillBpmnTypes(convertersToJsonMap);
    }

    public static void fillJsonTypes(final Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap) {
        convertersToBpmnMap.put(STENCIL_MESSAGE_FLOW, MessageFlowJsonConverter.class);
    }

    public static void fillBpmnTypes(
            final Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {

        convertersToJsonMap.put(MessageFlow.class, MessageFlowJsonConverter.class);
    }

    @Override
    protected String getStencilId(final BaseElement baseElement) {
        return STENCIL_MESSAGE_FLOW;
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

        MessageFlow messageFlow = (MessageFlow) baseElement;
        ObjectNode flowNode = BpmnJsonConverterUtil.createChildShape(
                messageFlow.getId(), STENCIL_MESSAGE_FLOW, 172, 212, 128, 212);
        ArrayNode dockersArrayNode = JSON_MAPPER.createArrayNode();
        ObjectNode dockNode = JSON_MAPPER.createObjectNode();
        dockNode.put(EDITOR_BOUNDS_X, model.getGraphicInfo(messageFlow.getSourceRef()).getWidth() / 2.0);
        dockNode.put(EDITOR_BOUNDS_Y, model.getGraphicInfo(messageFlow.getSourceRef()).getHeight() / 2.0);
        dockersArrayNode.add(dockNode);

        if (model.getFlowLocationGraphicInfo(messageFlow.getId()).size() > 2) {
            for (int i = 1; i < model.getFlowLocationGraphicInfo(messageFlow.getId()).size() - 1; i++) {
                GraphicInfo graphicInfo = model.getFlowLocationGraphicInfo(messageFlow.getId()).get(i);
                dockNode = JSON_MAPPER.createObjectNode();
                dockNode.put(EDITOR_BOUNDS_X, graphicInfo.getX());
                dockNode.put(EDITOR_BOUNDS_Y, graphicInfo.getY());
                dockersArrayNode.add(dockNode);
            }
        }

        dockNode = JSON_MAPPER.createObjectNode();
        dockNode.put(EDITOR_BOUNDS_X, model.getGraphicInfo(messageFlow.getTargetRef()).getWidth() / 2.0);
        dockNode.put(EDITOR_BOUNDS_Y, model.getGraphicInfo(messageFlow.getTargetRef()).getHeight() / 2.0);
        dockersArrayNode.add(dockNode);
        flowNode.set("dockers", dockersArrayNode);
        ArrayNode outgoingArrayNode = JSON_MAPPER.createArrayNode();
        outgoingArrayNode.add(BpmnJsonConverterUtil.createResourceNode(messageFlow.getTargetRef()));
        flowNode.set("outgoing", outgoingArrayNode);
        flowNode.set("target", BpmnJsonConverterUtil.createResourceNode(messageFlow.getTargetRef()));

        ObjectNode propertiesNode = JSON_MAPPER.createObjectNode();
        propertiesNode.put(PROPERTY_OVERRIDE_ID, messageFlow.getId());
        if (StringUtils.isNotEmpty(messageFlow.getName())) {
            propertiesNode.put(PROPERTY_NAME, messageFlow.getName());
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
    protected BaseElement convertJsonToElement(
            final JsonNode elementNode,
            final JsonNode modelNode,
            final Map<String, JsonNode> shapeMap,
            final BpmnJsonConverterContext converterContext) {

        MessageFlow flow = new MessageFlow();

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

        return flow;
    }
}
