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
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.CompensateEventDefinition;
import org.flowable.bpmn.model.EscalationEventDefinition;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.SignalEventDefinition;
import org.flowable.bpmn.model.ThrowEvent;

/**
 * @author Tijs Rademakers
 */
public class ThrowEventJsonConverter extends BaseBpmnJsonConverter {

    public static void fillTypes(
            final Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap,
            final Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {

        fillJsonTypes(convertersToBpmnMap);
        fillBpmnTypes(convertersToJsonMap);
    }

    public static void fillJsonTypes(final Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap) {
        convertersToBpmnMap.put(STENCIL_EVENT_THROW_NONE, ThrowEventJsonConverter.class);
        convertersToBpmnMap.put(STENCIL_EVENT_THROW_SIGNAL, ThrowEventJsonConverter.class);
        convertersToBpmnMap.put(STENCIL_EVENT_THROW_ESCALATION, ThrowEventJsonConverter.class);
        convertersToBpmnMap.put(STENCIL_EVENT_THROW_COMPENSATION, ThrowEventJsonConverter.class);
    }

    public static void fillBpmnTypes(
            final Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {

        convertersToJsonMap.put(ThrowEvent.class, ThrowEventJsonConverter.class);
    }

    @Override
    protected String getStencilId(final BaseElement baseElement) {
        ThrowEvent throwEvent = (ThrowEvent) baseElement;
        List<EventDefinition> eventDefinitions = throwEvent.getEventDefinitions();
        if (eventDefinitions.size() != 1) {
            // return none event as default;
            return STENCIL_EVENT_THROW_NONE;
        }

        EventDefinition eventDefinition = eventDefinitions.get(0);
        if (eventDefinition instanceof SignalEventDefinition) {
            return STENCIL_EVENT_THROW_SIGNAL;
        }
        if (eventDefinition instanceof EscalationEventDefinition) {
            return STENCIL_EVENT_THROW_ESCALATION;
        }
        if (eventDefinition instanceof CompensateEventDefinition) {
            return STENCIL_EVENT_THROW_COMPENSATION;
        }
        return STENCIL_EVENT_THROW_NONE;
    }

    @Override
    protected void convertElementToJson(
            final ObjectNode propertiesNode,
            final BaseElement baseElement,
            final BpmnJsonConverterContext converterContext) {

        ThrowEvent throwEvent = (ThrowEvent) baseElement;
        if (throwEvent.isAsynchronous()) {
            propertiesNode.put(PROPERTY_ASYNCHRONOUS, true);
        }
        addEventProperties(throwEvent, propertiesNode);
    }

    @Override
    protected FlowElement convertJsonToElement(
            final JsonNode elementNode,
            final JsonNode modelNode,
            final Map<String, JsonNode> shapeMap,
            final BpmnJsonConverterContext converterContext) {

        ThrowEvent throwEvent = new ThrowEvent();
        boolean isAsync = getPropertyValueAsBoolean(PROPERTY_ASYNCHRONOUS, elementNode);
        if (isAsync) {
            throwEvent.setAsynchronous(isAsync);
        }

        String stencilId = BpmnJsonConverterUtil.getStencilId(elementNode);
        if (STENCIL_EVENT_THROW_SIGNAL.equals(stencilId)) {
            convertJsonToSignalDefinition(elementNode, throwEvent);
        } else if (STENCIL_EVENT_THROW_ESCALATION.equals(stencilId)) {
            convertJsonToEscalationDefinition(elementNode, throwEvent);
        } else if (STENCIL_EVENT_THROW_COMPENSATION.equals(stencilId)) {
            convertJsonToCompensationDefinition(elementNode, throwEvent);
        }
        return throwEvent;
    }
}
