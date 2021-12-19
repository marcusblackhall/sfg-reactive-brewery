package guru.springframework.sfgrestbrewery.web.functional;

import guru.springframework.sfgrestbrewery.services.BeerService;
import guru.springframework.sfgrestbrewery.web.controller.NotFoundException;
import guru.springframework.sfgrestbrewery.web.model.BeerDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class BeerHandler {

    final BeerService beerService;
    /**
     * Spring boot provides the validator
     */
    final Validator validator;

    public Mono<ServerResponse> updateBeer(ServerRequest serverRequest) {

        return serverRequest.bodyToMono(BeerDto.class)
                .doOnNext(this::validate)
                .flatMap(beerDto -> {
                    return beerService.updateBeer(Integer.valueOf(serverRequest.pathVariable("beerId")), beerDto);
                })
                .flatMap(savedBeerDto -> {

                    if (savedBeerDto.getId() != null) {
                        log.debug("saved beer with id {}", savedBeerDto.getId());
                        return ServerResponse.noContent().build();
                    } else {

                        log.info("saved beer not found with id {}", savedBeerDto.getId());
                        return ServerResponse.notFound().build();
                    }


                });
    }

    public Mono<ServerResponse> saveNewBeer(ServerRequest request) {

        Mono<BeerDto> beerDtoMono = request.bodyToMono(BeerDto.class)
                .doOnNext(this::validate);

        return beerService.saveNewBeerMono(beerDtoMono).flatMap(beerDto ->
                ServerResponse.ok()
                        .header("location", BeerRouterConfig.API_V_2_BEER + "/" + beerDto.getId()).build()
        );

    }

    public Mono<ServerResponse> getBeerById(ServerRequest request) {

        Integer beerId = Integer.valueOf(request.pathVariable("beerId"));
        Boolean showOnHand = Boolean.valueOf(request.queryParam("showInventoryOnHand").orElse("false"));
        return beerService.getById(beerId, showOnHand).flatMap(
                beerDto -> {
                    return ServerResponse.ok().bodyValue(beerDto);
                }
        ).switchIfEmpty(ServerResponse.notFound().build());


    }

    public Mono<ServerResponse> getBeerByUpc(ServerRequest request) {

        String upc = request.pathVariable("upc");
        return beerService.getByUpc(upc).flatMap(
                beerDto -> {
                    return ServerResponse.ok().bodyValue(beerDto);
                }
        ).switchIfEmpty(ServerResponse.notFound().build());


    }

    private void validate(BeerDto beerDto) {
        log.debug("Validation beer with name {}", beerDto.getBeerName());
        Errors errors = new BeanPropertyBindingResult(beerDto, "beerDto");
        validator.validate(beerDto, errors);
        if (errors.hasErrors()) {
            log.debug("errors found {}", errors.toString());
            throw new ServerWebInputException(errors.toString());
        }

    }

    public Mono<ServerResponse> deleteReactiveBeer(ServerRequest serverRequest) {

        return beerService.deleteReactiveBeer(Integer.valueOf(serverRequest.pathVariable("beerId")))
                .flatMap(voidMono -> ServerResponse.ok().build())
                .onErrorResume(e ->
                        e instanceof NotFoundException, e -> ServerResponse.notFound().build());

    }
}
