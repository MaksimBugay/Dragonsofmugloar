package com.bmv.dragonsofmugloar.domain.api.commandment;

import static com.bmv.flow.foundry.application.core.GlobalCommandmentDefinitionRegistry.registerCommandmentDefinition;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.bmv.flow.foundry.application.core.Command;
import com.bmv.flow.foundry.application.core.CommandmentWithMultipleStagesAndMutableBufferImpl;
import com.bmv.flow.foundry.application.core.CustomRunTimeAttributes;
import com.bmv.flow.foundry.application.core.PublicCommandment;
import com.bmv.flow.foundry.domain.model.auth.AnonymousAuthorization;
import com.bmv.dragonsofmugloar.domain.api.commandment.definition.PlayDragonsofmugloarGameCommandDefinition;
import com.bmv.dragonsofmugloar.domain.api.request.PlayGameRequest;
import com.bmv.dragonsofmugloar.domain.api.response.PlayGameResponse;
import com.bmv.dragonsofmugloar.domain.model.GameConfig;
import com.bmv.dragonsofmugloar.domain.model.GameState;

@JsonTypeName(PlayDragonsofmugloarGameCommand.NAME)
public class PlayDragonsofmugloarGameCommand extends CommandmentWithMultipleStagesAndMutableBufferImpl<
    AnonymousAuthorization, PlayGameRequest, PlayGameResponse, PlayDragonsofmugloarGameCommand,
    PlayDragonsofmugloarGameCommand.Buffer>
    implements Command<AnonymousAuthorization, PlayGameRequest, PlayGameResponse, PlayDragonsofmugloarGameCommand>,
    PublicCommandment<PlayGameRequest, PlayGameResponse, PlayDragonsofmugloarGameCommand> {

  public static final String NAME = "PLAY_DRAGONSOFMUGLOAR_GAME";
  public static final PlayDragonsofmugloarGameCommandDefinition TYPE_DEFINITION =
      registerCommandmentDefinition(new PlayDragonsofmugloarGameCommandDefinition());

  @JsonCreator
  public PlayDragonsofmugloarGameCommand(
      @JsonProperty("id") UUID id,
      @JsonProperty("authorization") AnonymousAuthorization authorization,
      @JsonProperty("requestObject") PlayGameRequest requestObject,
      @JsonProperty("responseObject") PlayGameResponse responseObject,
      @JsonProperty("customRunTimeAttributes") CustomRunTimeAttributes customRunTimeAttributes,
      @JsonProperty("stageNumber") int stageNumber,
      @JsonProperty("mutableBuffer") Buffer buffer) {
    super(id, authorization, requestObject, responseObject, customRunTimeAttributes, stageNumber,
        buffer == null ? Buffer.empty() : buffer);
  }

  public PlayDragonsofmugloarGameCommand(PlayGameRequest request) {
    this(null, AnonymousAuthorization.ANONYMOUS, request, null, null, 0, Buffer.empty());
  }

  @JsonIgnore
  @Override
  public PlayDragonsofmugloarGameCommandDefinition getCommandmentWithMultipleStagesAndMutableBufferDefinition() {
    return TYPE_DEFINITION;
  }

  public record Buffer(GameState gameState, GameConfig gameConfig) {

    public static Buffer empty() {
      return new Buffer(null, null);
    }

    public Buffer withGameState(GameState state) {
      return new Buffer(state, gameConfig);
    }
  }
}
