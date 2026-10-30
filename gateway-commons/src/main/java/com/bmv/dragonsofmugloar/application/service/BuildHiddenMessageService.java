package com.bmv.dragonsofmugloar.application.service;

import com.bmv.flow.foundry.domain.model.api.exception.CommandmentInvocationException;
import com.bmv.flow.foundry.application.core.ai.AiMessage;
import com.bmv.flow.foundry.application.core.cflow.CFlowStateWithAiAgentTool;
import com.bmv.flow.foundry.application.service.ai.AiAgentToolWithAiPromptServiceBase;
import com.bmv.dragonsofmugloar.application.commandment.BuildHiddenMessageAiTool;
import com.bmv.flow.foundry.domain.model.api.exception.BmvExceptionBase;
import com.bmv.dragonsofmugloar.application.commandment.request.BuildHiddenMessageRequest;
import com.bmv.dragonsofmugloar.application.commandment.response.HiddenMessageResponse;
import com.bmv.flow.foundry.domain.model.cluster.BmvService;
import com.bmv.flow.foundry.infrastructure.adapter.out.ai.azure.gpt_5_4.AiAgentToolAzureGpt54OutboundPort;
import com.bmv.flow.foundry.infrastructure.adapter.out.ai.azure.gpt_5_4.OpenAiPortConfig;
import com.bmv.flow.foundry.infrastructure.adapter.out.ai.azure.request.OpenAiRequest;
import com.bmv.flow.foundry.infrastructure.adapter.out.ai.azure.request_config.OpenAiModel;
import com.bmv.flow.foundry.infrastructure.adapter.out.ai.azure.request_config.ReasoningConfig;
import com.bmv.flow.foundry.infrastructure.adapter.out.ai.azure.request_config.ResponseFormatConfig;
import com.bmv.flow.foundry.serialization.json.JsonUtility;
import com.bmv.flow.foundry.util.BmvObjectUtils;

public class BuildHiddenMessageService
    extends AiAgentToolWithAiPromptServiceBase<BuildHiddenMessageRequest, HiddenMessageResponse, BuildHiddenMessageAiTool> {

  private static final OpenAiPortConfig PORT_CONFIG = new OpenAiPortConfig(
      OpenAiModel.DEEPSEEK_FLASH,
      //new ReasoningConfig(ReasoningEffort.LOW, null),
      ReasoningConfig.NO_REASONING,
      new ResponseFormatConfig(GeneratedHiddenMessage.class),
      false,
      false,
      18000,
      null);

  public OpenAiPortConfig getOpenAiPortConfig() {
    return PORT_CONFIG;
  }

  @Override
  protected AiMessage prepareAiPrompt(
      CFlowStateWithAiAgentTool<BuildHiddenMessageRequest, HiddenMessageResponse, BuildHiddenMessageAiTool> flowState) {
    var portConfig = getOpenAiPortConfig();
    var openAiRequest = new OpenAiRequest(
        portConfig.model(),
        flowState.getAgentDefinition(),
        JsonUtility.toJson(flowState.getRequestObject()),
        portConfig.reasoning(),
        portConfig.text(),
        portConfig.tools(),
        portConfig.toolChoice(),
        portConfig.maxOutputTokens(),
        portConfig.store(),
        portConfig.stream());
    String preparedRequest = JsonUtility.toJson(openAiRequest);
    return new AiMessage(BmvObjectUtils.generateUUID(), AiMessage.Role.prompt, null,
        () -> preparedRequest, BmvObjectUtils.getCurrentTimeMillis());
  }

  @Override
  protected Class<?> getAiPortImplementationClass() {
    return AiAgentToolAzureGpt54OutboundPort.class;
  }

  @Override
  protected HiddenMessageResponse buildResponse(
      CFlowStateWithAiAgentTool<BuildHiddenMessageRequest, HiddenMessageResponse, BuildHiddenMessageAiTool> flowState,
      AiMessage aiAssistantMessage) {
    try {
      var response = JsonUtility.fromJson(aiAssistantMessage.content(), GeneratedHiddenMessage.class);
      return new HiddenMessageResponse(response.text(), null);
    } catch (Exception exception) {
      throw new CommandmentInvocationException(
          "Unexpected error during AI response parsing: "
              + BmvExceptionBase.buildCauseMessage(exception, BmvService.GATEWAY),
          flowState.getName());
    }
  }

  // The model returns only text; the framework response carries type and error details separately.
  private record GeneratedHiddenMessage(String text) {
  }
}
