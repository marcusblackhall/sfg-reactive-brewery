package guru.springframework.sfgrestbrewery.web.functional;

import guru.springframework.sfgrestbrewery.services.BeerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class BeerHandler {

    final BeerService beerService;

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

}
