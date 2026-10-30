package com.bmv.dragonsofmugloar.application.commandment;

import static com.bmv.flow.foundry.application.core.GlobalCommandmentDefinitionRegistry.registerCommandmentDefinition;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.bmv.flow.foundry.application.core.AiAgentTool;
import com.bmv.flow.foundry.application.core.AiAgentToolDefinition;
import com.bmv.flow.foundry.application.core.CommandmentImpl;
import com.bmv.flow.foundry.application.core.CustomRunTimeAttributes;
import com.bmv.dragonsofmugloar.application.commandment.definition.BuildHiddenMessageAiToolDefinition;
import com.bmv.dragonsofmugloar.application.commandment.request.BuildHiddenMessageRequest;
import com.bmv.dragonsofmugloar.application.commandment.response.HiddenMessageResponse;
import com.bmv.flow.foundry.domain.model.auth.BmvServiceAuthorization;
import com.bmv.flow.foundry.util.BmvObjectUtils;

@JsonTypeName(BuildHiddenMessageAiTool.NAME)
public class BuildHiddenMessageAiTool
    extends
    CommandmentImpl<BmvServiceAuthorization, BuildHiddenMessageRequest, HiddenMessageResponse, BuildHiddenMessageAiTool>
    implements
    AiAgentTool<BuildHiddenMessageRequest, HiddenMessageResponse, BuildHiddenMessageAiTool> {

  public static final String NAME = "BUILD_HIDDEN_MESSAGE_AI_TOOL";
  public static final BuildHiddenMessageAiToolDefinition TYPE_DEFINITION = registerCommandmentDefinition(new BuildHiddenMessageAiToolDefinition());

  @JsonCreator
  public BuildHiddenMessageAiTool(
      @JsonProperty("id") UUID id,
      @JsonProperty("authorization") BmvServiceAuthorization authorization,
      @JsonProperty("requestObject") BuildHiddenMessageRequest requestObject,
      @JsonProperty("responseObject") HiddenMessageResponse responseObject,
      @JsonProperty("customRunTimeAttributes") CustomRunTimeAttributes customRunTimeAttributes) {
    super(id, authorization, requestObject, responseObject, customRunTimeAttributes);
  }

  public BuildHiddenMessageAiTool(UUID id, BuildHiddenMessageRequest requestObject) {
    this(id, new BmvServiceAuthorization(), requestObject, null, null);
  }

  @SuppressWarnings("unused") // Public constructor for callers submitting this tool.
  public BuildHiddenMessageAiTool(BuildHiddenMessageRequest requestObject) {
    this(BmvObjectUtils.generateUUID(), requestObject);
  }

  @JsonIgnore
  @Override
  public AiAgentToolDefinition<BuildHiddenMessageRequest, HiddenMessageResponse, BuildHiddenMessageAiTool> getCommandmentDefinition() {
    return TYPE_DEFINITION;
  }
}
