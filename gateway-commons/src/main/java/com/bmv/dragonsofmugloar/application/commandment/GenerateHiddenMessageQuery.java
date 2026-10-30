package com.bmv.dragonsofmugloar.application.commandment;

import static com.bmv.flow.foundry.application.core.GlobalCommandmentDefinitionRegistry.registerCommandmentDefinition;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.bmv.flow.foundry.application.core.Query;
import com.bmv.flow.foundry.application.core.PublicCommandment;
import com.bmv.flow.foundry.application.core.CommandmentImpl;
import com.bmv.flow.foundry.application.core.CustomRunTimeAttributes;
import com.bmv.dragonsofmugloar.application.commandment.definition.GenerateHiddenMessageQueryDefinition;
import com.bmv.dragonsofmugloar.application.commandment.request.BuildHiddenMessageRequest;
import com.bmv.dragonsofmugloar.application.commandment.response.HiddenMessageResponse;
import com.bmv.flow.foundry.domain.model.auth.AnonymousAuthorization;
import com.bmv.flow.foundry.util.BmvObjectUtils;

@JsonTypeName(GenerateHiddenMessageQuery.NAME)
public class GenerateHiddenMessageQuery
    extends
    CommandmentImpl<AnonymousAuthorization, BuildHiddenMessageRequest, HiddenMessageResponse, GenerateHiddenMessageQuery>
    implements
    Query<AnonymousAuthorization, BuildHiddenMessageRequest, HiddenMessageResponse, GenerateHiddenMessageQuery>,
    PublicCommandment<BuildHiddenMessageRequest, HiddenMessageResponse, GenerateHiddenMessageQuery> {

  public static final String NAME = "GENERATE_HIDDEN_MESSAGE";
  public static final GenerateHiddenMessageQueryDefinition TYPE_DEFINITION = registerCommandmentDefinition(new GenerateHiddenMessageQueryDefinition());

  @JsonCreator
  public GenerateHiddenMessageQuery(
      @JsonProperty("id") UUID id,
      @JsonProperty("authorization") AnonymousAuthorization authorization,
      @JsonProperty("requestObject") BuildHiddenMessageRequest requestObject,
      @JsonProperty("responseObject") HiddenMessageResponse responseObject,
      @JsonProperty("customRunTimeAttributes") CustomRunTimeAttributes customRunTimeAttributes) {
    super(id, authorization, requestObject, responseObject, customRunTimeAttributes);
  }

  public GenerateHiddenMessageQuery(UUID id, BuildHiddenMessageRequest requestObject) {
    this(id, AnonymousAuthorization.ANONYMOUS, requestObject, null, null);
  }

  @SuppressWarnings("unused") // Public constructor for callers submitting this query.
  public GenerateHiddenMessageQuery(BuildHiddenMessageRequest requestObject) {
    this(BmvObjectUtils.generateUUID(), requestObject);
  }

  @JsonIgnore
  @Override
  public GenerateHiddenMessageQueryDefinition getCommandmentDefinition() {
    return TYPE_DEFINITION;
  }
}
