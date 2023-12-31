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
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.FieldExtension;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.HttpServiceTask;
import org.flowable.bpmn.model.ImplementationType;
import org.flowable.bpmn.model.MapExceptionEntry;
import org.flowable.bpmn.model.ServiceTask;

/**
 * @author Tijs Rademakers
 */
public class ServiceTaskJsonConverter extends BaseBpmnJsonConverter {

    public static void fillTypes(
            final Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap,
            final Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {

        fillJsonTypes(convertersToBpmnMap);
        fillBpmnTypes(convertersToJsonMap);
    }

    public static void fillJsonTypes(final Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap) {
        convertersToBpmnMap.put(STENCIL_TASK_SERVICE, ServiceTaskJsonConverter.class);
    }

    public static void fillBpmnTypes(
            final Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {

        convertersToJsonMap.put(ServiceTask.class, ServiceTaskJsonConverter.class);
        convertersToJsonMap.put(HttpServiceTask.class, ServiceTaskJsonConverter.class);
    }

    @Override
    protected String getStencilId(final BaseElement baseElement) {
        return STENCIL_TASK_SERVICE;
    }

    @Override
    protected void convertElementToJson(
            final ObjectNode propertiesNode,
            final BaseElement baseElement,
            final BpmnJsonConverterContext converterContext) {

        ServiceTask serviceTask = (ServiceTask) baseElement;

        setPropertyValue(PROPERTY_SKIP_EXPRESSION, serviceTask.getSkipExpression(), propertiesNode);

        if ("mail".equalsIgnoreCase(serviceTask.getType())) {
            setPropertyFieldValue(PROPERTY_MAILTASK_HEADERS, serviceTask, propertiesNode);
            setPropertyFieldValue(PROPERTY_MAILTASK_TO, serviceTask, propertiesNode);
            setPropertyFieldValue(PROPERTY_MAILTASK_FROM, serviceTask, propertiesNode);
            setPropertyFieldValue(PROPERTY_MAILTASK_SUBJECT, serviceTask, propertiesNode);
            setPropertyFieldValue(PROPERTY_MAILTASK_CC, serviceTask, propertiesNode);
            setPropertyFieldValue(PROPERTY_MAILTASK_BCC, serviceTask, propertiesNode);
            setPropertyFieldValue(PROPERTY_MAILTASK_TEXT, serviceTask, propertiesNode);
            setPropertyFieldValue(PROPERTY_MAILTASK_HTML, serviceTask, propertiesNode);
            setPropertyFieldValue(PROPERTY_MAILTASK_HTML_VAR, serviceTask, propertiesNode);
            setPropertyFieldValue(PROPERTY_MAILTASK_TEXT_VAR, serviceTask, propertiesNode);
            setPropertyFieldValue(PROPERTY_MAILTASK_CHARSET, serviceTask, propertiesNode);
        } else if ("camel".equalsIgnoreCase(serviceTask.getType())) {
            setPropertyFieldValue(PROPERTY_CAMELTASK_CAMELCONTEXT, "camelContext", serviceTask, propertiesNode);
        } else if ("mule".equalsIgnoreCase(serviceTask.getType())) {
            setPropertyFieldValue(PROPERTY_MULETASK_ENDPOINT_URL, "endpointUrl", serviceTask, propertiesNode);
            setPropertyFieldValue(PROPERTY_MULETASK_LANGUAGE, "language", serviceTask, propertiesNode);
            setPropertyFieldValue(
                    PROPERTY_MULETASK_PAYLOAD_EXPRESSION, "payloadExpression", serviceTask, propertiesNode);
            setPropertyFieldValue(PROPERTY_MULETASK_RESULT_VARIABLE, "resultVariable", serviceTask, propertiesNode);
        } else if ("dmn".equalsIgnoreCase(serviceTask.getType())) {
            for (FieldExtension fieldExtension : serviceTask.getFieldExtensions()) {
                if (PROPERTY_DECISIONTABLE_REFERENCE_KEY.equals(fieldExtension.getFieldName())) {
                    Map<String, String> decisionServiceModelInfo = converterContext.
                            getDecisionServiceModelInfoForDecisionServiceModelKey(fieldExtension.getStringValue());
                    if (decisionServiceModelInfo != null) {
                        ObjectNode decisionServiceReferenceNode = JSON_MAPPER.createObjectNode();
                        propertiesNode.set(PROPERTY_DECISIONSERVICE_REFERENCE, decisionServiceReferenceNode);
                        decisionServiceReferenceNode.put("id", decisionServiceModelInfo.get("id"));
                        decisionServiceReferenceNode.put("name", decisionServiceModelInfo.get("name"));
                        decisionServiceReferenceNode.put("key", decisionServiceModelInfo.get("key"));
                    } else {
                        Map<String, String> decisionTableModelInfo = converterContext.
                                getDecisionTableModelInfoForDecisionTableModelKey(fieldExtension.getStringValue());
                        if (decisionTableModelInfo != null) {
                            ObjectNode decisionTableReferenceNode = JSON_MAPPER.createObjectNode();
                            propertiesNode.set(PROPERTY_DECISIONTABLE_REFERENCE, decisionTableReferenceNode);
                            decisionTableReferenceNode.put("id", decisionTableModelInfo.get("id"));
                            decisionTableReferenceNode.put("name", decisionTableModelInfo.get("name"));
                            decisionTableReferenceNode.put("key", decisionTableModelInfo.get("key"));
                        }
                    }
                } else if (PROPERTY_DECISIONTABLE_THROW_ERROR_NO_HITS_KEY.equals(fieldExtension.getFieldName())) {
                    propertiesNode.set(PROPERTY_DECISIONTABLE_THROW_ERROR_NO_HITS,
                            BooleanNode.valueOf(Boolean.parseBoolean(fieldExtension.getStringValue())));
                }
                if (PROPERTY_DECISIONTABLE_FALLBACK_TO_DEFAULT_TENANT_KEY.equals(fieldExtension.getFieldName())) {
                    propertiesNode.set(PROPERTY_DECISIONTABLE_FALLBACK_TO_DEFAULT_TENANT,
                            BooleanNode.valueOf(Boolean.parseBoolean(fieldExtension.getStringValue())));
                }
                if (PROPERTY_DECISIONTABLE_SAME_DEPLOYMENT_KEY.equals(fieldExtension.getFieldName())) {
                    propertiesNode.set(PROPERTY_DECISIONTABLE_SAME_DEPLOYMENT,
                            BooleanNode.valueOf(Boolean.parseBoolean(fieldExtension.getStringValue())));
                }
            }
        } else if ("http".equalsIgnoreCase(serviceTask.getType())) {
            setPropertyFieldValue(PROPERTY_HTTPTASK_REQ_METHOD, "requestMethod", serviceTask, propertiesNode);
            setPropertyFieldValue(PROPERTY_HTTPTASK_REQ_URL, "requestUrl", serviceTask, propertiesNode);
            setPropertyFieldValue(PROPERTY_HTTPTASK_REQ_HEADERS, "requestHeaders", serviceTask, propertiesNode);
            setPropertyFieldValue(PROPERTY_HTTPTASK_REQ_BODY, "requestBody", serviceTask, propertiesNode);
            setPropertyFieldValue(
                    PROPERTY_HTTPTASK_REQ_BODY_ENCODING, "requestBodyEncoding", serviceTask, propertiesNode);
            setPropertyFieldValue(PROPERTY_HTTPTASK_REQ_TIMEOUT, "requestTimeout", serviceTask, propertiesNode);
            setPropertyFieldValue(
                    PROPERTY_HTTPTASK_REQ_DISALLOW_REDIRECTS, "disallowRedirects", serviceTask, propertiesNode);
            setPropertyFieldValue(
                    PROPERTY_HTTPTASK_REQ_FAIL_STATUS_CODES, "failStatusCodes", serviceTask, propertiesNode);
            setPropertyFieldValue(
                    PROPERTY_HTTPTASK_REQ_HANDLE_STATUS_CODES, "handleStatusCodes", serviceTask, propertiesNode);
            setPropertyFieldValue(
                    PROPERTY_HTTPTASK_RESPONSE_VARIABLE_NAME, "responseVariableName", serviceTask, propertiesNode);
            setPropertyFieldValue(
                    PROPERTY_HTTPTASK_REQ_IGNORE_EXCEPTION, "ignoreException", serviceTask, propertiesNode);
            setPropertyFieldValue(
                    PROPERTY_HTTPTASK_SAVE_REQUEST_VARIABLES, "saveRequestVariables", serviceTask, propertiesNode);
            setPropertyFieldValue(
                    PROPERTY_HTTPTASK_SAVE_RESPONSE_PARAMETERS, "saveResponseParameters", serviceTask, propertiesNode);
            setPropertyFieldValue(
                    PROPERTY_HTTPTASK_RESULT_VARIABLE_PREFIX, "resultVariablePrefix", serviceTask, propertiesNode);
            setPropertyFieldValue(
                    PROPERTY_HTTPTASK_SAVE_RESPONSE_TRANSIENT,
                    "saveResponseParametersTransient",
                    serviceTask,
                    propertiesNode);
            setPropertyFieldValue(
                    PROPERTY_HTTPTASK_SAVE_RESPONSE_AS_JSON, "saveResponseVariableAsJson", serviceTask, propertiesNode);

            Boolean parallelInSameTransaction = ((HttpServiceTask) serviceTask).getParallelInSameTransaction();
            if (parallelInSameTransaction != null) {
                setPropertyValue(
                        PROPERTY_HTTPTASK_PARALLEL_IN_SAME_TRANSACTION,
                        parallelInSameTransaction.toString(),
                        propertiesNode);
            }
        } else if ("shell".equalsIgnoreCase(serviceTask.getType())) {
            setPropertyFieldValue(PROPERTY_SHELLTASK_COMMAND, "command", serviceTask, propertiesNode);
            setPropertyFieldValue(PROPERTY_SHELLTASK_ARG1, "arg1", serviceTask, propertiesNode);
            setPropertyFieldValue(PROPERTY_SHELLTASK_ARG2, "arg2", serviceTask, propertiesNode);
            setPropertyFieldValue(PROPERTY_SHELLTASK_ARG3, "arg3", serviceTask, propertiesNode);
            setPropertyFieldValue(PROPERTY_SHELLTASK_ARG4, "arg4", serviceTask, propertiesNode);
            setPropertyFieldValue(PROPERTY_SHELLTASK_ARG5, "arg5", serviceTask, propertiesNode);
            setPropertyFieldValue(PROPERTY_SHELLTASK_CLEAN_ENV, "cleanEnv", serviceTask, propertiesNode);
            setPropertyFieldValue(PROPERTY_SHELLTASK_DIRECTORY, "directory", serviceTask, propertiesNode);
            setPropertyFieldValue(
                    PROPERTY_SHELLTASK_ERROR_CODE_VARIABLE, "errorCodeVariable", serviceTask, propertiesNode);
            setPropertyFieldValue(PROPERTY_SHELLTASK_ERROR_REDIRECT, "errorRedirect", serviceTask, propertiesNode);
            setPropertyFieldValue(PROPERTY_SHELLTASK_OUTPUT_VARIABLE, "outputVariable", serviceTask, propertiesNode);
            setPropertyFieldValue(PROPERTY_SHELLTASK_WAIT, "wait", serviceTask, propertiesNode);
        } else {
            if (ImplementationType.IMPLEMENTATION_TYPE_CLASS.equals(serviceTask.getImplementationType())) {
                propertiesNode.put(PROPERTY_SERVICETASK_CLASS, serviceTask.getImplementation());
            } else if (ImplementationType.IMPLEMENTATION_TYPE_EXPRESSION.equals(serviceTask.getImplementationType())) {
                propertiesNode.put(PROPERTY_SERVICETASK_EXPRESSION, serviceTask.getImplementation());
            } else if (ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION.equals(
                    serviceTask.getImplementationType())) {

                propertiesNode.put(PROPERTY_SERVICETASK_DELEGATE_EXPRESSION, serviceTask.getImplementation());
            }

            if (serviceTask.isTriggerable()) {
                propertiesNode.put(PROPERTY_SERVICETASK_TRIGGERABLE, serviceTask.isTriggerable());
            }

            if (StringUtils.isNotEmpty(serviceTask.getResultVariableName())) {
                propertiesNode.put(PROPERTY_SERVICETASK_RESULT_VARIABLE, serviceTask.getResultVariableName());
            }

            if (serviceTask.isUseLocalScopeForResultVariable()) {
                propertiesNode.put(
                        PROPERTY_SERVICETASK_USE_LOCAL_SCOPE_FOR_RESULT_VARIABLE,
                        serviceTask.isUseLocalScopeForResultVariable());
            }

            if (serviceTask.isStoreResultVariableAsTransient()) {
                propertiesNode.put(
                        PROPERTY_SERVICETASK_STORE_TRANSIENT_VARIABLE,
                        serviceTask.isStoreResultVariableAsTransient());
            }

            if (StringUtils.isNotEmpty(serviceTask.getFailedJobRetryTimeCycleValue())) {
                propertiesNode.put(
                        PROPERTY_SERVICETASK_FAILED_JOB_RETRY_TIME_CYCLE,
                        serviceTask.getFailedJobRetryTimeCycleValue());
            }

            addFieldExtensions(serviceTask.getFieldExtensions(), propertiesNode);
            addMapException(serviceTask.getMapExceptions(), propertiesNode);
        }
    }

    @Override
    protected FlowElement convertJsonToElement(
            final JsonNode elementNode,
            final JsonNode modelNode,
            final Map<String, JsonNode> shapeMap,
            final BpmnJsonConverterContext converterContext) {

        ServiceTask task = new ServiceTask();
        if (StringUtils.isNotEmpty(getPropertyValueAsString(PROPERTY_SERVICETASK_CLASS, elementNode))) {
            task.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);
            task.setImplementation(getPropertyValueAsString(PROPERTY_SERVICETASK_CLASS, elementNode));
        } else if (StringUtils.isNotEmpty(getPropertyValueAsString(PROPERTY_SERVICETASK_EXPRESSION, elementNode))) {
            task.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_EXPRESSION);
            task.setImplementation(getPropertyValueAsString(PROPERTY_SERVICETASK_EXPRESSION, elementNode));
        } else if (StringUtils.isNotEmpty(
                getPropertyValueAsString(PROPERTY_SERVICETASK_DELEGATE_EXPRESSION, elementNode))) {

            task.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION);
            task.setImplementation(getPropertyValueAsString(PROPERTY_SERVICETASK_DELEGATE_EXPRESSION, elementNode));
        }

        if (getPropertyValueAsBoolean(PROPERTY_SERVICETASK_TRIGGERABLE, elementNode)) {
            task.setTriggerable(true);
        }

        if (StringUtils.isNotEmpty(getPropertyValueAsString(PROPERTY_SERVICETASK_RESULT_VARIABLE, elementNode))) {
            task.setResultVariableName(getPropertyValueAsString(PROPERTY_SERVICETASK_RESULT_VARIABLE, elementNode));
        }

        if (getPropertyValueAsBoolean(PROPERTY_SERVICETASK_USE_LOCAL_SCOPE_FOR_RESULT_VARIABLE, elementNode)) {
            task.setUseLocalScopeForResultVariable(true);
        }

        if (getPropertyValueAsBoolean(PROPERTY_SERVICETASK_STORE_TRANSIENT_VARIABLE, elementNode)) {
            task.setStoreResultVariableAsTransient(true);
        }

        if (StringUtils.isNotEmpty(getPropertyValueAsString(
                PROPERTY_SERVICETASK_FAILED_JOB_RETRY_TIME_CYCLE, elementNode))) {

            task.setFailedJobRetryTimeCycleValue(getPropertyValueAsString(
                    PROPERTY_SERVICETASK_FAILED_JOB_RETRY_TIME_CYCLE, elementNode));
        }

        task.setSkipExpression(getPropertyValueAsString(PROPERTY_SKIP_EXPRESSION, elementNode));

        JsonNode fieldsNode = getProperty(PROPERTY_SERVICETASK_FIELDS, elementNode);
        if (fieldsNode != null) {
            JsonNode itemsArrayNode = fieldsNode.get("fields");
            if (itemsArrayNode != null) {
                for (JsonNode itemNode : itemsArrayNode) {
                    JsonNode nameNode = itemNode.get(PROPERTY_SERVICETASK_FIELD_NAME);
                    if (nameNode != null && StringUtils.isNotEmpty(nameNode.asText())) {
                        FieldExtension field = new FieldExtension();
                        field.setFieldName(nameNode.asText());
                        if (StringUtils.isNotEmpty(
                                getValueAsString(PROPERTY_SERVICETASK_FIELD_STRING_VALUE, itemNode))) {

                            field.setStringValue(
                                    getValueAsString(PROPERTY_SERVICETASK_FIELD_STRING_VALUE, itemNode));
                        } else if (StringUtils.isNotEmpty(
                                getValueAsString(PROPERTY_SERVICETASK_FIELD_STRING, itemNode))) {

                            field.setStringValue(getValueAsString(PROPERTY_SERVICETASK_FIELD_STRING, itemNode));
                        } else if (StringUtils.isNotEmpty(
                                getValueAsString(PROPERTY_SERVICETASK_FIELD_EXPRESSION, itemNode))) {

                            field.setExpression(getValueAsString(PROPERTY_SERVICETASK_FIELD_EXPRESSION, itemNode));
                        }
                        task.getFieldExtensions().add(field);
                    }
                }
            }
        }

        JsonNode exceptionsNode = getProperty(PROPERTY_SERVICETASK_EXCEPTIONS, elementNode);
        if (exceptionsNode != null) {
            JsonNode itemsArrayNode = exceptionsNode.get("exceptions");
            if (itemsArrayNode != null) {
                for (JsonNode itemNode : itemsArrayNode) {
                    MapExceptionEntry exception = new MapExceptionEntry();

                    exception.setClassName(getValueAsString(PROPERTY_SERVICETASK_EXCEPTION_CLASS, itemNode));
                    exception.setErrorCode(getValueAsString(PROPERTY_SERVICETASK_EXCEPTION_CODE, itemNode));
                    exception.setAndChildren(getValueAsBoolean(PROPERTY_SERVICETASK_EXCEPTION_CHILDREN, itemNode));
                    task.getMapExceptions().add(exception);
                }
            }
        }

        return task;
    }

    protected void setPropertyFieldValue(final String name, final ServiceTask task, final ObjectNode propertiesNode) {
        for (FieldExtension extension : task.getFieldExtensions()) {
            if (name.substring(8).equalsIgnoreCase(extension.getFieldName())) {
                if (StringUtils.isNotEmpty(extension.getStringValue())) {
                    setPropertyValue(name, extension.getStringValue(), propertiesNode);
                } else if (StringUtils.isNotEmpty(extension.getExpression())) {
                    setPropertyValue(name, extension.getExpression(), propertiesNode);
                }
            }
        }
    }

    protected void setPropertyFieldValue(
            final String propertyName,
            final String fieldName,
            final ServiceTask task,
            final ObjectNode propertiesNode) {

        for (FieldExtension extension : task.getFieldExtensions()) {
            if (fieldName.equalsIgnoreCase(extension.getFieldName())) {
                if (StringUtils.isNotEmpty(extension.getStringValue())) {
                    setPropertyValue(propertyName, extension.getStringValue(), propertiesNode);
                } else if (StringUtils.isNotEmpty(extension.getExpression())) {
                    setPropertyValue(propertyName, extension.getExpression(), propertiesNode);
                }
            }
        }
    }
}
