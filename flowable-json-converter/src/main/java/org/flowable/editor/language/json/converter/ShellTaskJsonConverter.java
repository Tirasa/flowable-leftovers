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
 * This class converts {@link org.flowable.bpmn.model.ServiceTask} to json representation for modeler
 *
 * @author martin.grofcik
 */
public class ShellTaskJsonConverter extends BaseBpmnJsonConverter {

    public static void fillTypes(
            final Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap,
            final Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {

        fillJsonTypes(convertersToBpmnMap);
        fillBpmnTypes(convertersToJsonMap);
    }

    public static void fillJsonTypes(final Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap) {
        convertersToBpmnMap.put(STENCIL_TASK_SHELL, ShellTaskJsonConverter.class);
    }

    public static void fillBpmnTypes(
            final Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {
    }

    @Override
    protected String getStencilId(final BaseElement baseElement) {
        return STENCIL_TASK_SHELL;
    }

    @Override
    protected void convertElementToJson(
            final ObjectNode propertiesNode,
            final BaseElement baseElement,
            final BpmnJsonConverterContext converterContext) {

        // done in service task
    }

    @Override
    protected FlowElement convertJsonToElement(
            final JsonNode elementNode,
            final JsonNode modelNode,
            final Map<String, JsonNode> shapeMap,
            final BpmnJsonConverterContext converterContext) {

        ServiceTask task = new ServiceTask();
        task.setType("shell");
        addField("command", PROPERTY_SHELLTASK_COMMAND, null, elementNode, task);
        addField("arg1", PROPERTY_SHELLTASK_ARG1, null, elementNode, task);
        addField("arg2", PROPERTY_SHELLTASK_ARG2, null, elementNode, task);
        addField("arg3", PROPERTY_SHELLTASK_ARG3, null, elementNode, task);
        addField("arg4", PROPERTY_SHELLTASK_ARG4, null, elementNode, task);
        addField("arg5", PROPERTY_SHELLTASK_ARG5, null, elementNode, task);
        addField("wait", PROPERTY_SHELLTASK_WAIT, null, elementNode, task);
        addField("cleanEnv", PROPERTY_SHELLTASK_CLEAN_ENV, null, elementNode, task);
        addField("errorCodeVariable", PROPERTY_SHELLTASK_ERROR_CODE_VARIABLE, null, elementNode, task);
        addField("errorRedirect", PROPERTY_SHELLTASK_ERROR_REDIRECT, null, elementNode, task);
        addField("outputVariable", PROPERTY_SHELLTASK_OUTPUT_VARIABLE, null, elementNode, task);
        addField("directory", PROPERTY_SHELLTASK_DIRECTORY, null, elementNode, task);
        return task;
    }
}
