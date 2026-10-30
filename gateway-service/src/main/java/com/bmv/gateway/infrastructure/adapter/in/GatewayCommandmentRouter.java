package com.bmv.gateway.infrastructure.adapter.in;



import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import com.bmv.flow.foundry.domain.model.api.exception.CommandmentInvocationException;
import com.bmv.flow.foundry.application.lock.LockService;
import com.bmv.flow.foundry.application.port.cache.CommandmentExecutionCache;
import com.bmv.flow.foundry.application.port.out.InternalApiOutboundPort;
import com.bmv.flow.foundry.application.port.out.async.AsyncCommandmentExecutionAdapter;
import com.bmv.flow.foundry.application.service.authorization.AuthorizationService;
import com.bmv.flow.foundry.application.service.permission.CheckPermissionsService;
import com.bmv.flow.foundry.application.core.Commandment;
import com.bmv.flow.foundry.application.core.CommandmentWithDefinition;
import com.bmv.flow.foundry.domain.model.api.error.BmvError;
import com.bmv.flow.foundry.domain.model.api.error.WithErrorDetails;
import com.bmv.flow.foundry.domain.model.api.exception.BmvExceptionBase;
import com.bmv.flow.foundry.domain.model.auth.ClientAuthorization;
import com.bmv.flow.foundry.domain.model.cluster.BmvService;
import com.bmv.gateway.config.RdmCacheConfig;
import com.bmv.gateway.config.RdmConfig;
import com.bmv.flow.foundry.infrastructure.adapter.in.CommandmentRouter;
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
      AuthorizationService authorizationService,
      CheckPermissionsService checkPermissionsService,
      @Qualifier(value = "GatewayCommandmentExecutor") InternalApiOutboundPort internalApiOutboundPort,
      AsyncCommandmentExecutionAdapter asyncCommandmentExecutionAdapter,
      @Qualifier(value = "GatewayConditionalCommandmentExecutionCache") CommandmentExecutionCache commandmentExecutionCache,
      RdmCacheConfig rdmCacheConfig,
      RdmConfig rdmConfig,
      @Value("${rdm.cluster.secret}") String rdmClusterSecret,
      LockService lockService) {
    super(
        BmvService.GATEWAY,
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

    this.authorizationService = authorizationService;
    this.checkPermissionsService = checkPermissionsService;
  }

  public <A extends ClientAuthorization, P, R extends WithErrorDetails, C extends Commandment<A, P, R>> Mono<R> execute(
      String commandmentName, Supplier<CommandmentWithDefinition<A, P, R, C>> commandmentSupplier) {
    return Mono.just(commandmentSupplier.get())
        .onErrorResume(
            throwable -> Mono.error(
                new CommandmentInvocationException(
                    BmvExceptionBase.buildCauseMessage(throwable, BmvService.GATEWAY),
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
                if (response.errorDetails().code() == BmvError.FUTURE_RESPONSE_IS_NOT_READY.getCode()) {
                  LOGGER.debug(
                      "Error during commandment execution: commandment {}, error {}",
                      commandment.getName(),
                      response.errorDetails()
                  );
                } else {
                  LOGGER.error(
                      "Error during commandment execution: commandment {}, error {}",
                      commandment.getName(),
                      response.errorDetails()
                  );
                }
              }
            }
        )
        .doOnError(
            throwable -> LOGGER.error(
                "Unexpected Error during commandment execution: commandment {}",
                commandment.getName(),
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
