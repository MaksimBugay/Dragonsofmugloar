package com.bmv.gateway.infrastructure.adapter.in;

import static com.trimble.transportation.reference.data.management.serialization.json.JsonUtility.toJson;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Supplier;

import com.trimble.transportation.reference.data.management.application.exception.CommandmentInvocationException;
import com.trimble.transportation.reference.data.management.application.lock.LockService;
import com.trimble.transportation.reference.data.management.application.port.CommandmentDispatcher;
import com.trimble.transportation.reference.data.management.application.port.cache.CommandmentExecutionCache;
import com.trimble.transportation.reference.data.management.application.port.out.InternalApiOutboundPort;
import com.trimble.transportation.reference.data.management.application.port.out.async.AsyncCommandmentExecutionAdapter;
import com.trimble.transportation.reference.data.management.application.service.authorization.AuthorizationService;
import com.trimble.transportation.reference.data.management.application.service.permission.CheckPermissionsService;
import com.trimble.transportation.reference.data.management.domain.model.api.Commandment;
import com.trimble.transportation.reference.data.management.domain.model.api.CommandmentWithDefinition;
import com.trimble.transportation.reference.data.management.domain.model.api.error.RdmError;
import com.trimble.transportation.reference.data.management.domain.model.api.error.WithErrorDetails;
import com.trimble.transportation.reference.data.management.domain.model.api.exception.RdmExceptionBase;
import com.trimble.transportation.reference.data.management.domain.model.auth.ClientAuthorization;
import com.trimble.transportation.reference.data.management.domain.model.cluster.RdmService;
import com.bmv.gateway.config.RdmCacheConfig;
import com.bmv.gateway.config.RdmConfig;
import com.trimble.transportation.reference.data.management.infrastructure.adapter.in.CommandmentRouter;
import com.trimble.transportation.reference.data.management.util.LoggingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@SuppressWarnings("unused")
@Component
public class GatewayCommandmentRouter extends CommandmentRouter {

  private static final Logger LOGGER = LoggerFactory.getLogger("GatewayCommandRouter");

  private final AuthorizationService authorizationService;

  private final CheckPermissionsService checkPermissionsService;

  public GatewayCommandmentRouter(
      List<CommandmentDispatcher> commandmentDispatchers,
      @Qualifier(value = "GatewayCommandmentExecutor") InternalApiOutboundPort internalApiOutboundPort,
      AsyncCommandmentExecutionAdapter asyncCommandmentExecutionAdapter,
      @Qualifier(value = "GatewayConditionalCommandmentExecutionCache") CommandmentExecutionCache commandmentExecutionCache,
      RdmCacheConfig rdmCacheConfig,
      RdmConfig rdmConfig,
      @Value("${rdm.cluster.secret}") String rdmClusterSecret,
      LockService lockService) {
    super(
        RdmService.GATEWAY,
        commandmentDispatchers,
        internalApiOutboundPort,
        null,
        asyncCommandmentExecutionAdapter,
        commandmentExecutionCache,
        (long) rdmCacheConfig.getExpireAfterSec(),
        null,
        rdmConfig.getCommandmentExecutionTimeout(),
        null,
        null,
        lockService,
        rdmClusterSecret.getBytes(StandardCharsets.UTF_8),
        true
    );

    this.authorizationService = this.commandmentDispatchers.stream()
        .filter(AuthorizationService.class::isInstance)
        .map(AuthorizationService.class::cast)
        .findFirst().orElse(null);

    this.checkPermissionsService = this.commandmentDispatchers.stream()
        .filter(CheckPermissionsService.class::isInstance)
        .map(CheckPermissionsService.class::cast)
        .findFirst().orElse(null);
  }

  public <A extends ClientAuthorization, P, R extends WithErrorDetails, C extends Commandment<A, P, R>> Mono<R> execute(
      String commandmentName, Supplier<CommandmentWithDefinition<A, P, R, C>> commandmentSupplier) {
    return Mono.just(commandmentSupplier.get())
        .onErrorResume(
            throwable -> Mono.error(
                new CommandmentInvocationException(
                    RdmExceptionBase.buildCauseMessage(throwable, RdmService.GATEWAY),
                    commandmentName
                )
            )
        )
        .flatMap(this::execute);
  }

  @Override
  public <A extends ClientAuthorization, P, R extends WithErrorDetails, C extends Commandment<A, P, R>> Mono<R> execute(
      CommandmentWithDefinition<A, P, R, C> commandment) {
    return super.execute(commandment)
        .doOnNext(response -> {
              if (response.hasError()) {
                if (response.errorDetails().code() == RdmError.FUTURE_RESPONSE_IS_NOT_READY.getCode()) {
                  LOGGER.debug(
                      "Error during commandment execution: commandment {}, error {}",
                      toJson(LoggingHelper.stripSensitiveData(commandment)),
                      response.errorDetails()
                  );
                } else {
                  LOGGER.error(
                      "Error during commandment execution: commandment {}, error {}",
                      toJson(LoggingHelper.stripSensitiveData(commandment)),
                      response.errorDetails()
                  );
                }
              }
            }
        )
        .doOnError(
            throwable -> LOGGER.error(
                "Unexpected Error during commandment execution: commandment {}",
                toJson(LoggingHelper.stripSensitiveData(commandment)),
                throwable)
        );
  }

  @SuppressWarnings("unused")
  public AuthorizationService getAuthorizationService() {
    return authorizationService;
  }

  public CheckPermissionsService getCheckPermissionsService() {
    return checkPermissionsService;
  }
}
