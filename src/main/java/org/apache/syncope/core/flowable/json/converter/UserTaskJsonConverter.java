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

import static org.apache.syncope.core.flowable.json.converter.JsonConverterUtil.getProperty;
import static org.apache.syncope.core.flowable.json.converter.JsonConverterUtil.getPropertyValueAsString;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.UserTask;
import org.springframework.util.CollectionUtils;

/**
 * @author Tijs Rademakers
 */
public class UserTaskJsonConverter extends BaseBpmnJsonConverter {

    public static void fillTypes(
            final Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap,
            final Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {

        fillJsonTypes(convertersToBpmnMap);
        fillBpmnTypes(convertersToJsonMap);
    }

    public static void fillJsonTypes(Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap) {
        convertersToBpmnMap.put(STENCIL_TASK_USER, UserTaskJsonConverter.class);
    }

    public static void fillBpmnTypes(
            Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {
        convertersToJsonMap.put(UserTask.class, UserTaskJsonConverter.class);
    }

    @Override
    protected String getStencilId(BaseElement baseElement) {
        return STENCIL_TASK_USER;
    }

    @Override
    protected void convertElementToJson(
            final ObjectNode propertiesNode,
            final BaseElement baseElement,
            final BpmnJsonConverterContext converterContext) {

        UserTask userTask = (UserTask) baseElement;
        String assignee = userTask.getAssignee();

        if (StringUtils.isNotEmpty(assignee)
                || !CollectionUtils.isEmpty(userTask.getCandidateUsers())
                || !CollectionUtils.isEmpty(userTask.getCandidateGroups())) {

            ObjectNode assignmentNode = JSON_MAPPER.createObjectNode();
            ObjectNode assignmentValuesNode = JSON_MAPPER.createObjectNode();

            List<ExtensionElement> idmAssigneeList = userTask.getExtensionElements().get("activiti-idm-assignee");
            List<ExtensionElement> idmAssigneeFieldList =
                    userTask.getExtensionElements().get("activiti-idm-assignee-field");
            if (!CollectionUtils.isEmpty(idmAssigneeList) || !CollectionUtils.isEmpty(idmAssigneeFieldList)
                    || !CollectionUtils.isEmpty(userTask.getExtensionElements().get("activiti-idm-candidate-user"))
                    || !CollectionUtils.isEmpty(userTask.getExtensionElements().get("activiti-idm-candidate-group"))) {

                assignmentValuesNode.put("type", "idm");
                ObjectNode idmNode = JSON_MAPPER.createObjectNode();
                assignmentValuesNode.set("idm", idmNode);

                List<ExtensionElement> canCompleteList = userTask.getExtensionElements().get("initiator-can-complete");
                if (!CollectionUtils.isEmpty(canCompleteList)) {
                    assignmentValuesNode.put(
                            "initiatorCanCompleteTask",
                            Boolean.valueOf(canCompleteList.get(0).getElementText()));
                }

                if (StringUtils.isNotEmpty(userTask.getAssignee())) {
                    ObjectNode assigneeNode = JSON_MAPPER.createObjectNode();
                    assigneeNode.put("id", userTask.getAssignee());
                    idmNode.set("assignee", assigneeNode);
                    idmNode.put("type", "user");

                    fillProperty("email", "assignee-info-email", assigneeNode, userTask);
                    fillProperty("firstName", "assignee-info-firstname", assigneeNode, userTask);
                    fillProperty("lastName", "assignee-info-lastname", assigneeNode, userTask);
                }

                List<ExtensionElement> idmCandidateUserList =
                        userTask.getExtensionElements().get("activiti-idm-candidate-user");
                if (!CollectionUtils.isEmpty(userTask.getCandidateUsers())
                        && !CollectionUtils.isEmpty(idmCandidateUserList)) {

                    if (!userTask.getCandidateUsers().isEmpty()) {
                        ArrayNode candidateUsersNode = JSON_MAPPER.createArrayNode();
                        idmNode.set("candidateUsers", candidateUsersNode);
                        idmNode.put("type", "users");
                        for (String candidateUser : userTask.getCandidateUsers()) {
                            ObjectNode candidateUserNode = JSON_MAPPER.createObjectNode();
                            candidateUserNode.put("id", candidateUser);
                            candidateUsersNode.add(candidateUserNode);

                            fillProperty(
                                    "email", "user-info-email-" + candidateUser, candidateUserNode, userTask);
                            fillProperty(
                                    "firstName", "user-info-firstname-" + candidateUser, candidateUserNode, userTask);
                            fillProperty(
                                    "lastName", "user-info-lastname-" + candidateUser, candidateUserNode, userTask);
                        }
                    }
                }

                List<ExtensionElement> idmCandidateGroupList =
                        userTask.getExtensionElements().get("activiti-idm-candidate-group");
                if (!CollectionUtils.isEmpty(userTask.getCandidateGroups())
                        && !CollectionUtils.isEmpty(idmCandidateGroupList)) {

                    if (!userTask.getCandidateGroups().isEmpty()) {
                        ArrayNode candidateGroupsNode = JSON_MAPPER.createArrayNode();
                        idmNode.set("candidateGroups", candidateGroupsNode);
                        idmNode.put("type", "groups");
                        for (String candidateGroup : userTask.getCandidateGroups()) {
                            ObjectNode candidateGroupNode = JSON_MAPPER.createObjectNode();
                            candidateGroupNode.put("id", candidateGroup);
                            candidateGroupsNode.add(candidateGroupNode);

                            fillProperty("name", "group-info-name-" + candidateGroup, candidateGroupNode, userTask);
                        }
                    }
                }

            } else {
                assignmentValuesNode.put("type", "static");

                if (StringUtils.isNotEmpty(assignee)) {
                    assignmentValuesNode.put(PROPERTY_USERTASK_ASSIGNEE, assignee);
                }

                if (!CollectionUtils.isEmpty(userTask.getCandidateUsers())) {
                    ArrayNode candidateArrayNode = JSON_MAPPER.createArrayNode();
                    for (String candidateUser : userTask.getCandidateUsers()) {
                        ObjectNode candidateNode = JSON_MAPPER.createObjectNode();
                        candidateNode.put("value", candidateUser);
                        candidateArrayNode.add(candidateNode);
                    }
                    assignmentValuesNode.set(PROPERTY_USERTASK_CANDIDATE_USERS, candidateArrayNode);
                }

                if (!CollectionUtils.isEmpty(userTask.getCandidateGroups())) {
                    ArrayNode candidateArrayNode = JSON_MAPPER.createArrayNode();
                    for (String candidateGroup : userTask.getCandidateGroups()) {
                        ObjectNode candidateNode = JSON_MAPPER.createObjectNode();
                        candidateNode.put("value", candidateGroup);
                        candidateArrayNode.add(candidateNode);
                    }
                    assignmentValuesNode.set(PROPERTY_USERTASK_CANDIDATE_GROUPS, candidateArrayNode);
                }
            }

            assignmentNode.set("assignment", assignmentValuesNode);
            propertiesNode.set(PROPERTY_USERTASK_ASSIGNMENT, assignmentNode);
        }

        if (userTask.getPriority() != null) {
            setPropertyValue(PROPERTY_USERTASK_PRIORITY, userTask.getPriority(), propertiesNode);
        }

        setPropertyValue(PROPERTY_SKIP_EXPRESSION, userTask.getSkipExpression(), propertiesNode);

        if (StringUtils.isNotEmpty(userTask.getFormKey())) {
            Map<String, String> modelInfo = converterContext.getFormModelInfoForFormModelKey(userTask.getFormKey());
            if (modelInfo != null) {
                ObjectNode formRefNode = JSON_MAPPER.createObjectNode();
                formRefNode.put("id", modelInfo.get("id"));
                formRefNode.put("name", modelInfo.get("name"));
                formRefNode.put("key", modelInfo.get("key"));
                propertiesNode.set(PROPERTY_FORM_REFERENCE, formRefNode);

            } else {
                setPropertyValue(PROPERTY_FORMKEY, userTask.getFormKey(), propertiesNode);
            }
        }

        setPropertyValue(PROPERTY_FORM_FIELD_VALIDATION, userTask.getValidateFormFields(), propertiesNode);
        setPropertyValue(PROPERTY_USERTASK_DUEDATE, userTask.getDueDate(), propertiesNode);
        setPropertyValue(PROPERTY_CALENDAR_NAME, userTask.getBusinessCalendarName(), propertiesNode);
        setPropertyValue(PROPERTY_USERTASK_CATEGORY, userTask.getCategory(), propertiesNode);
        setPropertyValue(PROPERTY_USERTASK_TASK_ID_VARIABLE_NAME, userTask.getTaskIdVariableName(), propertiesNode);

        addFormProperties(userTask.getFormProperties(), propertiesNode);
    }

    protected int getExtensionElementValueAsInt(final String name, final UserTask userTask) {
        int intValue = 0;
        String value = getExtensionElementValue(name, userTask);
        if (NumberUtils.isCreatable(value)) {
            intValue = Integer.parseInt(value);
        }
        return intValue;
    }

    protected String getExtensionElementValue(final String name, final UserTask userTask) {
        String value = "";
        if (!CollectionUtils.isEmpty(userTask.getExtensionElements().get(name))) {
            ExtensionElement extensionElement = userTask.getExtensionElements().get(name).get(0);
            value = extensionElement.getElementText();
        }
        return value;
    }

    @Override
    protected FlowElement convertJsonToElement(
            final JsonNode elementNode,
            final JsonNode modelNode,
            final Map<String, JsonNode> shapeMap,
            final BpmnJsonConverterContext converterContext) {

        UserTask task = new UserTask();

        task.setPriority(getPropertyValueAsString(PROPERTY_USERTASK_PRIORITY, elementNode));
        String formKey = getPropertyValueAsString(PROPERTY_FORMKEY, elementNode);
        if (StringUtils.isNotEmpty(formKey)) {
            task.setFormKey(formKey);
        } else {
            JsonNode formReferenceNode = getProperty(PROPERTY_FORM_REFERENCE, elementNode);
            if (formReferenceNode != null && formReferenceNode.get("id") != null) {

                String formModelId = formReferenceNode.get("id").asText();
                String formModelKey = converterContext.getFormModelKeyForFormModelId(formModelId);
                if (formModelKey != null) {
                    task.setFormKey(formModelKey);
                } else {
                    String key = formReferenceNode.get("key").asText();
                    if (StringUtils.isNotEmpty(key)) {
                        task.setFormKey(key);
                    }
                }
            }
        }

        task.setValidateFormFields(getPropertyValueAsString(PROPERTY_FORM_FIELD_VALIDATION, elementNode));
        task.setDueDate(getPropertyValueAsString(PROPERTY_USERTASK_DUEDATE, elementNode));
        task.setBusinessCalendarName(getPropertyValueAsString(PROPERTY_CALENDAR_NAME, elementNode));
        task.setCategory(getPropertyValueAsString(PROPERTY_USERTASK_CATEGORY, elementNode));
        task.setTaskIdVariableName(getPropertyValueAsString(PROPERTY_USERTASK_TASK_ID_VARIABLE_NAME, elementNode));

        JsonNode assignmentNode = getProperty(PROPERTY_USERTASK_ASSIGNMENT, elementNode);
        if (assignmentNode != null) {
            JsonNode assignmentDefNode = assignmentNode.get("assignment");
            if (assignmentDefNode != null) {
                JsonNode typeNode = assignmentDefNode.get("type");
                JsonNode canCompleteTaskNode = assignmentDefNode.get("initiatorCanCompleteTask");
                if (typeNode == null || "static".equalsIgnoreCase(typeNode.asText())) {
                    JsonNode assigneeNode = assignmentDefNode.get(PROPERTY_USERTASK_ASSIGNEE);
                    if (assigneeNode != null && !assigneeNode.isNull()) {
                        task.setAssignee(assigneeNode.asText());
                    }

                    task.setCandidateUsers(getValueAsList(PROPERTY_USERTASK_CANDIDATE_USERS, assignmentDefNode));
                    task.setCandidateGroups(getValueAsList(PROPERTY_USERTASK_CANDIDATE_GROUPS, assignmentDefNode));

                    if (!"$INITIATOR".equalsIgnoreCase(task.getAssignee())) {
                        if (canCompleteTaskNode != null && !canCompleteTaskNode.isNull()) {
                            addInitiatorCanCompleteExtensionElement(
                                    Boolean.parseBoolean(canCompleteTaskNode.asText()), task);
                        } else {
                            addInitiatorCanCompleteExtensionElement(false, task);
                        }

                    } else if ("$INITIATOR".equalsIgnoreCase(task.getAssignee())) {
                        addInitiatorCanCompleteExtensionElement(true, task);
                    }

                } else if ("idm".equalsIgnoreCase(typeNode.asText())) {
                    JsonNode idmDefNode = assignmentDefNode.get("idm");
                    if (idmDefNode != null && idmDefNode.has("type")) {
                        JsonNode idmTypeNode = idmDefNode.get("type");
                        if (idmTypeNode != null && "user".equalsIgnoreCase(idmTypeNode.asText())
                                && (idmDefNode.has("assignee") || idmDefNode.has("assigneeField"))) {

                            fillAssigneeInfo(idmDefNode, canCompleteTaskNode, task);
                        } else if (idmTypeNode != null
                                && "users".equalsIgnoreCase(idmTypeNode.asText())
                                && (idmDefNode.has("candidateUsers") || idmDefNode.has("candidateUserFields"))) {

                            fillCandidateUsers(idmDefNode, canCompleteTaskNode, task);
                        } else if (idmTypeNode != null && "groups".equalsIgnoreCase(idmTypeNode.asText())
                                && (idmDefNode.has("candidateGroups") || idmDefNode.has("candidateGroupFields"))) {

                            fillCandidateGroups(idmDefNode, canCompleteTaskNode, task);
                        } else {
                            task.setAssignee("$INITIATOR");
                            addExtensionElement("activiti-idm-initiator", String.valueOf(true), task);
                        }
                    }
                }
            }
        }

        task.setSkipExpression(getPropertyValueAsString(PROPERTY_SKIP_EXPRESSION, elementNode));

        convertJsonToFormProperties(elementNode, task);
        return task;
    }

    protected void fillAssigneeInfo(
            final JsonNode idmDefNode,
            final JsonNode canCompleteTaskNode,
            final UserTask task) {

        JsonNode assigneeNode = idmDefNode.get("assignee");
        if (assigneeNode != null && !assigneeNode.isNull()) {
            JsonNode idNode = assigneeNode.get("id");
            JsonNode emailNode = assigneeNode.get("email");
            if (idNode != null && !idNode.isNull() && StringUtils.isNotEmpty(idNode.asText())) {
                task.setAssignee(idNode.asText());
                addExtensionElement("activiti-idm-assignee", String.valueOf(true), task);
                addExtensionElement("assignee-info-email", emailNode, task);
                addExtensionElement("assignee-info-firstname", assigneeNode.get("firstName"), task);
                addExtensionElement("assignee-info-lastname", assigneeNode.get("lastName"), task);

            } else if (emailNode != null && !emailNode.isNull() && StringUtils.isNotEmpty(emailNode.asText())) {
                task.setAssignee(emailNode.asText());
            }
        }

        if (canCompleteTaskNode != null && !canCompleteTaskNode.isNull()) {
            addInitiatorCanCompleteExtensionElement(Boolean.parseBoolean(canCompleteTaskNode.asText()), task);
        } else {
            addInitiatorCanCompleteExtensionElement(false, task);
        }
    }

    protected void fillCandidateUsers(
            final JsonNode idmDefNode,
            final JsonNode canCompleteTaskNode,
            final UserTask task) {

        List<String> candidateUsers = new ArrayList<>();
        JsonNode candidateUsersNode = idmDefNode.get("candidateUsers");
        if (candidateUsersNode != null && candidateUsersNode.isArray()) {
            List<String> emails = new ArrayList<>();
            for (JsonNode userNode : candidateUsersNode) {
                if (userNode != null && !userNode.isNull()) {
                    JsonNode idNode = userNode.get("id");
                    JsonNode emailNode = userNode.get("email");
                    if (idNode != null && !idNode.isNull() && StringUtils.isNotEmpty(idNode.asText())) {
                        String id = idNode.asText();
                        candidateUsers.add(id);

                        addExtensionElement("user-info-email-" + id, emailNode, task);
                        addExtensionElement("user-info-firstname-" + id, userNode.get("firstName"), task);
                        addExtensionElement("user-info-lastname-" + id, userNode.get("lastName"), task);
                    } else if (emailNode != null && !emailNode.isNull() && StringUtils.isNotEmpty(emailNode.asText())) {
                        String email = emailNode.asText();
                        candidateUsers.add(email);
                        emails.add(email);
                    }
                }
            }

            if (!emails.isEmpty()) {
                // Email extension element
                addExtensionElement("activiti-candidate-users-emails", String.join(",", emails), task);
            }

            if (!candidateUsers.isEmpty()) {
                addExtensionElement("activiti-idm-candidate-user", String.valueOf(true), task);
                if (canCompleteTaskNode != null && !canCompleteTaskNode.isNull()) {
                    addInitiatorCanCompleteExtensionElement(Boolean.parseBoolean(canCompleteTaskNode.asText()), task);
                } else {
                    addInitiatorCanCompleteExtensionElement(false, task);
                }
            }
        }

        JsonNode candidateUserFieldsNode = idmDefNode.get("candidateUserFields");
        if (candidateUserFieldsNode != null && candidateUserFieldsNode.isArray()) {
            for (JsonNode fieldNode : candidateUserFieldsNode) {
                JsonNode idNode = fieldNode.get("id");
                if (idNode != null && !idNode.isNull() && StringUtils.isNotEmpty(idNode.asText())) {
                    String id = idNode.asText();
                    candidateUsers.add("field(" + id + ")");

                    addExtensionElement("user-field-info-name-" + id, fieldNode.get("name"), task);
                }
            }
        }

        if (!candidateUsers.isEmpty()) {
            task.setCandidateUsers(candidateUsers);
        }
    }

    protected void fillCandidateGroups(
            final JsonNode idmDefNode,
            final JsonNode canCompleteTaskNode,
            final UserTask task) {

        List<String> candidateGroups = new ArrayList<>();
        JsonNode candidateGroupsNode = idmDefNode.get("candidateGroups");
        if (candidateGroupsNode != null && candidateGroupsNode.isArray()) {
            for (JsonNode groupNode : candidateGroupsNode) {
                if (groupNode != null && !groupNode.isNull()) {
                    JsonNode idNode = groupNode.get("id");
                    JsonNode nameNode = groupNode.get("name");
                    if (idNode != null && !idNode.isNull() && StringUtils.isNotEmpty(idNode.asText())) {
                        String id = idNode.asText();
                        candidateGroups.add(id);

                        addExtensionElement("group-info-name-" + id, nameNode, task);
                    }
                }
            }
        }

        JsonNode candidateGroupFieldsNode = idmDefNode.get("candidateGroupFields");
        if (candidateGroupFieldsNode != null && candidateGroupFieldsNode.isArray()) {
            for (JsonNode fieldNode : candidateGroupFieldsNode) {
                JsonNode idNode = fieldNode.get("id");
                if (idNode != null && !idNode.isNull() && StringUtils.isNotEmpty(idNode.asText())) {
                    String id = idNode.asText();
                    candidateGroups.add("field(" + id + ")");

                    addExtensionElement("group-field-info-name-" + id, fieldNode.get("name"), task);
                }
            }
        }

        if (!candidateGroups.isEmpty()) {
            task.setCandidateGroups(candidateGroups);

            addExtensionElement("activiti-idm-candidate-group", String.valueOf(true), task);
            if (canCompleteTaskNode != null && !canCompleteTaskNode.isNull()) {
                addInitiatorCanCompleteExtensionElement(Boolean.parseBoolean(canCompleteTaskNode.asText()), task);
            } else {
                addInitiatorCanCompleteExtensionElement(false, task);
            }
        }
    }

    protected void addInitiatorCanCompleteExtensionElement(final boolean canCompleteTask, final UserTask task) {
        addExtensionElement("initiator-can-complete", String.valueOf(canCompleteTask), task);
    }

    protected void addExtensionElement(final String name, final JsonNode elementNode, final UserTask task) {
        if (elementNode != null && !elementNode.isNull() && StringUtils.isNotEmpty(elementNode.asText())) {
            addExtensionElement(name, elementNode.asText(), task);
        }
    }

    protected void addExtensionElement(final String name, final String elementText, final UserTask task) {
        ExtensionElement extensionElement = new ExtensionElement();
        extensionElement.setNamespace(BpmnJsonConverter.MODELER_NAMESPACE);
        extensionElement.setNamespacePrefix("modeler");
        extensionElement.setName(name);
        extensionElement.setElementText(elementText);
        task.addExtensionElement(extensionElement);
    }

    protected void fillProperty(
            final String propertyName,
            final String extensionElementName,
            final ObjectNode elementNode,
            final UserTask task) {

        List<ExtensionElement> extensionElementList = task.getExtensionElements().get(extensionElementName);
        if (!CollectionUtils.isEmpty(extensionElementList)) {
            elementNode.put(propertyName, extensionElementList.get(0).getElementText());
        }
    }
}
