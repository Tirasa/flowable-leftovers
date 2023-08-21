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
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.ServiceTask;

/**
 * @author Tijs Rademakers
 */
public class MailTaskJsonConverter extends BaseBpmnJsonConverter {

    public static void fillTypes(
            final Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap,
            final Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {

        fillJsonTypes(convertersToBpmnMap);
        fillBpmnTypes(convertersToJsonMap);
    }

    public static void fillJsonTypes(final Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap) {
        convertersToBpmnMap.put(STENCIL_TASK_MAIL, MailTaskJsonConverter.class);
    }

    public static void fillBpmnTypes(
            final Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {

        // will be handled by ServiceTaskJsonConverter
    }

    @Override
    protected String getStencilId(final BaseElement baseElement) {
        return STENCIL_TASK_MAIL;
    }

    @Override
    protected void convertElementToJson(
            final ObjectNode propertiesNode,
            final BaseElement baseElement,
            final BpmnJsonConverterContext converterContext) {

        // will be handled by ServiceTaskJsonConverter
    }

    @Override
    protected FlowElement convertJsonToElement(
            final JsonNode elementNode,
            final JsonNode modelNode,
            final Map<String, JsonNode> shapeMap,
            final BpmnJsonConverterContext converterContext) {

        ServiceTask task = new ServiceTask();
        task.setType(ServiceTask.MAIL_TASK);
        addField(PROPERTY_MAILTASK_HEADERS, elementNode, task);
        addField(PROPERTY_MAILTASK_TO, elementNode, task);
        addField(PROPERTY_MAILTASK_FROM, elementNode, task);
        addField(PROPERTY_MAILTASK_SUBJECT, elementNode, task);
        addField(PROPERTY_MAILTASK_CC, elementNode, task);
        addField(PROPERTY_MAILTASK_BCC, elementNode, task);
        addField(PROPERTY_MAILTASK_TEXT, elementNode, task);
        addField(PROPERTY_MAILTASK_HTML, elementNode, task);
        addField("htmlVar", PROPERTY_MAILTASK_HTML_VAR, null, elementNode, task);
        addField("textVar", PROPERTY_MAILTASK_TEXT_VAR, null, elementNode, task);
        addField(PROPERTY_MAILTASK_CHARSET, elementNode, task);

        return task;
    }
}
