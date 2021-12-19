package guru.springframework.sfgrestbrewery.ittests;

import guru.springframework.sfgrestbrewery.bootstrap.BeerLoader;
import guru.springframework.sfgrestbrewery.web.functional.BeerRouterConfig;
import guru.springframework.sfgrestbrewery.web.model.BeerDto;
import guru.springframework.sfgrestbrewery.web.model.BeerPagedList;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Slf4j
public class WebClientV2IT {


    public static final String BASE_URL = "http://localhost:8080";
    public static final String BEER_V2_PATH = "/api/v2/beer";
    public static final String BEER_V2_PATH_ID = "/api/v2/beer/{id}";

    WebClient webClient;


    @BeforeEach
    void setUp() {

        webClient = WebClient.builder()
                .baseUrl(BASE_URL)
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create().wiretap(true)))
                .build();
    }

    @Test
    void shouldReturnBeerById() throws InterruptedException {

        CountDownLatch countDownLatch = new CountDownLatch(1);

        Mono<BeerDto> beerDtoMono = webClient.get().uri(uriBuilder ->
                        uriBuilder.path(BEER_V2_PATH_ID)
                                .build(1))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(BeerDto.class);

        beerDtoMono.subscribe(beer -> {
                    assertThat(beer).isNotNull();
                    log.info("Found beer with id {}", beer.getId());
                    assertThat(beer.getId()).isNotNull();
                    countDownLatch.countDown();
                }
        );

        countDownLatch.await(2, TimeUnit.SECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);

    }

    @Test
    void shouldReturnBeerByUpc() throws InterruptedException {

        CountDownLatch countDownLatch = new CountDownLatch(1);

        Mono<BeerDto> beerDtoMono = webClient.get().uri(uriBuilder ->
                        uriBuilder.path(BeerRouterConfig.API_V_2_BEER_UPC).build(BeerLoader.BEER_1_UPC))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(BeerDto.class);

        beerDtoMono.subscribe(beer -> {
            assertThat(beer).isNotNull();
            log.info("Found beer with upc {}", beer.getUpc());
            assertThat(beer.getUpc()).isEqualTo(BeerLoader.BEER_1_UPC);
            countDownLatch.countDown();
        }, response -> {
            log.info("response", response);

        });

        countDownLatch.await(2, TimeUnit.SECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);

    }

    @Test
    void shouldReturn404IfUpcCodeNotFound() throws InterruptedException {

        CountDownLatch countDownLatch = new CountDownLatch(1);

        Mono<BeerDto> beerDtoMono = webClient.get().uri(uriBuilder ->
                        uriBuilder.path("/api/v1/beerUpc/{upc}").build("doesntexistUpc"))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(BeerDto.class);

        beerDtoMono.subscribe(beer -> {
                    log.info("Shouldnt come here {}", beer.getUpc());


                }, response -> {
                    log.info("response is {}", response.getMessage());
                    countDownLatch.countDown();
                }
        );

        countDownLatch.await(2, TimeUnit.SECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);

    }


    @Test
    void shouldReturnListOfBeers() throws InterruptedException {

        CountDownLatch countDownLatch = new CountDownLatch(1);

        Mono<BeerPagedList> beerPagedListMono = webClient.get().uri("/api/v1/beer")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve().bodyToMono(BeerPagedList.class);


        BeerPagedList pagedList = beerPagedListMono.block();
        pagedList.getContent().forEach(beerDto -> System.out.println(beerDto.toString()));
        beerPagedListMono.publishOn(Schedulers.parallel()).subscribe(beerPagedList -> {

            beerPagedList.getContent().forEach(beerDto -> System.out.println(beerDto.toString()));

            countDownLatch.countDown();
        });

        countDownLatch.await();
    }

    @Test
    void shoudSaveBeer() throws InterruptedException {

        CountDownLatch countDownLatch = new CountDownLatch(1);
        BeerDto beerDto = BeerDto.builder()
                .beerName("My Beer")
                .upc("8687655555")
                .price(BigDecimal.valueOf(34.44))
                .beerStyle("PALE_ALE").build();

        Mono<ResponseEntity<Void>> beerMono = webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/beer")

                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(beerDto))
                .retrieve()
                .toBodilessEntity();

        beerMono.publishOn(Schedulers.parallel()).subscribe(responseEntity ->
                {
                    assertThat(responseEntity.getStatusCode().is2xxSuccessful());
                    countDownLatch.countDown();
                }
        );

        countDownLatch.await(2000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);


    }

    @Test
    void shoudSaveBeerV2() throws InterruptedException {

        CountDownLatch countDownLatch = new CountDownLatch(1);
        BeerDto beerDto = BeerDto.builder()
                .beerName("My Beer v2")
                .upc("8687655555")
                .price(BigDecimal.valueOf(34.44))
                .beerStyle("PALE_ALE").build();

        Mono<ResponseEntity<Void>> beerResponseMono = webClient.post()
                .uri(BeerRouterConfig.API_V_2_BEER)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(beerDto))
                .retrieve().toBodilessEntity();


        beerResponseMono.publishOn(Schedulers.parallel()).subscribe(responseEntity ->
                {
                    assertThat(responseEntity.getStatusCode().is2xxSuccessful());
                    countDownLatch.countDown();
                }
        );

        countDownLatch.await(2000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);


    }

    @Test
    void testSaveBeerBadRequest() throws InterruptedException {

        CountDownLatch countDownLatch = new CountDownLatch(1);

        // beer is missing the style and name
        BeerDto beerDto = BeerDto.builder()
                .price(new BigDecimal("8.99"))
                .build();

        Mono<ResponseEntity<Void>> beerResponseMono = webClient.post().uri(BeerRouterConfig.API_V_2_BEER)
                .accept(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(beerDto))
                .retrieve().toBodilessEntity();

        beerResponseMono.subscribe(responseEntity -> {

        }, throwable -> {
            if (throwable.getClass().getName().equals("org.springframework.web.reactive.function.client.WebClientResponseException$BadRequest")){
                WebClientResponseException ex = (WebClientResponseException) throwable;

                if (ex.getStatusCode().equals(HttpStatus.BAD_REQUEST)){
                    countDownLatch.countDown();
                }
            }
        });

        countDownLatch.await(2000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }

    @Test
    void testUpdateBeer() throws InterruptedException {

        final String newBeerName = "JTs Beer";
        final Integer beerId = 1;
        CountDownLatch countDownLatch = new CountDownLatch(2);

        webClient.put().uri(uriBuilder -> uriBuilder.path(BeerRouterConfig.API_V_2_BEER_ID).build(beerId))
                .accept(MediaType.APPLICATION_JSON).body(BodyInserters
                        .fromValue(BeerDto.builder()
                                .beerName(newBeerName)
                                .upc("1233455")
                                .beerStyle("PALE_ALE")
                                .price(new BigDecimal("8.99"))
                                .build()))
                .retrieve().toBodilessEntity()
                .subscribe(responseEntity -> {
                    assertThat(responseEntity.getStatusCode().is2xxSuccessful());
                    countDownLatch.countDown();
                });

        //wait for update thread to complete
        countDownLatch.await(500, TimeUnit.MILLISECONDS);

        webClient.get().uri(uriBuilder ->  uriBuilder.path(BeerRouterConfig.API_V_2_BEER_ID).build(beerId))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve().bodyToMono(BeerDto.class)
                .subscribe(beer -> {
                    assertThat(beer).isNotNull();
                    assertThat(beer.getBeerName()).isNotNull();
                    assertThat(beer.getBeerName()).isEqualTo(newBeerName);
                    countDownLatch.countDown();
                });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }

    @Test
    void testUpdateBeerNotFound() throws InterruptedException {

        final String newBeerName = "JTs Beer";
        final Integer beerId = 999;
        CountDownLatch countDownLatch = new CountDownLatch(1);

        webClient.put().uri(uriBuilder -> uriBuilder.path(BeerRouterConfig.API_V_2_BEER_ID).build(beerId))
                .accept(MediaType.APPLICATION_JSON).body(BodyInserters
                        .fromValue(BeerDto.builder()
                                .beerName(newBeerName)
                                .upc("1233455")
                                .beerStyle("PALE_ALE")
                                .price(new BigDecimal("8.99"))
                                .build()))
                .retrieve().toBodilessEntity()
                .subscribe(responseEntity -> {
                    assertThat(responseEntity.getStatusCode().is2xxSuccessful());
                }, throwable -> {
                    countDownLatch.countDown();
                });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }

    @Test
    void testDeleteBeer() {
        Integer beerId = 3;
        CountDownLatch countDownLatch = new CountDownLatch(1);

        webClient.delete().uri("/api/v2/beer/" + beerId )
                .retrieve().toBodilessEntity()
                .flatMap(responseEntity -> {
                    countDownLatch.countDown();

                    return webClient.get().uri("/api/v2/beer/" + beerId)
                            .accept(MediaType.APPLICATION_JSON)
                            .retrieve().bodyToMono(BeerDto.class);
                }) .subscribe(savedDto -> {

                }, throwable -> {
                    countDownLatch.countDown();
                });
    }

    @Test
    void testDeleteBeerNotFound() {
        Integer beerId = 4;

        webClient.delete().uri("/api/v2/beer/" + beerId )
                .retrieve().toBodilessEntity().block();

        assertThrows(WebClientResponseException.NotFound.class, () -> {
            webClient.delete().uri("/api/v2/beer/" + beerId )
                    .retrieve().toBodilessEntity().block();
        });
    }




}