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

import org.flowable.editor.language.json.converter.util.JsonConverterUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.flowable.editor.constants.EditorJsonConstants;
import org.flowable.editor.constants.StencilConstants;
import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BpmnDiEdge;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Escalation;
import org.flowable.bpmn.model.Event;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowElementsContainer;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.Gateway;
import org.flowable.bpmn.model.GraphicInfo;
import org.flowable.bpmn.model.Lane;
import org.flowable.bpmn.model.Message;
import org.flowable.bpmn.model.MessageEventDefinition;
import org.flowable.bpmn.model.MessageFlow;
import org.flowable.bpmn.model.Pool;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.Signal;
import org.flowable.bpmn.model.SignalEventDefinition;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.ValuedDataObject;
import org.flowable.editor.language.json.converter.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 */
public class BpmnJsonConverter implements EditorJsonConstants, StencilConstants, ActivityProcessor {

    private static class FlowWithContainer {

        private SequenceFlow sequenceFlow;

        private FlowElementsContainer flowContainer;

        FlowWithContainer(final SequenceFlow sequenceFlow, final FlowElementsContainer flowContainer) {
            this.sequenceFlow = sequenceFlow;
            this.flowContainer = flowContainer;
        }

        public SequenceFlow getSequenceFlow() {
            return sequenceFlow;
        }

        public void setSequenceFlow(final SequenceFlow sequenceFlow) {
            this.sequenceFlow = sequenceFlow;
        }

        public FlowElementsContainer getFlowContainer() {
            return flowContainer;
        }

        public void setFlowContainer(final FlowElementsContainer flowContainer) {
            this.flowContainer = flowContainer;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(BpmnJsonConverter.class);

    public static final String MODELER_NAMESPACE = "http://flowable.org/modeler";

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().findAndAddModules().build();

    protected static final Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> JSON_CONVERTERS =
            new HashMap<>();

    protected static final Map<String, Class<? extends BaseBpmnJsonConverter>> BPMN_CONVERTERS = new HashMap<>();

    static {
        // start and end events
        StartEventJsonConverter.fillTypes(BPMN_CONVERTERS, JSON_CONVERTERS);
        EndEventJsonConverter.fillTypes(BPMN_CONVERTERS, JSON_CONVERTERS);

        // connectors
        SequenceFlowJsonConverter.fillTypes(BPMN_CONVERTERS, JSON_CONVERTERS);
        MessageFlowJsonConverter.fillTypes(BPMN_CONVERTERS, JSON_CONVERTERS);
        AssociationJsonConverter.fillTypes(BPMN_CONVERTERS, JSON_CONVERTERS);

        // task types
        BusinessRuleTaskJsonConverter.fillTypes(BPMN_CONVERTERS, JSON_CONVERTERS);
        MailTaskJsonConverter.fillTypes(BPMN_CONVERTERS, JSON_CONVERTERS);
        ManualTaskJsonConverter.fillTypes(BPMN_CONVERTERS, JSON_CONVERTERS);
        ReceiveTaskJsonConverter.fillTypes(BPMN_CONVERTERS, JSON_CONVERTERS);
        ScriptTaskJsonConverter.fillTypes(BPMN_CONVERTERS, JSON_CONVERTERS);
        ServiceTaskJsonConverter.fillTypes(BPMN_CONVERTERS, JSON_CONVERTERS);
        ShellTaskJsonConverter.fillTypes(BPMN_CONVERTERS, JSON_CONVERTERS);
        UserTaskJsonConverter.fillTypes(BPMN_CONVERTERS, JSON_CONVERTERS);
        CallActivityJsonConverter.fillTypes(BPMN_CONVERTERS, JSON_CONVERTERS);
        CamelTaskJsonConverter.fillTypes(BPMN_CONVERTERS, JSON_CONVERTERS);
        MuleTaskJsonConverter.fillTypes(BPMN_CONVERTERS, JSON_CONVERTERS);
        HttpTaskJsonConverter.fillTypes(BPMN_CONVERTERS, JSON_CONVERTERS);
        SendTaskJsonConverter.fillTypes(BPMN_CONVERTERS, JSON_CONVERTERS);
        DecisionTaskJsonConverter.fillTypes(BPMN_CONVERTERS, JSON_CONVERTERS);
        SendEventTaskJsonConverter.fillTypes(BPMN_CONVERTERS, JSON_CONVERTERS);
        ExternalWorkerServiceTaskJsonConverter.fillTypes(BPMN_CONVERTERS, JSON_CONVERTERS);

        // gateways
        ExclusiveGatewayJsonConverter.fillTypes(BPMN_CONVERTERS, JSON_CONVERTERS);
        InclusiveGatewayJsonConverter.fillTypes(BPMN_CONVERTERS, JSON_CONVERTERS);
        ParallelGatewayJsonConverter.fillTypes(BPMN_CONVERTERS, JSON_CONVERTERS);
        EventGatewayJsonConverter.fillTypes(BPMN_CONVERTERS, JSON_CONVERTERS);

        // scope constructs
        SubProcessJsonConverter.fillTypes(BPMN_CONVERTERS, JSON_CONVERTERS);
        EventSubProcessJsonConverter.fillTypes(BPMN_CONVERTERS, JSON_CONVERTERS);
        AdhocSubProcessJsonConverter.fillTypes(BPMN_CONVERTERS, JSON_CONVERTERS);

        // catch events
        CatchEventJsonConverter.fillTypes(BPMN_CONVERTERS, JSON_CONVERTERS);

        // throw events
        ThrowEventJsonConverter.fillTypes(BPMN_CONVERTERS, JSON_CONVERTERS);

        // boundary events
        BoundaryEventJsonConverter.fillTypes(BPMN_CONVERTERS, JSON_CONVERTERS);

        // artifacts
        TextAnnotationJsonConverter.fillTypes(BPMN_CONVERTERS, JSON_CONVERTERS);
        DataStoreJsonConverter.fillTypes(BPMN_CONVERTERS, JSON_CONVERTERS);
    }

    private static final List<String> DI_CIRCLES = new ArrayList<>();

    private static final List<String> DI_RECTANGLES = new ArrayList<>();

    private static final List<String> DI_GATEWAY = new ArrayList<>();

    private static final double LINE_WIDTH = 0.05d;

    static {
        DI_CIRCLES.add(STENCIL_EVENT_START_CONDITIONAL);
        DI_CIRCLES.add(STENCIL_EVENT_START_ERROR);
        DI_CIRCLES.add(STENCIL_EVENT_START_ESCALATION);
        DI_CIRCLES.add(STENCIL_EVENT_START_MESSAGE);
        DI_CIRCLES.add(STENCIL_EVENT_START_NONE);
        DI_CIRCLES.add(STENCIL_EVENT_START_TIMER);
        DI_CIRCLES.add(STENCIL_EVENT_START_SIGNAL);
        DI_CIRCLES.add(STENCIL_EVENT_START_EVENT_REGISTRY);
        DI_CIRCLES.add(STENCIL_EVENT_START_VARIABLE_LISTENER);

        DI_CIRCLES.add(STENCIL_EVENT_BOUNDARY_CONDITIONAL);
        DI_CIRCLES.add(STENCIL_EVENT_BOUNDARY_ERROR);
        DI_CIRCLES.add(STENCIL_EVENT_BOUNDARY_ESCALATION);
        DI_CIRCLES.add(STENCIL_EVENT_BOUNDARY_SIGNAL);
        DI_CIRCLES.add(STENCIL_EVENT_BOUNDARY_TIMER);
        DI_CIRCLES.add(STENCIL_EVENT_BOUNDARY_MESSAGE);
        DI_CIRCLES.add(STENCIL_EVENT_BOUNDARY_EVENT_REGISTRY);
        DI_CIRCLES.add(STENCIL_EVENT_BOUNDARY_VARIABLE_LISTENER);
        DI_CIRCLES.add(STENCIL_EVENT_BOUNDARY_CANCEL);
        DI_CIRCLES.add(STENCIL_EVENT_BOUNDARY_COMPENSATION);

        DI_CIRCLES.add(STENCIL_EVENT_CATCH_CONDITIONAL);
        DI_CIRCLES.add(STENCIL_EVENT_CATCH_MESSAGE);
        DI_CIRCLES.add(STENCIL_EVENT_CATCH_SIGNAL);
        DI_CIRCLES.add(STENCIL_EVENT_CATCH_TIMER);
        DI_CIRCLES.add(STENCIL_EVENT_CATCH_EVENT_REGISTRY);
        DI_CIRCLES.add(STENCIL_EVENT_CATCH_VARIABLE_LISTENER);

        DI_CIRCLES.add(STENCIL_EVENT_THROW_NONE);
        DI_CIRCLES.add(STENCIL_EVENT_THROW_SIGNAL);
        DI_CIRCLES.add(STENCIL_EVENT_THROW_ESCALATION);
        DI_CIRCLES.add(STENCIL_EVENT_THROW_COMPENSATION);

        DI_CIRCLES.add(STENCIL_EVENT_END_NONE);
        DI_CIRCLES.add(STENCIL_EVENT_END_ERROR);
        DI_CIRCLES.add(STENCIL_EVENT_END_ESCALATION);
        DI_CIRCLES.add(STENCIL_EVENT_END_CANCEL);
        DI_CIRCLES.add(STENCIL_EVENT_END_TERMINATE);

        DI_RECTANGLES.add(STENCIL_CALL_ACTIVITY);
        DI_RECTANGLES.add(STENCIL_SUB_PROCESS);
        DI_RECTANGLES.add(STENCIL_COLLAPSED_SUB_PROCESS);
        DI_RECTANGLES.add(STENCIL_EVENT_SUB_PROCESS);
        DI_RECTANGLES.add(STENCIL_ADHOC_SUB_PROCESS);
        DI_RECTANGLES.add(STENCIL_TASK_BUSINESS_RULE);
        DI_RECTANGLES.add(STENCIL_TASK_MAIL);
        DI_RECTANGLES.add(STENCIL_TASK_MANUAL);
        DI_RECTANGLES.add(STENCIL_TASK_RECEIVE);
        DI_RECTANGLES.add(STENCIL_TASK_RECEIVE_EVENT);
        DI_RECTANGLES.add(STENCIL_TASK_SCRIPT);
        DI_RECTANGLES.add(STENCIL_TASK_SEND);
        DI_RECTANGLES.add(STENCIL_TASK_SEND_EVENT);
        DI_RECTANGLES.add(STENCIL_TASK_SERVICE);
        DI_RECTANGLES.add(STENCIL_TASK_USER);
        DI_RECTANGLES.add(STENCIL_TASK_CAMEL);
        DI_RECTANGLES.add(STENCIL_TASK_MULE);
        DI_RECTANGLES.add(STENCIL_TASK_HTTP);
        DI_RECTANGLES.add(STENCIL_TASK_DECISION);
        DI_RECTANGLES.add(STENCIL_TASK_SEND_EVENT);
        DI_RECTANGLES.add(STENCIL_TASK_EXTERNAL_WORKER);
        DI_RECTANGLES.add(STENCIL_TASK_SHELL);
        DI_RECTANGLES.add(STENCIL_TEXT_ANNOTATION);

        DI_GATEWAY.add(STENCIL_GATEWAY_EVENT);
        DI_GATEWAY.add(STENCIL_GATEWAY_EXCLUSIVE);
        DI_GATEWAY.add(STENCIL_GATEWAY_INCLUSIVE);
        DI_GATEWAY.add(STENCIL_GATEWAY_PARALLEL);
    }

    private static void fillSubShapes(final Map<String, SubProcess> subShapesMap, final SubProcess subProcess) {
        for (FlowElement flowElement : subProcess.getFlowElements()) {
            if (flowElement instanceof SubProcess childSubProcess) {
                subShapesMap.put(childSubProcess.getId(), subProcess);
                fillSubShapes(subShapesMap, childSubProcess);
            } else {
                subShapesMap.put(flowElement.getId(), subProcess);
            }
        }
    }

    private static void postProcessElements(
            final FlowElementsContainer parentContainer,
            final Collection<FlowElement> flowElementList,
            final Map<String, JsonNode> edgeMap,
            final BpmnModel bpmnModel,
            final Map<String, FlowWithContainer> allFlowMap,
            final List<Gateway> gatewayWithOrderList) {

        for (FlowElement flowElement : flowElementList) {
            parentContainer.addFlowElementToMap(flowElement);

            if (flowElement instanceof Event event) {
                if (CollectionUtils.isNotEmpty(event.getEventDefinitions())) {
                    EventDefinition eventDef = event.getEventDefinitions().get(0);
                    if (eventDef instanceof SignalEventDefinition signalEventDef) {
                        if (StringUtils.isNotEmpty(signalEventDef.getSignalRef())) {
                            if (bpmnModel.getSignal(signalEventDef.getSignalRef()) == null) {
                                bpmnModel.addSignal(
                                        new Signal(signalEventDef.getSignalRef(), signalEventDef.getSignalRef()));
                            }
                        }
                    } else if (eventDef instanceof MessageEventDefinition messageEventDef) {
                        if (StringUtils.isNotEmpty(messageEventDef.getMessageRef())) {
                            if (bpmnModel.getMessage(messageEventDef.getMessageRef()) == null) {
                                bpmnModel.addMessage(new Message(
                                        messageEventDef.getMessageRef(), messageEventDef.getMessageRef(), null));
                            }
                        }
                    }
                }
            }

            if (flowElement instanceof BoundaryEvent boundaryEvent) {
                Activity activity = retrieveAttachedRefObject(
                        boundaryEvent.getAttachedToRefId(), parentContainer.getFlowElements());

                if (activity == null) {
                    LOG.warn("Boundary event {} is not attached to any activity", boundaryEvent.getId());
                } else {
                    boundaryEvent.setAttachedToRef(activity);
                    activity.getBoundaryEvents().add(boundaryEvent);
                }
            } else if (flowElement instanceof Gateway gateway) {
                if (flowElement.getExtensionElements().containsKey("EDITOR_FLOW_ORDER")) {
                    gatewayWithOrderList.add(gateway);
                }
            } else if (flowElement instanceof SubProcess subProcess) {
                postProcessElements(subProcess, subProcess.getFlowElements(), edgeMap, bpmnModel, allFlowMap,
                        gatewayWithOrderList);

            } else if (flowElement instanceof SequenceFlow sequenceFlow) {
                FlowElement sourceFlowElement = parentContainer.getFlowElement(sequenceFlow.getSourceRef());
                if (sourceFlowElement instanceof FlowNode flowNode) {
                    FlowWithContainer flowWithContainer = new FlowWithContainer(sequenceFlow, parentContainer);
                    if (sequenceFlow.getExtensionElements().get("EDITOR_RESOURCEID") != null
                            && !sequenceFlow.getExtensionElements().get("EDITOR_RESOURCEID").isEmpty()) {

                        allFlowMap.put(
                                sequenceFlow.getExtensionElements().get("EDITOR_RESOURCEID").get(0).
                                        getElementText(), flowWithContainer);
                        sequenceFlow.getExtensionElements().remove("EDITOR_RESOURCEID");
                    }

                    flowNode.getOutgoingFlows().add(sequenceFlow);
                    JsonNode edgeNode = edgeMap.get(sequenceFlow.getId());
                    if (edgeNode != null) {
                        boolean isDefault =
                                JsonConverterUtil.getPropertyValueAsBoolean(PROPERTY_SEQUENCEFLOW_DEFAULT, edgeNode);
                        if (isDefault) {
                            if (sourceFlowElement instanceof Activity activity) {
                                activity.setDefaultFlow(sequenceFlow.getId());
                            } else if (sourceFlowElement instanceof Gateway gateway) {
                                gateway.setDefaultFlow(sequenceFlow.getId());
                            }
                        }
                    }
                }
                FlowElement targetFlowElement = parentContainer.getFlowElement(sequenceFlow.getTargetRef());
                if (targetFlowElement instanceof FlowNode flowNode) {
                    flowNode.getIncomingFlows().add(sequenceFlow);
                }
            }
        }
    }

    private static Activity retrieveAttachedRefObject(
            final String attachedToRefId,
            final Collection<FlowElement> flowElementList) {

        Activity activity = null;
        if (StringUtils.isNotEmpty(attachedToRefId)) {
            for (FlowElement flowElement : flowElementList) {
                if (attachedToRefId.equals(flowElement.getId())) {
                    activity = (Activity) flowElement;
                    break;
                } else if (flowElement instanceof SubProcess subProcess) {
                    Activity retrievedActivity =
                            retrieveAttachedRefObject(attachedToRefId, subProcess.getFlowElements());
                    if (retrievedActivity != null) {
                        activity = retrievedActivity;
                        break;
                    }
                }
            }
        }
        return activity;
    }

    private static void readShapeDI(
            final JsonNode objectNode,
            final double parentX,
            final double parentY,
            final Map<String, JsonNode> shapeMap,
            final Map<String, JsonNode> sourceRefMap,
            final BpmnModel bpmnModel) {

        if (objectNode.get(EDITOR_CHILD_SHAPES) == null) {
            return;
        }

        for (JsonNode jsonChildNode : objectNode.get(EDITOR_CHILD_SHAPES)) {
            String stencilId = BpmnJsonConverterUtil.getStencilId(jsonChildNode);
            if (!STENCIL_SEQUENCE_FLOW.equals(stencilId) && !STENCIL_ASSOCIATION.equals(stencilId)) {
                GraphicInfo graphicInfo = new GraphicInfo();

                JsonNode boundsNode = jsonChildNode.get(EDITOR_BOUNDS);
                ObjectNode upperLeftNode = (ObjectNode) boundsNode.get(EDITOR_BOUNDS_UPPER_LEFT);
                ObjectNode lowerRightNode = (ObjectNode) boundsNode.get(EDITOR_BOUNDS_LOWER_RIGHT);

                graphicInfo.setX(upperLeftNode.get(EDITOR_BOUNDS_X).asDouble() + parentX);
                graphicInfo.setY(upperLeftNode.get(EDITOR_BOUNDS_Y).asDouble() + parentY);
                graphicInfo.setWidth(lowerRightNode.get(EDITOR_BOUNDS_X).asDouble() - graphicInfo.getX() + parentX);
                graphicInfo.setHeight(lowerRightNode.get(EDITOR_BOUNDS_Y).asDouble() - graphicInfo.getY() + parentY);

                String childShapeId = jsonChildNode.get(EDITOR_SHAPE_ID).asText();
                bpmnModel.addGraphicInfo(BpmnJsonConverterUtil.getElementId(jsonChildNode), graphicInfo);

                shapeMap.put(childShapeId, jsonChildNode);

                ArrayNode outgoingNode = (ArrayNode) jsonChildNode.get("outgoing");
                if (outgoingNode != null && outgoingNode.size() > 0) {
                    for (JsonNode outgoingChildNode : outgoingNode) {
                        JsonNode resourceNode = outgoingChildNode.get(EDITOR_SHAPE_ID);
                        if (resourceNode != null) {
                            sourceRefMap.put(resourceNode.asText(), jsonChildNode);
                        }
                    }
                }

                //The graphic info of the collapsed subprocess is relative to its parent.
                //But the children of the collapsed subprocess are relative to the canvas upper corner. (always 0,0)
                if (STENCIL_COLLAPSED_SUB_PROCESS.equals(stencilId)) {
                    readShapeDI(
                            jsonChildNode, 0, 0, shapeMap, sourceRefMap, bpmnModel);
                } else {
                    readShapeDI(
                            jsonChildNode, graphicInfo.getX(), graphicInfo.getY(), shapeMap, sourceRefMap, bpmnModel);
                }
            }
        }
    }

    private static void filterAllEdges(
            final JsonNode objectNode,
            final Map<String, JsonNode> edgeMap,
            final Map<String, List<JsonNode>> sourceAndTargetMap,
            final Map<String, JsonNode> shapeMap,
            final Map<String, JsonNode> sourceRefMap) {

        if (objectNode.get(EDITOR_CHILD_SHAPES) == null) {
            return;
        }

        for (JsonNode jsonChildNode : objectNode.get(EDITOR_CHILD_SHAPES)) {
            ObjectNode childNode = (ObjectNode) jsonChildNode;
            String stencilId = BpmnJsonConverterUtil.getStencilId(childNode);
            if (STENCIL_SUB_PROCESS.equals(stencilId)
                    || STENCIL_POOL.equals(stencilId)
                    || STENCIL_LANE.equals(stencilId)
                    || STENCIL_COLLAPSED_SUB_PROCESS.equals(stencilId)
                    || STENCIL_EVENT_SUB_PROCESS.equals(stencilId)) {

                filterAllEdges(childNode, edgeMap, sourceAndTargetMap, shapeMap, sourceRefMap);
            } else if (STENCIL_SEQUENCE_FLOW.equals(stencilId) || STENCIL_ASSOCIATION.equals(stencilId)) {
                String childEdgeId = BpmnJsonConverterUtil.getElementId(childNode);
                JsonNode targetNode = childNode.get("target");
                if (targetNode != null && !targetNode.isNull()) {
                    String targetRefId = targetNode.get(EDITOR_SHAPE_ID).asText();
                    List<JsonNode> sourceAndTargetList = new ArrayList<>();
                    sourceAndTargetList.add(sourceRefMap.get(childNode.get(EDITOR_SHAPE_ID).asText()));
                    sourceAndTargetList.add(shapeMap.get(targetRefId));
                    sourceAndTargetMap.put(childEdgeId, sourceAndTargetList);
                }
                edgeMap.put(childEdgeId, childNode);
            }
        }
    }

    private static void readEdgeDI(
            final Map<String, JsonNode> edgeMap,
            final Map<String, List<JsonNode>> sourceAndTargetMap,
            final BpmnModel bpmnModel) {

        for (String edgeId : edgeMap.keySet()) {
            JsonNode edgeNode = edgeMap.get(edgeId);
            List<JsonNode> sourceAndTargetList = sourceAndTargetMap.get(edgeId);

            JsonNode sourceRefNode = null;
            JsonNode targetRefNode = null;

            if (sourceAndTargetList != null && sourceAndTargetList.size() > 1) {
                sourceRefNode = sourceAndTargetList.get(0);
                targetRefNode = sourceAndTargetList.get(1);
            }

            if (sourceRefNode == null) {
                LOG.info("Skipping edge {} because source ref is null", edgeId);
                continue;
            }

            if (targetRefNode == null) {
                LOG.info("Skipping edge {} because target ref is null", edgeId);
                continue;
            }

            JsonNode dockersNode = edgeNode.get(EDITOR_DOCKERS);
            double sourceDockersX = dockersNode.get(0).get(EDITOR_BOUNDS_X).asDouble();
            double sourceDockersY = dockersNode.get(0).get(EDITOR_BOUNDS_Y).asDouble();

            GraphicInfo sourceInfo = bpmnModel.getGraphicInfo(BpmnJsonConverterUtil.getElementId(sourceRefNode));
            GraphicInfo targetInfo = bpmnModel.getGraphicInfo(BpmnJsonConverterUtil.getElementId(targetRefNode));

            double sourceRefLineX = sourceInfo.getX() + sourceDockersX;
            double sourceRefLineY = sourceInfo.getY() + sourceDockersY;

            double nextPointInLineX = dockersNode.get(1).get(EDITOR_BOUNDS_X).asDouble();
            double nextPointInLineY = dockersNode.get(1).get(EDITOR_BOUNDS_Y).asDouble();

            if (dockersNode.size() == 2) {
                nextPointInLineX += targetInfo.getX();
                nextPointInLineY += targetInfo.getY();
            }

            java.awt.geom.Line2D firstLine = new java.awt.geom.Line2D.Double(
                    sourceRefLineX, sourceRefLineY, nextPointInLineX, nextPointInLineY);

            String sourceRefStencilId = BpmnJsonConverterUtil.getStencilId(sourceRefNode);
            String targetRefStencilId = BpmnJsonConverterUtil.getStencilId(targetRefNode);

            List<GraphicInfo> graphicInfoList = new ArrayList<>();

            Area source2D = null;
            if (DI_CIRCLES.contains(sourceRefStencilId)) {
                source2D = createEllipse(sourceInfo, sourceDockersX, sourceDockersY);
            } else if (DI_RECTANGLES.contains(sourceRefStencilId)) {
                source2D = createRectangle(sourceInfo);
            } else if (DI_GATEWAY.contains(sourceRefStencilId)) {
                source2D = createGateway(sourceInfo);
            }

            if (source2D != null) {
                Collection<java.awt.geom.Point2D> intersections = getIntersections(firstLine, source2D);
                if (intersections != null && !intersections.isEmpty()) {
                    java.awt.geom.Point2D intersection = intersections.iterator().next();
                    graphicInfoList.add(createGraphicInfo(intersection.getX(), intersection.getY()));
                } else {
                    graphicInfoList.add(createGraphicInfo(sourceRefLineX, sourceRefLineY));
                }
            }

            java.awt.geom.Line2D lastLine;

            if (dockersNode.size() > 2) {
                for (int i = 1; i < dockersNode.size() - 1; i++) {
                    double x = dockersNode.get(i).get(EDITOR_BOUNDS_X).asDouble();
                    double y = dockersNode.get(i).get(EDITOR_BOUNDS_Y).asDouble();
                    graphicInfoList.add(createGraphicInfo(x, y));
                }

                double startLastLineX = dockersNode.get(dockersNode.size() - 2).get(EDITOR_BOUNDS_X).asDouble();
                double startLastLineY = dockersNode.get(dockersNode.size() - 2).get(EDITOR_BOUNDS_Y).asDouble();

                double endLastLineX = dockersNode.get(dockersNode.size() - 1).get(EDITOR_BOUNDS_X).asDouble();
                double endLastLineY = dockersNode.get(dockersNode.size() - 1).get(EDITOR_BOUNDS_Y).asDouble();

                endLastLineX += targetInfo.getX();
                endLastLineY += targetInfo.getY();

                lastLine = new java.awt.geom.Line2D.Double(startLastLineX, startLastLineY, endLastLineX, endLastLineY);
            } else {
                lastLine = firstLine;
            }

            BpmnDiEdge edgeInfo = new BpmnDiEdge();
            edgeInfo.setWaypoints(graphicInfoList);
            GraphicInfo sourceDockerInfo = new GraphicInfo();
            sourceDockerInfo.setX(dockersNode.get(0).get(EDITOR_BOUNDS_X).asDouble());
            sourceDockerInfo.setY(dockersNode.get(0).get(EDITOR_BOUNDS_Y).asDouble());
            edgeInfo.setSourceDockerInfo(sourceDockerInfo);

            GraphicInfo targetDockerInfo = new GraphicInfo();
            targetDockerInfo.setX(dockersNode.get(dockersNode.size() - 1).get(EDITOR_BOUNDS_X).asDouble());
            targetDockerInfo.setY(dockersNode.get(dockersNode.size() - 1).get(EDITOR_BOUNDS_Y).asDouble());
            edgeInfo.setTargetDockerInfo(targetDockerInfo);

            bpmnModel.addEdgeInfo(edgeId, edgeInfo);

            Area target2D = null;
            if (DI_RECTANGLES.contains(targetRefStencilId)) {
                target2D = createRectangle(targetInfo);
            } else if (DI_CIRCLES.contains(targetRefStencilId)) {
                double targetDockersX = dockersNode.get(dockersNode.size() - 1).get(EDITOR_BOUNDS_X).asDouble();
                double targetDockersY = dockersNode.get(dockersNode.size() - 1).get(EDITOR_BOUNDS_Y).asDouble();

                target2D = createEllipse(targetInfo, targetDockersX, targetDockersY);
            } else if (DI_GATEWAY.contains(targetRefStencilId)) {
                target2D = createGateway(targetInfo);
            }

            if (target2D != null) {
                Collection<java.awt.geom.Point2D> intersections = getIntersections(lastLine, target2D);
                if (intersections != null && !intersections.isEmpty()) {
                    java.awt.geom.Point2D intersection = intersections.iterator().next();
                    graphicInfoList.add(createGraphicInfo(intersection.getX(), intersection.getY()));
                } else {
                    graphicInfoList.add(createGraphicInfo(lastLine.getX2(), lastLine.getY2()));
                }
            }

            bpmnModel.addFlowGraphicInfoList(edgeId, graphicInfoList);
        }
    }

    private static Area createEllipse(
            final GraphicInfo sourceInfo,
            final double halfWidth,
            final double halfHeight) {

        Area outerCircle = new Area(new Ellipse2D.Double(
                sourceInfo.getX(), sourceInfo.getY(), 2 * halfWidth, 2 * halfHeight));
        Area innerCircle = new Area(new Ellipse2D.Double(
                sourceInfo.getX() + LINE_WIDTH, sourceInfo.getY() + LINE_WIDTH,
                2 * (halfWidth - LINE_WIDTH), 2 * (halfHeight - LINE_WIDTH)));
        outerCircle.subtract(innerCircle);
        return outerCircle;
    }

    private static Collection<java.awt.geom.Point2D> getIntersections(
            final java.awt.geom.Line2D line, final Area shape) {

        Area intersectionArea = new Area(getLineShape(line));
        intersectionArea.intersect(shape);
        if (!intersectionArea.isEmpty()) {
            Rectangle2D bounds2D = intersectionArea.getBounds2D();
            HashSet<java.awt.geom.Point2D> intersections = new HashSet<>(1);
            intersections.add(new java.awt.geom.Point2D.Double(bounds2D.getX(), bounds2D.getY()));
            return intersections;
        }
        return Collections.emptySet();
    }

    private static Shape getLineShape(final java.awt.geom.Line2D line2D) {
        Path2D line = new Path2D.Double(Path2D.WIND_NON_ZERO, 4);
        line.moveTo(line2D.getX1(), line2D.getY1());
        line.lineTo(line2D.getX2(), line2D.getY2());
        line.lineTo(line2D.getX2() + LINE_WIDTH, line2D.getY2() + LINE_WIDTH);
        line.closePath();
        return line;
    }

    private static Area createRectangle(final GraphicInfo graphicInfo) {
        Area outerRectangle = new Area(new Rectangle2D.Double(
                graphicInfo.getX(), graphicInfo.getY(),
                graphicInfo.getWidth(), graphicInfo.getHeight()));
        Area innerRectangle = new Area(new Rectangle2D.Double(
                graphicInfo.getX() + LINE_WIDTH, graphicInfo.getY() + LINE_WIDTH,
                graphicInfo.getWidth() - 2 * LINE_WIDTH, graphicInfo.getHeight() - 2 * LINE_WIDTH));
        outerRectangle.subtract(innerRectangle);
        return outerRectangle;
    }

    private static Area createGateway(final GraphicInfo graphicInfo) {
        Area outerGatewayArea = new Area(
                createGatewayShape(graphicInfo.getX(), graphicInfo.getY(), graphicInfo.getWidth(), graphicInfo.
                        getHeight()));
        Area innerGatewayArea = new Area(
                createGatewayShape(graphicInfo.getX() + LINE_WIDTH, graphicInfo.getY() + LINE_WIDTH,
                        graphicInfo.getWidth() - 2 * LINE_WIDTH, graphicInfo.getHeight() - 2 * LINE_WIDTH));
        outerGatewayArea.subtract(innerGatewayArea);
        return outerGatewayArea;
    }

    private static Path2D.Double createGatewayShape(
            final double x, final double y, final double width, final double height) {

        double middleX = x + (width / 2);
        double middleY = y + (height / 2);

        Path2D.Double gatewayShape = new Path2D.Double(Path2D.WIND_NON_ZERO, 4);
        gatewayShape.moveTo(x, middleY);
        gatewayShape.lineTo(middleX, y);
        gatewayShape.lineTo(x + width, middleY);
        gatewayShape.lineTo(middleX, y + height);
        gatewayShape.closePath();
        return gatewayShape;
    }

    private static GraphicInfo createGraphicInfo(final double x, final double y) {
        GraphicInfo graphicInfo = new GraphicInfo();
        graphicInfo.setX(x);
        graphicInfo.setY(y);
        return graphicInfo;
    }

    public ObjectNode convertToJson(final BpmnModel model) {
        return convertToJson(model, new StandaloneBpmnConverterContext());
    }

    public ObjectNode convertToJson(final BpmnModel model, final BpmnJsonConverterContext converterContext) {
        ObjectNode modelNode = JSON_MAPPER.createObjectNode();
        double maxX = 0.0;
        double maxY = 0.0;
        for (GraphicInfo flowInfo : model.getLocationMap().values()) {
            if ((flowInfo.getX() + flowInfo.getWidth()) > maxX) {
                maxX = flowInfo.getX() + flowInfo.getWidth();
            }

            if ((flowInfo.getY() + flowInfo.getHeight()) > maxY) {
                maxY = flowInfo.getY() + flowInfo.getHeight();
            }
        }
        maxX += 50;
        maxY += 50;

        if (maxX < 1485) {
            maxX = 1485;
        }

        if (maxY < 700) {
            maxY = 700;
        }

        modelNode.set("bounds", BpmnJsonConverterUtil.createBoundsNode(maxX, maxY, 0, 0));
        modelNode.put("resourceId", "canvas");

        ObjectNode stencilNode = JSON_MAPPER.createObjectNode();
        stencilNode.put("id", "BPMNDiagram");
        modelNode.set("stencil", stencilNode);

        ObjectNode stencilsetNode = JSON_MAPPER.createObjectNode();
        stencilsetNode.put("namespace", "http://b3mn.org/stencilset/bpmn2.0#");
        stencilsetNode.put("url", "../editor/stencilsets/bpmn2.0/bpmn2.0.json");
        modelNode.set("stencilset", stencilsetNode);

        ArrayNode shapesArrayNode = JSON_MAPPER.createArrayNode();

        Process mainProcess;
        if (!model.getPools().isEmpty()) {
            mainProcess = model.getProcess(model.getPools().get(0).getId());
        } else {
            mainProcess = model.getMainProcess();
        }

        ObjectNode propertiesNode = JSON_MAPPER.createObjectNode();
        if (StringUtils.isNotEmpty(mainProcess.getId())) {
            propertiesNode.put(PROPERTY_PROCESS_ID, mainProcess.getId());
        }
        if (StringUtils.isNotEmpty(mainProcess.getName())) {
            propertiesNode.put(PROPERTY_NAME, mainProcess.getName());
        }
        if (StringUtils.isNotEmpty(mainProcess.getDocumentation())) {
            propertiesNode.put(PROPERTY_DOCUMENTATION, mainProcess.getDocumentation());
        }
        if (!mainProcess.isExecutable()) {
            propertiesNode.put(PROPERTY_IS_EXECUTABLE, "false");
        }
        if (StringUtils.isNoneEmpty(model.getTargetNamespace())) {
            propertiesNode.put(PROPERTY_PROCESS_NAMESPACE, model.getTargetNamespace());
        }
        if (CollectionUtils.isNotEmpty(mainProcess.getCandidateStarterGroups())) {
            propertiesNode.put(
                    PROPERTY_PROCESS_POTENTIALSTARTERGROUP,
                    String.join(",", mainProcess.getCandidateStarterGroups()));
        }
        if (CollectionUtils.isNotEmpty(mainProcess.getCandidateStarterUsers())) {
            propertiesNode.put(
                    PROPERTY_PROCESS_POTENTIALSTARTERUSER,
                    String.join(",", mainProcess.getCandidateStarterUsers()));
        }

        if (mainProcess.getExtensionElements().containsKey("historyLevel")) {
            List<ExtensionElement> historyExtensionElements = mainProcess.getExtensionElements().get("historyLevel");
            if (historyExtensionElements != null && !historyExtensionElements.isEmpty()) {
                String historyLevel = historyExtensionElements.get(0).getElementText();
                if (StringUtils.isNotEmpty(historyLevel)) {
                    propertiesNode.put(PROPERTY_PROCESS_HISTORYLEVEL, historyLevel);
                }
            }
        }

        propertiesNode.put(
                PROPERTY_IS_EAGER_EXECUTION_FETCHING,
                Boolean.valueOf(mainProcess.isEnableEagerExecutionTreeFetching()));

        BpmnJsonConverterUtil.convertMessagesToJson(model.getMessages(), propertiesNode);

        BpmnJsonConverterUtil.convertListenersToJson(mainProcess.getExecutionListeners(), true, propertiesNode);
        BpmnJsonConverterUtil.convertEventListenersToJson(mainProcess.getEventListeners(), propertiesNode);
        BpmnJsonConverterUtil.convertSignalDefinitionsToJson(model, propertiesNode);
        BpmnJsonConverterUtil.convertMessagesToJson(model, propertiesNode);
        BpmnJsonConverterUtil.convertEscalationDefinitionsToJson(model, propertiesNode);

        if (CollectionUtils.isNotEmpty(mainProcess.getDataObjects())) {
            BpmnJsonConverterUtil.convertDataPropertiesToJson(mainProcess.getDataObjects(), propertiesNode);
        }

        modelNode.set(EDITOR_SHAPE_PROPERTIES, propertiesNode);

        boolean poolHasDI = false;
        if (!model.getPools().isEmpty()) {
            for (Pool pool : model.getPools()) {
                GraphicInfo graphicInfo = model.getGraphicInfo(pool.getId());
                if (graphicInfo != null) {
                    poolHasDI = true;
                    break;
                }
            }
        }

        if (!model.getPools().isEmpty() && poolHasDI) {
            for (Pool pool : model.getPools()) {
                GraphicInfo poolGraphicInfo = model.getGraphicInfo(pool.getId());
                if (poolGraphicInfo == null) {
                    continue;
                }
                ObjectNode poolNode = BpmnJsonConverterUtil.createChildShape(pool.getId(), STENCIL_POOL,
                        poolGraphicInfo.getX() + poolGraphicInfo.getWidth(),
                        poolGraphicInfo.getY() + poolGraphicInfo.getHeight(), poolGraphicInfo.getX(), poolGraphicInfo.
                        getY());
                shapesArrayNode.add(poolNode);
                ObjectNode poolPropertiesNode = JSON_MAPPER.createObjectNode();
                poolPropertiesNode.put(PROPERTY_OVERRIDE_ID, pool.getId());
                poolPropertiesNode.put(PROPERTY_PROCESS_ID, pool.getProcessRef());
                if (!pool.isExecutable()) {
                    poolPropertiesNode.put(PROPERTY_IS_EXECUTABLE, "false");
                }
                if (StringUtils.isNotEmpty(pool.getName())) {
                    poolPropertiesNode.put(PROPERTY_NAME, pool.getName());
                }
                poolNode.set(EDITOR_SHAPE_PROPERTIES, poolPropertiesNode);

                ArrayNode laneShapesArrayNode = JSON_MAPPER.createArrayNode();
                poolNode.set(EDITOR_CHILD_SHAPES, laneShapesArrayNode);

                ArrayNode outgoingArrayNode = JSON_MAPPER.createArrayNode();
                poolNode.set("outgoing", outgoingArrayNode);

                Process process = model.getProcess(pool.getId());
                if (process != null) {
                    Map<String, ArrayNode> laneMap = new HashMap<>();
                    for (Lane lane : process.getLanes()) {
                        GraphicInfo laneGraphicInfo = model.getGraphicInfo(lane.getId());
                        if (laneGraphicInfo == null) {
                            continue;
                        }
                        ObjectNode laneNode = BpmnJsonConverterUtil.createChildShape(lane.getId(), STENCIL_LANE,
                                laneGraphicInfo.getX() + laneGraphicInfo.getWidth() - poolGraphicInfo.getX(),
                                laneGraphicInfo.getY() + laneGraphicInfo.getHeight() - poolGraphicInfo.getY(),
                                laneGraphicInfo.getX() - poolGraphicInfo.getX(), laneGraphicInfo.getY()
                                - poolGraphicInfo.getY());
                        laneShapesArrayNode.add(laneNode);
                        ObjectNode lanePropertiesNode = JSON_MAPPER.createObjectNode();
                        lanePropertiesNode.put(PROPERTY_OVERRIDE_ID, lane.getId());
                        if (StringUtils.isNotEmpty(lane.getName())) {
                            lanePropertiesNode.put(PROPERTY_NAME, lane.getName());
                        }
                        laneNode.set(EDITOR_SHAPE_PROPERTIES, lanePropertiesNode);

                        ArrayNode elementShapesArrayNode = JSON_MAPPER.createArrayNode();
                        laneNode.set(EDITOR_CHILD_SHAPES, elementShapesArrayNode);
                        laneNode.set("outgoing", JSON_MAPPER.createArrayNode());

                        laneMap.put(lane.getId(), elementShapesArrayNode);
                    }

                    for (FlowElement flowElement : process.getFlowElements()) {
                        Lane laneForElement = null;
                        GraphicInfo laneGraphicInfo = null;

                        FlowElement lookForElement;
                        if (flowElement instanceof SequenceFlow sequenceFlow) {
                            lookForElement = model.getFlowElement(sequenceFlow.getSourceRef());
                        } else {
                            lookForElement = flowElement;
                        }

                        for (Lane lane : process.getLanes()) {
                            if (lane.getFlowReferences().contains(lookForElement.getId())) {
                                laneGraphicInfo = model.getGraphicInfo(lane.getId());
                                if (laneGraphicInfo != null) {
                                    laneForElement = lane;
                                }
                                break;
                            }
                        }

                        if (flowElement instanceof SequenceFlow || laneForElement != null) {
                            processFlowElement(
                                    flowElement, process, model, laneMap.get(laneForElement.getId()),
                                    converterContext, laneGraphicInfo.getX(), laneGraphicInfo.getY());
                        }
                    }

                    processArtifacts(converterContext, process, model, shapesArrayNode, 0.0, 0.0);
                }

                for (MessageFlow messageFlow : model.getMessageFlows().values()) {
                    if (messageFlow.getSourceRef().equals(pool.getId())) {
                        outgoingArrayNode.add(BpmnJsonConverterUtil.createResourceNode(messageFlow.getId()));
                    }
                }
            }

        } else {
            processFlowElements(model.getMainProcess(), model, shapesArrayNode, converterContext, 0.0, 0.0);
        }
        processMessageFlows(model, shapesArrayNode, converterContext);

        modelNode.set(EDITOR_CHILD_SHAPES, shapesArrayNode);
        return modelNode;
    }

    @Override
    public void processFlowElements(
            final FlowElementsContainer container,
            final BpmnModel model,
            final ArrayNode shapesArrayNode,
            final BpmnJsonConverterContext converterContext,
            final double subProcessX,
            final double subProcessY) {

        for (FlowElement flowElement : container.getFlowElements()) {
            processFlowElement(
                    flowElement, container, model, shapesArrayNode, converterContext, subProcessX, subProcessY);
        }

        processArtifacts(converterContext, container, model, shapesArrayNode, subProcessX, subProcessY);
    }

    private void processFlowElement(
            final FlowElement flowElement,
            final FlowElementsContainer container,
            final BpmnModel model,
            final ArrayNode shapesArrayNode,
            final BpmnJsonConverterContext converterContext,
            final double containerX,
            final double containerY) {

        Optional.ofNullable(JSON_CONVERTERS.get(flowElement.getClass())).ifPresent(converter -> {
            try {
                BaseBpmnJsonConverter converterInstance = converter.getDeclaredConstructor().newInstance();
                converterInstance.convertToJson(
                        converterContext, flowElement, this, model, container, shapesArrayNode,
                        containerX, containerY);
            } catch (Exception e) {
                LOG.error("Error converting {}", flowElement, e);
            }
        });
    }

    private void processArtifacts(
            final BpmnJsonConverterContext converterContext,
            final FlowElementsContainer container,
            final BpmnModel model,
            final ArrayNode shapesArrayNode,
            final double containerX,
            final double containerY) {

        container.getArtifacts().forEach(artifact -> Optional.ofNullable(JSON_CONVERTERS.get(artifact.getClass())).
                ifPresent(converter -> {
                    try {
                        converter.getDeclaredConstructor().newInstance().convertToJson(
                                converterContext,
                                artifact,
                                this,
                                model,
                                container,
                                shapesArrayNode,
                                containerX,
                                containerY);
                    } catch (Exception e) {
                        LOG.error("Error converting {}", artifact, e);
                    }
                }));
    }

    private void processMessageFlows(
            final BpmnModel model,
            final ArrayNode shapesArrayNode,
            final BpmnJsonConverterContext converterCOntext) {

        for (MessageFlow messageFlow : model.getMessageFlows().values()) {
            MessageFlowJsonConverter jsonConverter = new MessageFlowJsonConverter();
            jsonConverter.convertToJson(converterCOntext, messageFlow, this, model, null, shapesArrayNode, 0.0, 0.0);
        }
    }

    public BpmnModel convertToBpmnModel(final JsonNode modelNode) {
        return convertToBpmnModel(modelNode, new StandaloneBpmnConverterContext());
    }

    public BpmnModel convertToBpmnModel(final JsonNode modelNode, final BpmnJsonConverterContext converterContext) {
        BpmnModel bpmnModel = new BpmnModel();

        bpmnModel.setTargetNamespace("http://flowable.org/test");
        bpmnModel.setExporter("Flowable Open Source Modeler");
        bpmnModel.setExporterVersion(getClass().getPackage().getImplementationVersion());
        Map<String, JsonNode> shapeMap = new HashMap<>();
        Map<String, JsonNode> sourceRefMap = new HashMap<>();
        Map<String, JsonNode> edgeMap = new HashMap<>();
        Map<String, List<JsonNode>> sourceAndTargetMap = new HashMap<>();

        readShapeDI(modelNode, 0, 0, shapeMap, sourceRefMap, bpmnModel);
        filterAllEdges(modelNode, edgeMap, sourceAndTargetMap, shapeMap, sourceRefMap);
        readEdgeDI(edgeMap, sourceAndTargetMap, bpmnModel);

        ArrayNode shapesArrayNode = (ArrayNode) modelNode.get(EDITOR_CHILD_SHAPES);

        if (shapesArrayNode == null || shapesArrayNode.size() == 0) {
            return bpmnModel;
        }

        boolean nonEmptyPoolFound = false;
        Map<String, Lane> elementInLaneMap = new HashMap<>();
        // first create the pool structure
        for (JsonNode shapeNode : shapesArrayNode) {
            String stencilId = BpmnJsonConverterUtil.getStencilId(shapeNode);
            if (STENCIL_POOL.equals(stencilId)) {
                Pool pool = new Pool();
                pool.setId(BpmnJsonConverterUtil.getElementId(shapeNode));
                pool.setName(JsonConverterUtil.getPropertyValueAsString(PROPERTY_NAME, shapeNode));
                pool.setProcessRef(JsonConverterUtil.getPropertyValueAsString(PROPERTY_PROCESS_ID, shapeNode));
                pool.setExecutable(
                        JsonConverterUtil.getPropertyValueAsBoolean(PROPERTY_IS_EXECUTABLE, shapeNode, true));
                bpmnModel.getPools().add(pool);

                Process process = new Process();
                process.setId(pool.getProcessRef());
                process.setName(pool.getName());
                process.setExecutable(pool.isExecutable());
                process.setEnableEagerExecutionTreeFetching(JsonConverterUtil.getPropertyValueAsBoolean(
                        PROPERTY_IS_EAGER_EXECUTION_FETCHING, shapeNode, false));

                BpmnJsonConverterUtil.convertJsonToMessages(modelNode, bpmnModel);
                BpmnJsonConverterUtil.convertJsonToListeners(modelNode, process);
                JsonNode eventListenersNode = BpmnJsonConverterUtil.getProperty(PROPERTY_EVENT_LISTENERS, modelNode);
                if (eventListenersNode != null) {
                    eventListenersNode = BpmnJsonConverterUtil.validateIfNodeIsTextual(eventListenersNode);
                    BpmnJsonConverterUtil.parseEventListeners(
                            eventListenersNode.get(PROPERTY_EVENTLISTENER_VALUE), process);
                }

                JsonNode processDataPropertiesNode =
                        modelNode.get(EDITOR_SHAPE_PROPERTIES).get(PROPERTY_DATA_PROPERTIES);

                if (processDataPropertiesNode != null) {
                    List<ValuedDataObject> dataObjects =
                            BpmnJsonConverterUtil.convertJsonToDataProperties(processDataPropertiesNode, process);
                    process.setDataObjects(dataObjects);
                    process.getFlowElements().addAll(dataObjects);
                }

                String userStarterValue = BpmnJsonConverterUtil.getPropertyValueAsString(
                        PROPERTY_PROCESS_POTENTIALSTARTERUSER, modelNode);
                String groupStarterValue = BpmnJsonConverterUtil.getPropertyValueAsString(
                        PROPERTY_PROCESS_POTENTIALSTARTERGROUP, modelNode);

                if (StringUtils.isNotEmpty(userStarterValue)) {
                    String[] userStartArray = userStarterValue.split(",");

                    List<String> userStarters = new ArrayList<>(Arrays.asList(userStartArray));

                    process.setCandidateStarterUsers(userStarters);
                }

                if (StringUtils.isNotEmpty(groupStarterValue)) {
                    String[] groupStarterArray = groupStarterValue.split(",");

                    List<String> groupStarters = new ArrayList<>(Arrays.asList(groupStarterArray));

                    process.setCandidateStarterGroups(groupStarters);
                }

                bpmnModel.addProcess(process);

                ArrayNode laneArrayNode = (ArrayNode) shapeNode.get(EDITOR_CHILD_SHAPES);
                for (JsonNode laneNode : laneArrayNode) {
                    // should be a lane, but just check to be certain
                    String laneStencilId = BpmnJsonConverterUtil.getStencilId(laneNode);
                    if (STENCIL_LANE.equals(laneStencilId)) {
                        nonEmptyPoolFound = true;
                        Lane lane = new Lane();
                        lane.setId(BpmnJsonConverterUtil.getElementId(laneNode));
                        lane.setName(JsonConverterUtil.getPropertyValueAsString(PROPERTY_NAME, laneNode));
                        lane.setParentProcess(process);
                        process.getLanes().add(lane);

                        processJsonElements(laneNode.get(EDITOR_CHILD_SHAPES), modelNode, lane, shapeMap,
                                converterContext, bpmnModel);
                        if (CollectionUtils.isNotEmpty(lane.getFlowReferences())) {
                            for (String elementRef : lane.getFlowReferences()) {
                                elementInLaneMap.put(elementRef, lane);
                            }
                        }
                    }
                }
            }
        }

        // Signal Definitions exist on the root level
        JsonNode signalDefinitionNode = BpmnJsonConverterUtil.getProperty(PROPERTY_SIGNAL_DEFINITIONS, modelNode);
        // no idea why this needs to be done twice
        signalDefinitionNode = BpmnJsonConverterUtil.validateIfNodeIsTextual(signalDefinitionNode);
        signalDefinitionNode = BpmnJsonConverterUtil.validateIfNodeIsTextual(signalDefinitionNode);
        if (signalDefinitionNode != null) {
            if (signalDefinitionNode instanceof ArrayNode signalDefinitionArrayNode) {
                for (JsonNode signalDefinitionJsonNode : signalDefinitionArrayNode) {
                    String signalId = signalDefinitionJsonNode.get(PROPERTY_SIGNAL_DEFINITION_ID).asText();
                    String signalName = signalDefinitionJsonNode.get(PROPERTY_SIGNAL_DEFINITION_NAME).asText();
                    String signalScope = signalDefinitionJsonNode.get(PROPERTY_SIGNAL_DEFINITION_SCOPE).asText();

                    if (StringUtils.isNotEmpty(signalId) && StringUtils.isNotEmpty(signalName)) {
                        Signal signal = new Signal();
                        signal.setId(signalId);
                        signal.setName(signalName);
                        signal.setScope("processinstance".equals(signalScope.toLowerCase())
                                ? Signal.SCOPE_PROCESS_INSTANCE : Signal.SCOPE_GLOBAL);
                        bpmnModel.addSignal(signal);
                    }
                }
            }
        }

        // Escalation Definitions exist on the root level
        JsonNode escalationDefinitionNode = BpmnJsonConverterUtil.
                getProperty(PROPERTY_ESCALATION_DEFINITIONS, modelNode);
        // no idea why this needs to be done twice
        escalationDefinitionNode = BpmnJsonConverterUtil.validateIfNodeIsTextual(escalationDefinitionNode);
        escalationDefinitionNode = BpmnJsonConverterUtil.validateIfNodeIsTextual(escalationDefinitionNode);
        if (escalationDefinitionNode != null) {
            if (escalationDefinitionNode instanceof ArrayNode escalationDefinitionArrayNode) {
                for (JsonNode signalDefinitionJsonNode : escalationDefinitionArrayNode) {
                    String escalationId = signalDefinitionJsonNode.get(PROPERTY_ESCALATION_DEFINITION_ID).asText();
                    String escalationName = signalDefinitionJsonNode.get(PROPERTY_ESCALATION_DEFINITION_NAME).asText();

                    if (StringUtils.isNotEmpty(escalationId) && StringUtils.isNotEmpty(escalationName)) {
                        Escalation escalation = new Escalation();
                        escalation.setId(escalationId);
                        escalation.setEscalationCode(escalationId);
                        escalation.setName(escalationName);
                        bpmnModel.addEscalation(escalation);
                    }
                }
            }
        }

        if (!nonEmptyPoolFound) {
            Process process = new Process();
            bpmnModel.getProcesses().add(process);
            process.setId(BpmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_PROCESS_ID, modelNode));
            process.setName(BpmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_NAME, modelNode));
            String namespace = BpmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_PROCESS_NAMESPACE, modelNode);
            if (StringUtils.isNotEmpty(namespace)) {
                bpmnModel.setTargetNamespace(namespace);
            }
            process.setDocumentation(BpmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_DOCUMENTATION, modelNode));
            JsonNode processExecutableNode = JsonConverterUtil.getProperty(PROPERTY_IS_EXECUTABLE, modelNode);
            if (processExecutableNode != null && StringUtils.isNotEmpty(processExecutableNode.asText())) {
                process.setExecutable(JsonConverterUtil.getPropertyValueAsBoolean(PROPERTY_IS_EXECUTABLE, modelNode));
            }
            String historyLevel =
                    BpmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_PROCESS_HISTORYLEVEL, modelNode);
            if (StringUtils.isNotEmpty(historyLevel)) {
                ExtensionElement historyExtensionElement = new ExtensionElement();
                historyExtensionElement.setName("historyLevel");
                historyExtensionElement.setNamespace("http://flowable.org/bpmn");
                historyExtensionElement.setNamespacePrefix("flowable");
                historyExtensionElement.setElementText(historyLevel);
                process.addExtensionElement(historyExtensionElement);
            }

            BpmnJsonConverterUtil.convertJsonToMessages(modelNode, bpmnModel);

            BpmnJsonConverterUtil.convertJsonToListeners(modelNode, process);
            JsonNode eventListenersNode = BpmnJsonConverterUtil.getProperty(PROPERTY_EVENT_LISTENERS, modelNode);
            if (eventListenersNode != null) {
                eventListenersNode = BpmnJsonConverterUtil.validateIfNodeIsTextual(eventListenersNode);
                BpmnJsonConverterUtil.parseEventListeners(
                        eventListenersNode.get(PROPERTY_EVENTLISTENER_VALUE), process);
            }

            JsonNode processDataPropertiesNode = modelNode.get(EDITOR_SHAPE_PROPERTIES).get(PROPERTY_DATA_PROPERTIES);

            if (processDataPropertiesNode != null) {
                List<ValuedDataObject> dataObjects =
                        BpmnJsonConverterUtil.convertJsonToDataProperties(processDataPropertiesNode, process);
                process.setDataObjects(dataObjects);
                process.getFlowElements().addAll(dataObjects);
            }

            String userStarterValue = BpmnJsonConverterUtil.getPropertyValueAsString(
                    PROPERTY_PROCESS_POTENTIALSTARTERUSER, modelNode);
            String groupStarterValue = BpmnJsonConverterUtil.getPropertyValueAsString(
                    PROPERTY_PROCESS_POTENTIALSTARTERGROUP, modelNode);

            if (StringUtils.isNotEmpty(userStarterValue)) {
                String[] userStartArray = userStarterValue.split(",");

                List<String> userStarters = new ArrayList<>(Arrays.asList(userStartArray));

                process.setCandidateStarterUsers(userStarters);
            }

            if (StringUtils.isNotEmpty(groupStarterValue)) {
                String[] groupStarterArray = groupStarterValue.split(",");

                List<String> groupStarters = new ArrayList<>(Arrays.asList(groupStarterArray));

                process.setCandidateStarterGroups(groupStarters);
            }

            process.setEnableEagerExecutionTreeFetching(JsonConverterUtil.getPropertyValueAsBoolean(
                    PROPERTY_IS_EAGER_EXECUTION_FETCHING, modelNode, false));

            processJsonElements(shapesArrayNode, modelNode, process, shapeMap, converterContext, bpmnModel);

        } else {
            // sequence flows are on root level so need additional parsing for pools
            for (JsonNode shapeNode : shapesArrayNode) {
                if (STENCIL_SEQUENCE_FLOW.equalsIgnoreCase(BpmnJsonConverterUtil.getStencilId(shapeNode))
                        || STENCIL_ASSOCIATION.equalsIgnoreCase(BpmnJsonConverterUtil.getStencilId(shapeNode))) {

                    String sourceRef = BpmnJsonConverterUtil.lookForSourceRef(
                            shapeNode.get(EDITOR_SHAPE_ID).asText(), modelNode.get(EDITOR_CHILD_SHAPES));
                    if (sourceRef != null) {
                        Lane lane = elementInLaneMap.get(sourceRef);
                        SequenceFlowJsonConverter flowConverter = new SequenceFlowJsonConverter();
                        if (lane != null) {
                            flowConverter.convertToBpmnModel(
                                    shapeNode, modelNode, this, lane, shapeMap, bpmnModel, converterContext);
                        } else {
                            flowConverter.convertToBpmnModel(
                                    shapeNode, modelNode, this, bpmnModel.getProcesses().get(0), shapeMap, bpmnModel,
                                    converterContext);
                        }
                    }
                }
            }
        }

        // sequence flows are now all on root level
        Map<String, SubProcess> subShapesMap = new HashMap<>();
        for (Process process : bpmnModel.getProcesses()) {
            for (FlowElement flowElement : process.findFlowElementsOfType(SubProcess.class)) {
                SubProcess subProcess = (SubProcess) flowElement;
                fillSubShapes(subShapesMap, subProcess);
            }

            if (!subShapesMap.isEmpty()) {
                List<String> removeSubFlowsList = new ArrayList<>();
                for (FlowElement flowElement : process.findFlowElementsOfType(SequenceFlow.class)) {
                    SequenceFlow sequenceFlow = (SequenceFlow) flowElement;
                    if (subShapesMap.containsKey(sequenceFlow.getSourceRef())) {
                        SubProcess subProcess = subShapesMap.get(sequenceFlow.getSourceRef());
                        if (subProcess.getFlowElement(sequenceFlow.getId()) == null) {
                            subProcess.addFlowElement(sequenceFlow);
                            removeSubFlowsList.add(sequenceFlow.getId());
                        }
                    }
                }

                List<SubProcess> collapsedSubProcess = new ArrayList<>();
                for (SubProcess subProcess : subShapesMap.values()) {
                    // determine if its a collapsed subprocess
                    GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(subProcess.getId());
                    if (graphicInfo != null && Boolean.FALSE.equals(graphicInfo.getExpanded())) {
                        collapsedSubProcess.add(subProcess);
                    }
                }

                for (String flowId : removeSubFlowsList) {
                    process.removeFlowElement(flowId);

                    // check if the sequenceflow to remove is not assigned to a collapsed subprocess.
                    for (SubProcess subProcess : collapsedSubProcess) {
                        subProcess.removeFlowElement(flowId);
                    }
                }
            }
        }

        Map<String, FlowWithContainer> allFlowMap = new HashMap<>();
        List<Gateway> gatewayWithOrderList = new ArrayList<>();

        // post handling of process elements
        for (Process process : bpmnModel.getProcesses()) {
            postProcessElements(
                    process, process.getFlowElements(), edgeMap, bpmnModel, allFlowMap, gatewayWithOrderList);
        }

        // sort the sequence flows
        for (Gateway gateway : gatewayWithOrderList) {
            List<ExtensionElement> orderList = gateway.getExtensionElements().get("EDITOR_FLOW_ORDER");
            if (CollectionUtils.isNotEmpty(orderList)) {
                for (ExtensionElement orderElement : orderList) {
                    String flowValue = orderElement.getElementText();
                    if (StringUtils.isNotEmpty(flowValue)) {
                        if (allFlowMap.containsKey(flowValue)) {
                            FlowWithContainer flowWithContainer = allFlowMap.get(flowValue);
                            flowWithContainer.getFlowContainer().removeFlowElement(flowWithContainer.getSequenceFlow().
                                    getId());
                            flowWithContainer.getFlowContainer().addFlowElement(flowWithContainer.getSequenceFlow());
                        }
                    }
                }
            }
            gateway.getExtensionElements().remove("EDITOR_FLOW_ORDER");
        }

        return bpmnModel;
    }

    @Override
    public void processJsonElements(
            final JsonNode shapesArrayNode,
            final JsonNode modelNode,
            final BaseElement parentElement,
            final Map<String, JsonNode> shapeMap,
            final BpmnJsonConverterContext converterContext,
            final BpmnModel bpmnModel) {

        for (JsonNode shapeNode : shapesArrayNode) {
            String stencilId = BpmnJsonConverterUtil.getStencilId(shapeNode);
            Class<? extends BaseBpmnJsonConverter> converter = BPMN_CONVERTERS.get(stencilId);
            try {
                BaseBpmnJsonConverter converterInstance = converter.getDeclaredConstructor().newInstance();
                converterInstance.convertToBpmnModel(
                        shapeNode, modelNode, this, parentElement, shapeMap, bpmnModel, converterContext);
            } catch (Exception e) {
                LOG.error("Error converting {}", BpmnJsonConverterUtil.getStencilId(shapeNode), e);
            }
        }
    }
}
