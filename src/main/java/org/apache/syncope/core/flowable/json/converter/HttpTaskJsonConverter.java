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

import static org.apache.syncope.core.flowable.json.converter.JsonConverterUtil.getPropertyValueAsString;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.HttpServiceTask;

/**
 * @author Tijs Rademakers
 */
public class HttpTaskJsonConverter extends BaseBpmnJsonConverter {

    public static void fillTypes(Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap, Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {

        fillJsonTypes(convertersToBpmnMap);
        fillBpmnTypes(convertersToJsonMap);
    }

    public static void fillJsonTypes(Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap) {
        convertersToBpmnMap.put(STENCIL_TASK_HTTP, HttpTaskJsonConverter.class);
    }

    public static void fillBpmnTypes(Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {
    }

    @Override
    protected String getStencilId(BaseElement baseElement) {
        return STENCIL_TASK_HTTP;
    }

    @Override
    protected void convertElementToJson(ObjectNode propertiesNode, BaseElement baseElement,
        BpmnJsonConverterContext converterContext) {
        // done in service task
    }

    @Override
    protected FlowElement convertJsonToElement(JsonNode elementNode, JsonNode modelNode, Map<String, JsonNode> shapeMap,
        BpmnJsonConverterContext converterContext) {
        HttpServiceTask task = new HttpServiceTask();
        task.setType("http");
        String parallelInSameTransaction = getPropertyValueAsString(PROPERTY_HTTPTASK_PARALLEL_IN_SAME_TRANSACTION, elementNode);
        if (StringUtils.isNotEmpty(parallelInSameTransaction)) {
            task.setParallelInSameTransaction(Boolean.valueOf(parallelInSameTransaction));
        }
        addField("requestMethod", PROPERTY_HTTPTASK_REQ_METHOD, "GET", elementNode, task);
        addField("requestUrl", PROPERTY_HTTPTASK_REQ_URL, null,elementNode, task);
        addField("requestHeaders", PROPERTY_HTTPTASK_REQ_HEADERS, null,elementNode, task);
        addField("requestBody", PROPERTY_HTTPTASK_REQ_BODY, null,elementNode, task);
        addField("requestBodyEncoding", PROPERTY_HTTPTASK_REQ_BODY_ENCODING, null,elementNode, task);
        addField("requestTimeout", PROPERTY_HTTPTASK_REQ_TIMEOUT, null,elementNode, task);
        addField("disallowRedirects", PROPERTY_HTTPTASK_REQ_DISALLOW_REDIRECTS, null,elementNode, task);
        addField("failStatusCodes", PROPERTY_HTTPTASK_REQ_FAIL_STATUS_CODES, null,elementNode, task);
        addField("handleStatusCodes", PROPERTY_HTTPTASK_REQ_HANDLE_STATUS_CODES, null,elementNode, task);
        addField("responseVariableName", PROPERTY_HTTPTASK_RESPONSE_VARIABLE_NAME, null,elementNode, task);
        addField("ignoreException", PROPERTY_HTTPTASK_REQ_IGNORE_EXCEPTION, null,elementNode, task);
        addField("saveRequestVariables", PROPERTY_HTTPTASK_SAVE_REQUEST_VARIABLES, null,elementNode, task);
        addField("saveResponseParameters", PROPERTY_HTTPTASK_SAVE_RESPONSE_PARAMETERS, null,elementNode, task);
        addField("resultVariablePrefix", PROPERTY_HTTPTASK_RESULT_VARIABLE_PREFIX, null,elementNode, task);
        addField("saveResponseParametersTransient", PROPERTY_HTTPTASK_SAVE_RESPONSE_TRANSIENT, null,elementNode, task);
        addField("saveResponseVariableAsJson", PROPERTY_HTTPTASK_SAVE_RESPONSE_AS_JSON, null,elementNode, task);

        task.setSkipExpression(getPropertyValueAsString(PROPERTY_SKIP_EXPRESSION, elementNode));
        return task;
    }
}