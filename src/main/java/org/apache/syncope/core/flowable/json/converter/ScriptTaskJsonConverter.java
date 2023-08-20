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

import static org.apache.syncope.core.flowable.json.converter.JsonConverterUtil.getPropertyValueAsBoolean;
import static org.apache.syncope.core.flowable.json.converter.JsonConverterUtil.getPropertyValueAsString;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.ScriptTask;

/**
 * @author Tijs Rademakers
 */
public class ScriptTaskJsonConverter extends BaseBpmnJsonConverter {

    public static void fillTypes(Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap, Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {

        fillJsonTypes(convertersToBpmnMap);
        fillBpmnTypes(convertersToJsonMap);
    }

    public static void fillJsonTypes(Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap) {
        convertersToBpmnMap.put(STENCIL_TASK_SCRIPT, ScriptTaskJsonConverter.class);
    }

    public static void fillBpmnTypes(Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {
        convertersToJsonMap.put(ScriptTask.class, ScriptTaskJsonConverter.class);
    }

    @Override
    protected String getStencilId(BaseElement baseElement) {
        return STENCIL_TASK_SCRIPT;
    }

    @Override
    protected void convertElementToJson(ObjectNode propertiesNode, BaseElement baseElement,
        BpmnJsonConverterContext converterContext) {
        ScriptTask scriptTask = (ScriptTask) baseElement;
        propertiesNode.put(PROPERTY_SCRIPT_FORMAT, scriptTask.getScriptFormat());
        propertiesNode.put(PROPERTY_SCRIPT_TEXT, scriptTask.getScript());
        propertiesNode.put(PROPERTY_SKIP_EXPRESSION, scriptTask.getSkipExpression());
        propertiesNode.put(PROPERTY_SCRIPT_AUTO_STORE_VARIABLES, scriptTask.isAutoStoreVariables());
    }

    @Override
    protected FlowElement convertJsonToElement(JsonNode elementNode, JsonNode modelNode, Map<String, JsonNode> shapeMap,
        BpmnJsonConverterContext converterContext) {
        ScriptTask task = new ScriptTask();
        task.setScriptFormat(getPropertyValueAsString(PROPERTY_SCRIPT_FORMAT, elementNode));
        task.setScript(getPropertyValueAsString(PROPERTY_SCRIPT_TEXT, elementNode));
        task.setSkipExpression(getPropertyValueAsString(PROPERTY_SKIP_EXPRESSION, elementNode));
        task.setAutoStoreVariables(getPropertyValueAsBoolean(PROPERTY_SCRIPT_AUTO_STORE_VARIABLES, elementNode));
        return task;
    }
}