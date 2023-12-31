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
import static org.flowable.editor.language.json.converter.util.JsonConverterUtil.getPropertyValueAsList;
import static org.flowable.editor.language.json.converter.util.JsonConverterUtil.getPropertyValueAsString;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.BusinessRuleTask;
import org.flowable.bpmn.model.FlowElement;

/**
 * @author Tijs Rademakers
 */
public class BusinessRuleTaskJsonConverter extends BaseBpmnJsonConverter {

    public static void fillTypes(
            final Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap,
            final Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {

        fillJsonTypes(convertersToBpmnMap);
        fillBpmnTypes(convertersToJsonMap);
    }

    public static void fillJsonTypes(final Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap) {
        convertersToBpmnMap.put(STENCIL_TASK_BUSINESS_RULE, BusinessRuleTaskJsonConverter.class);
    }

    public static void fillBpmnTypes(
            final Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {

        convertersToJsonMap.put(BusinessRuleTask.class, BusinessRuleTaskJsonConverter.class);
    }

    @Override
    protected String getStencilId(final BaseElement baseElement) {
        return STENCIL_TASK_BUSINESS_RULE;
    }

    @Override
    protected void convertElementToJson(
            final ObjectNode propertiesNode,
            final BaseElement baseElement,
            final BpmnJsonConverterContext converterContext) {

        BusinessRuleTask ruleTask = (BusinessRuleTask) baseElement;
        propertiesNode.put(PROPERTY_RULETASK_CLASS, ruleTask.getClassName());
        propertiesNode.put(
                PROPERTY_RULETASK_VARIABLES_INPUT,
                convertListToCommaSeparatedString(ruleTask.getInputVariables()));
        propertiesNode.put(PROPERTY_RULETASK_RESULT, ruleTask.getResultVariableName());
        propertiesNode.put(PROPERTY_RULETASK_RULES, convertListToCommaSeparatedString(ruleTask.getRuleNames()));
        if (ruleTask.isExclude()) {
            propertiesNode.put(PROPERTY_RULETASK_EXCLUDE, PROPERTY_VALUE_YES);
        }
    }

    @Override
    protected FlowElement convertJsonToElement(
            final JsonNode elementNode,
            final JsonNode modelNode,
            final Map<String, JsonNode> shapeMap,
            final BpmnJsonConverterContext converterContext) {

        BusinessRuleTask task = new BusinessRuleTask();
        task.setClassName(getPropertyValueAsString(PROPERTY_RULETASK_CLASS, elementNode));
        task.setInputVariables(getPropertyValueAsList(PROPERTY_RULETASK_VARIABLES_INPUT, elementNode));
        task.setResultVariableName(getPropertyValueAsString(PROPERTY_RULETASK_RESULT, elementNode));
        task.setRuleNames(getPropertyValueAsList(PROPERTY_RULETASK_RULES, elementNode));
        task.setExclude(getPropertyValueAsBoolean(PROPERTY_RULETASK_EXCLUDE, elementNode));
        return task;
    }
}
