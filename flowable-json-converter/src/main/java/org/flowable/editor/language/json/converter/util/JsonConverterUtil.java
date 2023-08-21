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
package org.flowable.editor.language.json.converter.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.flowable.editor.constants.EditorJsonConstants;
import org.flowable.editor.constants.StencilConstants;
import org.flowable.editor.language.json.converter.BpmnJsonConverterUtil;

public final class JsonConverterUtil implements EditorJsonConstants, StencilConstants {

    public static String getPropertyValueAsString(final String name, final JsonNode objectNode) {
        String propertyValue = null;
        JsonNode propertyNode = getProperty(name, objectNode);
        if (propertyNode != null && !"null".equalsIgnoreCase(propertyNode.asText())) {
            propertyValue = propertyNode.asText();
        }
        return propertyValue;
    }

    public static boolean getPropertyValueAsBoolean(final String name, final JsonNode objectNode) {
        return getPropertyValueAsBoolean(name, objectNode, false);
    }

    public static boolean getPropertyValueAsBoolean(
            final String name, final JsonNode objectNode, final boolean defaultValue) {

        boolean result = defaultValue;
        String stringValue = getPropertyValueAsString(name, objectNode);

        if (PROPERTY_VALUE_YES.equalsIgnoreCase(stringValue) || "true".equalsIgnoreCase(stringValue)) {
            result = true;
        } else if (PROPERTY_VALUE_NO.equalsIgnoreCase(stringValue) || "false".equalsIgnoreCase(stringValue)) {
            result = false;
        }

        return result;
    }

    public static List<String> getPropertyValueAsList(final String name, final JsonNode objectNode) {
        List<String> resultList = new ArrayList<>();
        JsonNode propertyNode = getProperty(name, objectNode);
        if (propertyNode != null && !"null".equalsIgnoreCase(propertyNode.asText())) {
            String propertyValue = propertyNode.asText();
            String[] valueList = propertyValue.split(",");
            for (String value : valueList) {
                resultList.add(value.trim());
            }
        }
        return resultList;
    }

    public static JsonNode getProperty(final String name, final JsonNode objectNode) {
        JsonNode propertyNode = null;
        if (objectNode.get(EDITOR_SHAPE_PROPERTIES) != null) {
            JsonNode propertiesNode = objectNode.get(EDITOR_SHAPE_PROPERTIES);
            propertyNode = propertiesNode.get(name);
        }
        return propertyNode;
    }

    /**
     * Usable for BPMN 2.0 editor json: traverses all child shapes (also nested), goes into the properties and sees if
     * there is a matching property in the 'properties' of the childshape and returns
     * those in a list.
     *
     * @param editorJsonNode
     * @param propertyName
     * @param allowedStencilTypes
     * @return a map with said json nodes, with the key the name of the childshape.
     */
    protected static List<JsonLookupResult> getBpmnProcessModelChildShapesPropertyValues(
            final JsonNode editorJsonNode,
            final String propertyName,
            final List<String> allowedStencilTypes) {

        List<JsonLookupResult> result = new ArrayList<>();
        internalGetBpmnProcessChildShapePropertyValues(editorJsonNode, propertyName, allowedStencilTypes, result);
        return result;
    }

    protected static void internalGetBpmnProcessChildShapePropertyValues(
            final JsonNode editorJsonNode,
            final String propertyName,
            final List<String> allowedStencilTypes,
            final List<JsonLookupResult> result) {

        JsonNode childShapesNode = editorJsonNode.get("childShapes");
        if (childShapesNode != null && childShapesNode.isArray()) {
            ArrayNode childShapesArrayNode = (ArrayNode) childShapesNode;
            for (JsonNode childShapeNode : childShapesArrayNode) {
                String childShapeNodeStencilId = BpmnJsonConverterUtil.getStencilId(childShapeNode);
                boolean readPropertiesNode = allowedStencilTypes.contains(childShapeNodeStencilId);

                if (readPropertiesNode) {
                    // Properties
                    JsonNode properties = childShapeNode.get("properties");
                    if (properties != null && properties.has(propertyName)) {
                        JsonNode nameNode = properties.get("name");
                        JsonNode propertyNode = properties.get(propertyName);
                        result.add(new JsonLookupResult(
                                BpmnJsonConverterUtil.getElementId(childShapeNode),
                                nameNode != null ? nameNode.asText() : null,
                                propertyNode));
                    }
                }

                // Potential nested child shapes
                if (childShapeNode.has("childShapes")) {
                    internalGetBpmnProcessChildShapePropertyValues(
                            childShapeNode, propertyName, allowedStencilTypes, result);
                }
            }
        }
    }

    public static List<JsonLookupResult> getBpmnProcessModelFormReferences(final JsonNode editorJsonNode) {
        List<String> allowedStencilTypes = new ArrayList<>();
        allowedStencilTypes.add(STENCIL_TASK_USER);
        allowedStencilTypes.add(STENCIL_EVENT_START_NONE);
        return getBpmnProcessModelChildShapesPropertyValues(editorJsonNode, "formreference", allowedStencilTypes);
    }

    public static List<JsonLookupResult> getBpmnProcessModelDecisionTableReferences(final JsonNode editorJsonNode) {
        List<String> allowedStencilTypes = new ArrayList<>();
        allowedStencilTypes.add(STENCIL_TASK_DECISION);
        return getBpmnProcessModelChildShapesPropertyValues(
                editorJsonNode, "decisiontaskdecisiontablereference", allowedStencilTypes);
    }

    public static List<JsonLookupResult> getBpmnProcessModelDecisionServiceReferences(final JsonNode editorJsonNode) {
        List<String> allowedStencilTypes = new ArrayList<>();
        allowedStencilTypes.add(STENCIL_TASK_DECISION);
        return getBpmnProcessModelChildShapesPropertyValues(
                editorJsonNode, "decisiontaskdecisionservicereference", allowedStencilTypes);
    }

    // APP MODEL
    public static List<JsonNode> getAppModelReferencedProcessModels(final JsonNode appModelJson) {
        List<JsonNode> result = new ArrayList<>();
        if (appModelJson.has("models")) {
            ArrayNode modelsArrayNode = (ArrayNode) appModelJson.get("models");
            Iterator<JsonNode> modelArrayIterator = modelsArrayNode.iterator();
            while (modelArrayIterator.hasNext()) {
                result.add(modelArrayIterator.next());
            }
        }
        return result;
    }

    public static Set<String> getAppModelReferencedModelIds(final JsonNode appModelJson) {
        if (appModelJson.has("models")) {
            return JsonConverterUtil.gatherStringPropertyFromJsonNodes(appModelJson.get("models"), "id");
        }
        return Collections.emptySet();
    }

    // GENERIC
    /**
     * Loops through a list of {@link JsonNode} instances, and stores the given property with given type in the returned
     * set
     * In Java 8, this probably could be done a lot cooler.
     *
     * @param jsonNodes
     * @param propertyName
     * @return long properties from JSON nodes
     */
    public static Set<Long> gatherLongPropertyFromJsonNodes(
            final Iterable<JsonNode> jsonNodes, final String propertyName) {

        Set<Long> result = new HashSet<>(); // Using a Set to filter out doubles
        for (JsonNode node : jsonNodes) {
            if (node.has(propertyName)) {
                long propertyValue = node.get(propertyName).asLong();
                if (propertyValue > 0) { // Just to be safe
                    result.add(propertyValue);
                }
            }
        }
        return result;
    }

    public static Set<String> gatherStringPropertyFromJsonNodes(
            final Iterable<JsonNode> jsonNodes, final String propertyName) {

        Set<String> result = new HashSet<>(); // Using a Set to filter out doubles
        for (JsonNode node : jsonNodes) {
            if (node.has(propertyName)) {
                String propertyValue = node.get(propertyName).asText();
                if (propertyValue != null) { // Just to be safe
                    result.add(propertyValue);
                }
            }
        }
        return result;
    }

    public static List<JsonNode> filterOutJsonNodes(final List<JsonLookupResult> lookupResults) {
        List<JsonNode> jsonNodes = new ArrayList<>(lookupResults.size());
        for (JsonLookupResult lookupResult : lookupResults) {
            jsonNodes.add(lookupResult.getJsonNode());
        }
        return jsonNodes;
    }

    // Helper classes
    public static class JsonLookupResult {

        private String id;

        private String name;

        private JsonNode jsonNode;

        public JsonLookupResult(final String id, final String name, final JsonNode jsonNode) {
            this(name, jsonNode);
            this.id = id;
        }

        public JsonLookupResult(final String name, final JsonNode jsonNode) {
            this.name = name;
            this.jsonNode = jsonNode;
        }

        public String getId() {
            return id;
        }

        public void setId(final String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public JsonNode getJsonNode() {
            return jsonNode;
        }

        public void setJsonNode(final JsonNode jsonNode) {
            this.jsonNode = jsonNode;
        }
    }

    private JsonConverterUtil() {
        // private constructor for static utility class
    }
}
