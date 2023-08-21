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

import static org.flowable.editor.language.json.converter.util.JsonConverterUtil.getPropertyValueAsBoolean;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.BooleanUtils;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.GraphicInfo;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.Transaction;
import org.flowable.bpmn.model.ValuedDataObject;

/**
 * @author Tijs Rademakers
 */
public class SubProcessJsonConverter extends BaseBpmnJsonConverter {

    public static void fillTypes(
            final Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap,
            final Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {

        fillJsonTypes(convertersToBpmnMap);
        fillBpmnTypes(convertersToJsonMap);
    }

    public static void fillJsonTypes(
            final Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap) {

        convertersToBpmnMap.put(STENCIL_SUB_PROCESS, SubProcessJsonConverter.class);
        convertersToBpmnMap.put(STENCIL_COLLAPSED_SUB_PROCESS, SubProcessJsonConverter.class);
    }

    public static void fillBpmnTypes(
            final Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {

        convertersToJsonMap.put(SubProcess.class, SubProcessJsonConverter.class);
        convertersToJsonMap.put(Transaction.class, SubProcessJsonConverter.class);
    }

    @Override
    protected String getStencilId(final BaseElement baseElement) {
        // see http://forum.flowable.org/t/collapsed-subprocess-navigation-in-the-web-based-bpmn-modeler/138/19
        GraphicInfo graphicInfo = model.getGraphicInfo(baseElement.getId());
        if (BooleanUtils.isFalse(graphicInfo.getExpanded())) {
            return STENCIL_COLLAPSED_SUB_PROCESS;
        }
        return STENCIL_SUB_PROCESS;
    }

    @Override
    protected void convertElementToJson(
            final ObjectNode propertiesNode,
            final BaseElement baseElement,
            final BpmnJsonConverterContext converterContext) {

        SubProcess subProcess = (SubProcess) baseElement;

        propertiesNode.put("activitytype", getStencilId(baseElement));
        GraphicInfo gi = model.getGraphicInfo(baseElement.getId());

        ArrayNode subProcessShapesArrayNode = JSON_MAPPER.createArrayNode();
        GraphicInfo graphicInfo = model.getGraphicInfo(subProcess.getId());

        if (BooleanUtils.isFalse(gi.getExpanded())) {
            processor.processFlowElements(
                    subProcess, model, subProcessShapesArrayNode, converterContext, 0, 0);
        } else {
            processor.processFlowElements(
                    subProcess, model, subProcessShapesArrayNode, converterContext,
                    graphicInfo.getX(), graphicInfo.getY());
        }

        flowElementNode.set("childShapes", subProcessShapesArrayNode);

        if (subProcess instanceof Transaction) {
            propertiesNode.put("istransaction", true);
        }

        BpmnJsonConverterUtil.convertDataPropertiesToJson(subProcess.getDataObjects(), propertiesNode);
    }

    @Override
    protected FlowElement convertJsonToElement(
            final JsonNode elementNode,
            final JsonNode modelNode,
            final Map<String, JsonNode> shapeMap,
            final BpmnJsonConverterContext converterContext) {

        SubProcess subProcess;
        if (getPropertyValueAsBoolean("istransaction", elementNode)) {
            subProcess = new Transaction();
        } else {
            subProcess = new SubProcess();
        }

        JsonNode childShapesArray = elementNode.get(EDITOR_CHILD_SHAPES);
        processor.processJsonElements(childShapesArray, modelNode, subProcess, shapeMap, converterContext, model);

        JsonNode processDataPropertiesNode = elementNode.get(EDITOR_SHAPE_PROPERTIES).get(PROPERTY_DATA_PROPERTIES);
        if (processDataPropertiesNode != null) {
            List<ValuedDataObject> dataObjects =
                    BpmnJsonConverterUtil.convertJsonToDataProperties(processDataPropertiesNode, subProcess);
            subProcess.setDataObjects(dataObjects);
            subProcess.getFlowElements().addAll(dataObjects);
        }

        //store correct conversion info...
        if (STENCIL_COLLAPSED_SUB_PROCESS.equals(BpmnJsonConverterUtil.getStencilId(elementNode))) {
            GraphicInfo graphicInfo = model.getGraphicInfo(BpmnJsonConverterUtil.getElementId(elementNode));
            graphicInfo.setExpanded(false); //default is null!
        }

        return subProcess;
    }
}
