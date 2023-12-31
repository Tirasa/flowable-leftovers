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
import static org.flowable.editor.language.json.converter.util.JsonConverterUtil.getPropertyValueAsString;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.CallActivity;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.IOParameter;

/**
 * @author Tijs Rademakers
 */
public class CallActivityJsonConverter extends BaseBpmnJsonConverter {

    public static void fillTypes(
            final Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap,
            final Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {

        fillJsonTypes(convertersToBpmnMap);
        fillBpmnTypes(convertersToJsonMap);
    }

    public static void fillJsonTypes(final Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap) {
        convertersToBpmnMap.put(STENCIL_CALL_ACTIVITY, CallActivityJsonConverter.class);
    }

    public static void fillBpmnTypes(
            final Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {

        convertersToJsonMap.put(CallActivity.class, CallActivityJsonConverter.class);
    }

    @Override
    protected String getStencilId(final BaseElement baseElement) {
        return STENCIL_CALL_ACTIVITY;
    }

    @Override
    protected void convertElementToJson(
            final ObjectNode propertiesNode,
            final BaseElement baseElement,
            final BpmnJsonConverterContext converterContext) {

        CallActivity callActivity = (CallActivity) baseElement;
        if (StringUtils.isNotEmpty(callActivity.getCalledElement())) {
            propertiesNode.put(PROPERTY_CALLACTIVITY_CALLEDELEMENT, callActivity.getCalledElement());
        }

        if (StringUtils.isNotEmpty(callActivity.getCalledElementType())) {
            propertiesNode.put(PROPERTY_CALLACTIVITY_CALLEDELEMENTTYPE, callActivity.getCalledElementType());
        }

        if (callActivity.isInheritVariables()) {
            propertiesNode.put(PROPERTY_CALLACTIVITY_INHERIT_VARIABLES, callActivity.isInheritVariables());
        }

        if (callActivity.isSameDeployment()) {
            propertiesNode.put(PROPERTY_CALLACTIVITY_SAME_DEPLOYMENT, callActivity.isSameDeployment());
        }

        if (StringUtils.isNotEmpty(callActivity.getProcessInstanceName())) {
            propertiesNode.put(PROPERTY_CALLACTIVITY_PROCESS_INSTANCE_NAME, callActivity.getProcessInstanceName());
        }

        if (StringUtils.isNotEmpty(callActivity.getBusinessKey())) {
            propertiesNode.put(PROPERTY_CALLACTIVITY_BUSINESS_KEY, callActivity.getBusinessKey());
        }

        if (callActivity.isInheritBusinessKey()) {
            propertiesNode.put(PROPERTY_CALLACTIVITY_INHERIT_BUSINESS_KEY, callActivity.isInheritBusinessKey());
        }

        if (callActivity.isUseLocalScopeForOutParameters()) {
            propertiesNode.put(
                    PROPERTY_CALLACTIVITY_USE_LOCALSCOPE_FOR_OUTPARAMETERS,
                    callActivity.isUseLocalScopeForOutParameters());
        }

        if (callActivity.isCompleteAsync()) {
            propertiesNode.put(PROPERTY_CALLACTIVITY_COMPLETE_ASYNC, callActivity.isCompleteAsync());
        }

        if (callActivity.getFallbackToDefaultTenant() != null) {
            propertiesNode.put(
                    PROPERTY_CALLACTIVITY_FALLBACK_TO_DEFAULT_TENANT,
                    callActivity.getFallbackToDefaultTenant());
        }

        if (callActivity.getProcessInstanceIdVariableName() != null) {
            propertiesNode.put(PROPERTY_CALLACTIVITY_ID_VARIABLE_NAME, callActivity.getProcessInstanceIdVariableName());
        }

        addJsonParameters(PROPERTY_CALLACTIVITY_IN, "inParameters", callActivity.getInParameters(), propertiesNode);
        addJsonParameters(PROPERTY_CALLACTIVITY_OUT, "outParameters", callActivity.getOutParameters(), propertiesNode);
    }

    private static void addJsonParameters(
            final String propertyName,
            final String valueName,
            final List<IOParameter> parameterList,
            final ObjectNode propertiesNode) {

        ObjectNode parametersNode = JSON_MAPPER.createObjectNode();
        ArrayNode itemsNode = JSON_MAPPER.createArrayNode();
        for (IOParameter parameter : parameterList) {
            ObjectNode parameterItemNode = JSON_MAPPER.createObjectNode();
            if (StringUtils.isNotEmpty(parameter.getSource())) {
                parameterItemNode.put(PROPERTY_IOPARAMETER_SOURCE, parameter.getSource());
            } else {
                parameterItemNode.putNull(PROPERTY_IOPARAMETER_SOURCE);
            }
            if (StringUtils.isNotEmpty(parameter.getTarget())) {
                parameterItemNode.put(PROPERTY_IOPARAMETER_TARGET, parameter.getTarget());
            } else {
                parameterItemNode.putNull(PROPERTY_IOPARAMETER_TARGET);
            }
            if (StringUtils.isNotEmpty(parameter.getSourceExpression())) {
                parameterItemNode.put(PROPERTY_IOPARAMETER_SOURCE_EXPRESSION, parameter.getSourceExpression());
            } else {
                parameterItemNode.putNull(PROPERTY_IOPARAMETER_SOURCE_EXPRESSION);
            }

            itemsNode.add(parameterItemNode);
        }

        parametersNode.set(valueName, itemsNode);
        propertiesNode.set(propertyName, parametersNode);
    }

    @Override
    protected FlowElement convertJsonToElement(
            final JsonNode elementNode,
            final JsonNode modelNode,
            final Map<String, JsonNode> shapeMap,
            final BpmnJsonConverterContext converterContext) {

        CallActivity callActivity = new CallActivity();
        if (StringUtils.isNotEmpty(getPropertyValueAsString(PROPERTY_CALLACTIVITY_CALLEDELEMENT, elementNode))) {
            callActivity.setCalledElement(getPropertyValueAsString(PROPERTY_CALLACTIVITY_CALLEDELEMENT, elementNode));
        }

        if (StringUtils.isNotEmpty(getPropertyValueAsString(PROPERTY_CALLACTIVITY_CALLEDELEMENTTYPE, elementNode))) {
            callActivity.setCalledElementType(
                    getPropertyValueAsString(PROPERTY_CALLACTIVITY_CALLEDELEMENTTYPE, elementNode));
        }

        if (getPropertyValueAsBoolean(PROPERTY_CALLACTIVITY_INHERIT_VARIABLES, elementNode)) {
            callActivity.setInheritVariables(true);
        }

        if (getPropertyValueAsBoolean(PROPERTY_CALLACTIVITY_SAME_DEPLOYMENT, elementNode)) {
            callActivity.setSameDeployment(true);
        }

        String processInstanceName = getPropertyValueAsString(PROPERTY_CALLACTIVITY_PROCESS_INSTANCE_NAME, elementNode);
        if (StringUtils.isNotEmpty(processInstanceName)) {
            callActivity.setProcessInstanceName(processInstanceName);
        }

        String businessKey = getPropertyValueAsString(PROPERTY_CALLACTIVITY_BUSINESS_KEY, elementNode);
        if (StringUtils.isNotEmpty(businessKey)) {
            callActivity.setBusinessKey(businessKey);
        }

        if (getPropertyValueAsBoolean(PROPERTY_CALLACTIVITY_INHERIT_BUSINESS_KEY, elementNode)) {
            callActivity.setInheritBusinessKey(true);
        }

        if (getPropertyValueAsBoolean(PROPERTY_CALLACTIVITY_USE_LOCALSCOPE_FOR_OUTPARAMETERS, elementNode)) {
            callActivity.setUseLocalScopeForOutParameters(true);
        }

        if (getPropertyValueAsBoolean(PROPERTY_CALLACTIVITY_COMPLETE_ASYNC, elementNode)) {
            callActivity.setCompleteAsync(true);
        }

        if (StringUtils.isNotEmpty(
                getPropertyValueAsString(PROPERTY_CALLACTIVITY_FALLBACK_TO_DEFAULT_TENANT, elementNode))) {

            callActivity.setFallbackToDefaultTenant(getPropertyValueAsBoolean(
                    PROPERTY_CALLACTIVITY_FALLBACK_TO_DEFAULT_TENANT, elementNode));
        }

        String idVariableName = getPropertyValueAsString(PROPERTY_CALLACTIVITY_ID_VARIABLE_NAME, elementNode);
        if (StringUtils.isNotEmpty(idVariableName)) {
            callActivity.setProcessInstanceIdVariableName(idVariableName);
        }

        callActivity.getInParameters().addAll(
                convertToIOParameters(PROPERTY_CALLACTIVITY_IN, "inParameters", elementNode));
        callActivity.getOutParameters().addAll(
                convertToIOParameters(PROPERTY_CALLACTIVITY_OUT, "outParameters", elementNode));

        return callActivity;
    }

    private static List<IOParameter> convertToIOParameters(
            final String propertyName, final String valueName, final JsonNode elementNode) {

        JsonNode parametersNode = getProperty(propertyName, elementNode);
        if (parametersNode == null) {
            return List.of();
        }

        parametersNode = BpmnJsonConverterUtil.validateIfNodeIsTextual(parametersNode);
        JsonNode itemsArrayNode = parametersNode.get(valueName);
        if (itemsArrayNode == null) {
            return List.of();
        }

        List<IOParameter> ioParameters = new ArrayList<>();
        for (JsonNode itemNode : itemsArrayNode) {
            JsonNode sourceNode = itemNode.get(PROPERTY_IOPARAMETER_SOURCE);
            JsonNode sourceExpressionNode = itemNode.get(PROPERTY_IOPARAMETER_SOURCE_EXPRESSION);
            if ((sourceNode != null
                    && StringUtils.isNotEmpty(sourceNode.asText()))
                    || (sourceExpressionNode != null && StringUtils.isNotEmpty(sourceExpressionNode.asText()))) {

                IOParameter parameter = new IOParameter();
                if (StringUtils.isNotEmpty(getValueAsString(PROPERTY_IOPARAMETER_SOURCE, itemNode))) {
                    parameter.setSource(getValueAsString(PROPERTY_IOPARAMETER_SOURCE, itemNode));
                } else if (StringUtils.isNotEmpty(
                        getValueAsString(PROPERTY_IOPARAMETER_SOURCE_EXPRESSION, itemNode))) {

                    parameter.setSourceExpression(
                            getValueAsString(PROPERTY_IOPARAMETER_SOURCE_EXPRESSION, itemNode));
                }
                if (StringUtils.isNotEmpty(getValueAsString(PROPERTY_IOPARAMETER_TARGET, itemNode))) {
                    parameter.setTarget(getValueAsString(PROPERTY_IOPARAMETER_TARGET, itemNode));
                }
                ioParameters.add(parameter);
            }
        }
        return ioParameters;
    }
}
