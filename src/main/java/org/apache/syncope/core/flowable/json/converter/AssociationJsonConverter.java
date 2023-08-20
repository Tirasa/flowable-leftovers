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
package org.apache.syncope.core.flowable.json.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import org.flowable.bpmn.model.Association;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElementsContainer;
import org.flowable.bpmn.model.GraphicInfo;

/**
 * @author Tijs Rademakers
 */
public class AssociationJsonConverter extends BaseBpmnJsonConverter {

    public static void fillTypes(
            final Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap,
            final Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {

        fillJsonTypes(convertersToBpmnMap);
        fillBpmnTypes(convertersToJsonMap);
    }

    public static void fillJsonTypes(
            final Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap) {

        convertersToBpmnMap.put(STENCIL_ASSOCIATION, AssociationJsonConverter.class);
    }

    public static void fillBpmnTypes(
            final Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {

        convertersToJsonMap.put(Association.class, AssociationJsonConverter.class);
    }

    @Override
    protected String getStencilId(final BaseElement baseElement) {
        return STENCIL_ASSOCIATION;
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

        Association association = (Association) baseElement;
        ObjectNode flowNode = BpmnJsonConverterUtil.createChildShape(
                association.getId(), STENCIL_ASSOCIATION, 172, 212, 128, 212);
        ArrayNode dockersArrayNode = JSON_MAPPER.createArrayNode();
        ObjectNode dockNode = JSON_MAPPER.createObjectNode();
        dockNode.put(EDITOR_BOUNDS_X, model.getGraphicInfo(association.getSourceRef()).getWidth() / 2.0);
        dockNode.put(EDITOR_BOUNDS_Y, model.getGraphicInfo(association.getSourceRef()).getHeight() / 2.0);
        dockersArrayNode.add(dockNode);

        List<GraphicInfo> graphicInfoList = model.getFlowLocationGraphicInfo(association.getId());
        if (graphicInfoList.size() > 2) {
            for (int i = 1; i < graphicInfoList.size() - 1; i++) {
                GraphicInfo graphicInfo = graphicInfoList.get(i);
                dockNode = JSON_MAPPER.createObjectNode();
                dockNode.put(EDITOR_BOUNDS_X, graphicInfo.getX());
                dockNode.put(EDITOR_BOUNDS_Y, graphicInfo.getY());
                dockersArrayNode.add(dockNode);
            }
        }

        GraphicInfo targetGraphicInfo = model.getGraphicInfo(association.getTargetRef());
        GraphicInfo flowGraphicInfo = graphicInfoList.get(graphicInfoList.size() - 1);

        double diffTopY = Math.abs(flowGraphicInfo.getY() - targetGraphicInfo.getY());
        double diffRightX = Math.abs(
                flowGraphicInfo.getX() - (targetGraphicInfo.getX() + targetGraphicInfo.getWidth()));
        double diffBottomY = Math.abs(
                flowGraphicInfo.getY() - (targetGraphicInfo.getY() + targetGraphicInfo.getHeight()));

        dockNode = JSON_MAPPER.createObjectNode();
        if (diffTopY < 5) {
            dockNode.put(EDITOR_BOUNDS_X, targetGraphicInfo.getWidth() / 2.0);
            dockNode.put(EDITOR_BOUNDS_Y, 0.0);

        } else if (diffRightX < 5) {
            dockNode.put(EDITOR_BOUNDS_X, targetGraphicInfo.getWidth());
            dockNode.put(EDITOR_BOUNDS_Y, targetGraphicInfo.getHeight() / 2.0);

        } else if (diffBottomY < 5) {
            dockNode.put(EDITOR_BOUNDS_X, targetGraphicInfo.getWidth() / 2.0);
            dockNode.put(EDITOR_BOUNDS_Y, targetGraphicInfo.getHeight());

        } else {
            dockNode.put(EDITOR_BOUNDS_X, 0.0);
            dockNode.put(EDITOR_BOUNDS_Y, targetGraphicInfo.getHeight() / 2.0);
        }
        dockersArrayNode.add(dockNode);
        flowNode.set("dockers", dockersArrayNode);
        ArrayNode outgoingArrayNode = JSON_MAPPER.createArrayNode();
        outgoingArrayNode.add(BpmnJsonConverterUtil.createResourceNode(association.getTargetRef()));
        flowNode.set("outgoing", outgoingArrayNode);
        flowNode.set("target", BpmnJsonConverterUtil.createResourceNode(association.getTargetRef()));

        ObjectNode propertiesNode = JSON_MAPPER.createObjectNode();
        propertiesNode.put(PROPERTY_OVERRIDE_ID, association.getId());

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

        Association association = new Association();

        String sourceRef = BpmnJsonConverterUtil.lookForSourceRef(
                elementNode.get(EDITOR_SHAPE_ID).asText(), modelNode.get(EDITOR_CHILD_SHAPES));
        if (sourceRef != null) {
            association.setSourceRef(sourceRef);
            String targetId = elementNode.get("target").get(EDITOR_SHAPE_ID).asText();
            association.setTargetRef(BpmnJsonConverterUtil.getElementId(shapeMap.get(targetId)));
        }

        return association;
    }
}
