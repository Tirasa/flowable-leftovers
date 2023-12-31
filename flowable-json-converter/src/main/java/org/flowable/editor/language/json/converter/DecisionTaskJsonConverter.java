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
import static org.flowable.editor.language.json.converter.util.JsonConverterUtil.getPropertyValueAsBoolean;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.FieldExtension;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.ServiceTask;

/**
 * @author Tijs Rademakers
 * @author Yvo Swillens
 */
public class DecisionTaskJsonConverter extends BaseBpmnJsonConverter {

    protected static final String REFERENCE_TYPE_DECISION_TABLE = "decisionTable";

    protected static final String REFERENCE_TYPE_DECISION_SERVICE = "decisionService";

    public static void fillTypes(
            final Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap,
            final Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {

        fillJsonTypes(convertersToBpmnMap);
        fillBpmnTypes(convertersToJsonMap);
    }

    public static void fillJsonTypes(final Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap) {
        convertersToBpmnMap.put(STENCIL_TASK_DECISION, DecisionTaskJsonConverter.class);
    }

    public static void fillBpmnTypes(
            final Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {

    }

    @Override
    protected String getStencilId(final BaseElement baseElement) {
        return STENCIL_TASK_DECISION;
    }

    @Override
    protected void convertElementToJson(
            final ObjectNode propertiesNode,
            final BaseElement baseElement,
            final BpmnJsonConverterContext converterContext) {

    }

    @Override
    protected FlowElement convertJsonToElement(
            final JsonNode elementNode,
            final JsonNode modelNode,
            final Map<String, JsonNode> shapeMap,
            final BpmnJsonConverterContext converterContext) {

        ServiceTask serviceTask = new ServiceTask();
        serviceTask.setType(ServiceTask.DMN_TASK);

        String decisionModelKey = null;
        String referenceType = null;

        // when both decision table and decision service reference are present
        // decision services reference will prevail
        JsonNode decisionTableReferenceNode = getProperty(PROPERTY_DECISIONTABLE_REFERENCE, elementNode);
        if (decisionTableReferenceNode != null
                && decisionTableReferenceNode.has("id")
                && !decisionTableReferenceNode.get("id").isNull()) {

            String decisionTableId = decisionTableReferenceNode.get("id").asText();
            decisionModelKey = converterContext.getDecisionTableModelKeyForDecisionTableModelId(decisionTableId);
            referenceType = REFERENCE_TYPE_DECISION_TABLE;
        }

        JsonNode decisionServiceReferenceNode = getProperty(PROPERTY_DECISIONSERVICE_REFERENCE, elementNode);
        if (decisionServiceReferenceNode != null
                && decisionServiceReferenceNode.has("id")
                && !decisionServiceReferenceNode.get("id").isNull()) {

            String decisionServiceId = decisionServiceReferenceNode.get("id").asText();
            decisionModelKey = converterContext.getDecisionServiceModelKeyForDecisionServiceModelId(decisionServiceId);
            referenceType = REFERENCE_TYPE_DECISION_SERVICE;
        }

        if (decisionModelKey != null) {
            FieldExtension decisionTableKeyField = new FieldExtension();
            decisionTableKeyField.setFieldName(PROPERTY_DECISIONTABLE_REFERENCE_KEY);
            decisionTableKeyField.setStringValue(decisionModelKey);
            serviceTask.getFieldExtensions().add(decisionTableKeyField);
            referenceType = REFERENCE_TYPE_DECISION_SERVICE;
        }

        addFlowableExtensionElementWithValue(PROPERTY_DECISION_REFERENCE_TYPE, referenceType, serviceTask);

        addBooleanField(elementNode, serviceTask, PROPERTY_DECISIONTABLE_THROW_ERROR_NO_HITS,
                PROPERTY_DECISIONTABLE_THROW_ERROR_NO_HITS_KEY);
        addBooleanField(elementNode, serviceTask, PROPERTY_DECISIONTABLE_FALLBACK_TO_DEFAULT_TENANT,
                PROPERTY_DECISIONTABLE_FALLBACK_TO_DEFAULT_TENANT_KEY);
        addBooleanField(elementNode, serviceTask, PROPERTY_DECISIONTABLE_SAME_DEPLOYMENT,
                PROPERTY_DECISIONTABLE_SAME_DEPLOYMENT_KEY);

        return serviceTask;
    }

    protected static void addBooleanField(
            final JsonNode elementNode,
            final ServiceTask decisionTask,
            final String propertyName,
            final String fieldName) {

        boolean decisionTableThrowErrorOnNoHitsNode = getPropertyValueAsBoolean(propertyName, elementNode);
        FieldExtension decisionTableThrowErrorOnNoHitsField = new FieldExtension();
        decisionTableThrowErrorOnNoHitsField.setFieldName(fieldName);
        decisionTableThrowErrorOnNoHitsField.setStringValue(decisionTableThrowErrorOnNoHitsNode ? "true" : "false");
        decisionTask.getFieldExtensions().add(decisionTableThrowErrorOnNoHitsField);
    }
}
