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

import static org.flowable.editor.language.json.converter.util.JsonConverterUtil.getPropertyValueAsString;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.TextAnnotation;

/**
 * @author Tijs Rademakers
 */
public class TextAnnotationJsonConverter extends BaseBpmnJsonConverter {

    public static void fillTypes(
            final Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap,
            final Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {

        fillJsonTypes(convertersToBpmnMap);
        fillBpmnTypes(convertersToJsonMap);
    }

    public static void fillJsonTypes(final Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap) {
        convertersToBpmnMap.put(STENCIL_TEXT_ANNOTATION, TextAnnotationJsonConverter.class);
    }

    public static void fillBpmnTypes(
            final Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {

        convertersToJsonMap.put(TextAnnotation.class, TextAnnotationJsonConverter.class);
    }

    @Override
    protected String getStencilId(final BaseElement baseElement) {
        return STENCIL_TEXT_ANNOTATION;
    }

    @Override
    protected void convertElementToJson(
            final ObjectNode propertiesNode,
            final BaseElement baseElement,
            final BpmnJsonConverterContext converterContext) {

        TextAnnotation annotation = (TextAnnotation) baseElement;
        if (StringUtils.isNotEmpty(annotation.getText())) {
            setPropertyValue("text", annotation.getText(), propertiesNode);
        }
    }

    @Override
    protected BaseElement convertJsonToElement(
            final JsonNode elementNode,
            final JsonNode modelNode,
            final Map<String, JsonNode> shapeMap,
            final BpmnJsonConverterContext converterContext) {

        TextAnnotation annotation = new TextAnnotation();
        String text = getPropertyValueAsString("text", elementNode);
        if (StringUtils.isNotEmpty(text)) {
            annotation.setText(text);
        }
        return annotation;
    }
}
