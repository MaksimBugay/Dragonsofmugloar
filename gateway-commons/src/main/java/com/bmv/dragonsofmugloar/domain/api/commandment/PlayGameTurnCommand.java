package com.bmv.dragonsofmugloar.domain.api.commandment;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.trimble.transportation.reference.data.management.domain.model.api.Command;
import com.trimble.transportation.reference.data.management.domain.model.api.CommandmentWithMultipleStagesAndMutableBufferImpl;
import com.trimble.transportation.reference.data.management.domain.model.api.CustomRunTimeAttributes;
import com.trimble.transportation.reference.data.management.domain.model.auth.RdmServiceAuthorization;
import com.bmv.dragonsofmugloar.domain.api.commandment.definition.PlayGameTurnCommandDefinition;
import com.bmv.dragonsofmugloar.domain.api.request.PlayGameTurnRequest;
import com.bmv.dragonsofmugloar.domain.api.response.PlayGameTurnResponse;
import com.bmv.dragonsofmugloar.domain.model.Ad;
import com.bmv.dragonsofmugloar.domain.model.ShopItem;
import com.bmv.dragonsofmugloar.domain.model.GameActionWithExecutionContext;

@JsonTypeName(PlayGameTurnCommand.NAME)
public class PlayGameTurnCommand extends CommandmentWithMultipleStagesAndMutableBufferImpl<
    RdmServiceAuthorization, PlayGameTurnRequest, PlayGameTurnResponse, PlayGameTurnCommand, PlayGameTurnCommand.Buffer>
    implements Command<RdmServiceAuthorization, PlayGameTurnRequest, PlayGameTurnResponse, PlayGameTurnCommand> {

  public static final String NAME = "PLAY_GAME_TURN";
  public static final PlayGameTurnCommandDefinition TYPE_DEFINITION = new PlayGameTurnCommandDefinition();

  @JsonCreator
  public PlayGameTurnCommand(
      @JsonProperty("id") UUID id,
      @JsonProperty("authorization") RdmServiceAuthorization authorization,
      @JsonProperty("requestObject") PlayGameTurnRequest requestObject,
      @JsonProperty("responseObject") PlayGameTurnResponse responseObject,
      @JsonProperty("customRunTimeAttributes") CustomRunTimeAttributes customRunTimeAttributes,
      @JsonProperty("stageNumber") int stageNumber,
      @JsonProperty("mutableBuffer") Buffer buffer) {
    super(id, authorization, requestObject, responseObject, customRunTimeAttributes, stageNumber,
        buffer == null ? Buffer.empty() : buffer);
  }

  public PlayGameTurnCommand(PlayGameTurnRequest request) {
    this(null, new RdmServiceAuthorization(), request, null, null, 0, Buffer.empty());
  }

  @JsonIgnore
  @Override
  public PlayGameTurnCommandDefinition getCommandmentWithMultipleStagesAndMutableBufferTypeDefinition() {
    return TYPE_DEFINITION;
  }

  public record Buffer(List<ShopItem> shopItems, List<Ad> ads, GameActionWithExecutionContext executionContext) {

    public Buffer {
      shopItems = List.copyOf(shopItems);
      ads = List.copyOf(ads);
    }

    public static Buffer empty() {
      return new Buffer(List.of(), List.of(), null);
    }

    public Buffer withShopItems(List<ShopItem> items) {
      return new Buffer(items, ads, executionContext);
    }

    public Buffer withAds(List<Ad> availableAds) {
      return new Buffer(shopItems, availableAds, executionContext);
    }

    public Buffer withExecutionContext(GameActionWithExecutionContext context) {
      return new Buffer(shopItems, ads, context);
    }
  }
}
