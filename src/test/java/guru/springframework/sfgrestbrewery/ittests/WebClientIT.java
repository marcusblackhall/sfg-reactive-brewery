package guru.springframework.sfgrestbrewery.ittests;

import guru.springframework.sfgrestbrewery.bootstrap.BeerLoader;
import guru.springframework.sfgrestbrewery.web.model.BeerDto;
import guru.springframework.sfgrestbrewery.web.model.BeerPagedList;
import guru.springframework.sfgrestbrewery.web.model.BeerStyleEnum;
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
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static guru.springframework.sfgrestbrewery.bootstrap.BeerLoader.BEER_8_UPC;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by jt on 3/7/21.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Slf4j
public class WebClientIT {

    public static final String BASE_URL = "http://localhost:8080";

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
                        uriBuilder.path("/api/v1/beer/{id}")
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
    void shouldUpdateBeer() throws InterruptedException {

        CountDownLatch countDownLatch = new CountDownLatch(3);

        webClient.get().uri("/api/v1/beer")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(BeerPagedList.class)
                .publishOn(Schedulers.single())
                .subscribe(beerPagedList -> {
                    countDownLatch.countDown();
                    BeerDto beerDto = beerPagedList.getContent().get(0);
                    log.info("Going to update beer {} with id {}", beerDto.getBeerName(), beerDto.getId());

                    BeerDto updatePayload = BeerDto.builder().beerName("MyUpdate Beer")
                            .beerStyle(beerDto.getBeerStyle())
                            .price(beerDto.getPrice())
                            .upc(beerDto.getUpc()).build();

                    webClient.put()
                            .uri(uriBuilder -> uriBuilder.path("/api/v1/beer/{beerId}")
                                    .build(beerDto.getId()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromValue(updatePayload))
                            .retrieve()
                            .toBodilessEntity()
                            .flatMap(responseEntity -> {
                                countDownLatch.countDown();
                                return webClient.get().uri("/api/v1/beer/" + beerDto.getId())
                                        .accept(MediaType.APPLICATION_JSON)
                                        .retrieve().bodyToMono(BeerDto.class);

                            })
                            .subscribe(savedDto -> {
                                assertThat(savedDto.getBeerName()).isEqualTo("MyUpdate Beer");
                                log.info("Beer name is now {}", savedDto.getBeerName());
                                countDownLatch.countDown();
                            });


                });

        countDownLatch.await(3, TimeUnit.SECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);


    }

    @Test
    void testUpdateBeerNotFound() throws InterruptedException {

        CountDownLatch countDownLatch = new CountDownLatch(1);
        BeerDto updatePayload = BeerDto.builder().beerName("MyUpdate Beer")
                .beerStyle("PALE_ALE")
                .price(new BigDecimal("9.99"))
                .upc(BEER_8_UPC).build();

// put with an id that doesnt exist
        webClient.put()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/beer/{beerId}")
                        .build(200))
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(updatePayload))
                .retrieve()
                .toBodilessEntity()

                .subscribe(savedDto -> {
                        log.info("Status code here is {}",savedDto.getStatusCode());

                }, throwable -> {

                    if (throwable instanceof WebClientResponseException.NotFound){
                        WebClientResponseException exception = (WebClientResponseException) throwable;
                        if (exception.getStatusCode().equals(HttpStatus.NOT_FOUND)){
                            countDownLatch.countDown();
                        }
                    }
                });

        countDownLatch.await(3,TimeUnit.SECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);


    }

    @Test
    void shouldReturnBeerByUpc() throws InterruptedException {

        CountDownLatch countDownLatch = new CountDownLatch(1);

        Mono<BeerDto> beerDtoMono = webClient.get().uri(uriBuilder ->
                        uriBuilder.path("/api/v1/beerUpc/{upc}").build(BeerLoader.BEER_1_UPC))
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
    void shouldSaveBeer() throws InterruptedException {

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



}