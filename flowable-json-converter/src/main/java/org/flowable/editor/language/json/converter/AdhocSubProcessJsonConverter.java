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
import static org.flowable.editor.language.json.converter.util.JsonConverterUtil.getPropertyValueAsString;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import org.flowable.bpmn.model.AdhocSubProcess;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.GraphicInfo;

/**
 * @author Tijs Rademakers
 */
public class AdhocSubProcessJsonConverter extends BaseBpmnJsonConverter {

    public static void fillTypes(
            final Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap,
            final Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {

        fillJsonTypes(convertersToBpmnMap);
        fillBpmnTypes(convertersToJsonMap);
    }

    public static void fillJsonTypes(
            final Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap) {

        convertersToBpmnMap.put(STENCIL_ADHOC_SUB_PROCESS, AdhocSubProcessJsonConverter.class);
    }

    public static void fillBpmnTypes(
            final Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {

        convertersToJsonMap.put(AdhocSubProcess.class, AdhocSubProcessJsonConverter.class);
    }

    @Override
    protected String getStencilId(final BaseElement baseElement) {
        return STENCIL_ADHOC_SUB_PROCESS;
    }

    @Override
    protected void convertElementToJson(
            final ObjectNode propertiesNode,
            final BaseElement baseElement,
            final BpmnJsonConverterContext converterContext) {

        AdhocSubProcess subProcess = (AdhocSubProcess) baseElement;
        propertiesNode.put("completioncondition", subProcess.getCompletionCondition());
        propertiesNode.put("ordering", subProcess.getOrdering());
        propertiesNode.put("cancelremaininginstances", subProcess.isCancelRemainingInstances());
        ArrayNode subProcessShapesArrayNode = JSON_MAPPER.createArrayNode();
        GraphicInfo graphicInfo = model.getGraphicInfo(subProcess.getId());
        processor.processFlowElements(
                subProcess, model, subProcessShapesArrayNode, converterContext, graphicInfo.getX(), graphicInfo.getY());
        flowElementNode.set("childShapes", subProcessShapesArrayNode);
    }

    @Override
    protected FlowElement convertJsonToElement(
            final JsonNode elementNode,
            final JsonNode modelNode,
            final Map<String, JsonNode> shapeMap,
            final BpmnJsonConverterContext converterContext) {

        AdhocSubProcess subProcess = new AdhocSubProcess();
        subProcess.setCompletionCondition(getPropertyValueAsString("completioncondition", elementNode));
        subProcess.setOrdering(getPropertyValueAsString("ordering", elementNode));
        subProcess.setCancelRemainingInstances(getPropertyValueAsBoolean("cancelremaininginstances", elementNode));
        JsonNode childShapesArray = elementNode.get(EDITOR_CHILD_SHAPES);
        processor.processJsonElements(childShapesArray, modelNode, subProcess, shapeMap, converterContext, model);
        return subProcess;
    }
}
