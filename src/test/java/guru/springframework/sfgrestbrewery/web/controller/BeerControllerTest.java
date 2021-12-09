package guru.springframework.sfgrestbrewery.web.controller;

import guru.springframework.sfgrestbrewery.bootstrap.BeerLoader;
import guru.springframework.sfgrestbrewery.services.BeerService;
import guru.springframework.sfgrestbrewery.web.model.BeerDto;
import guru.springframework.sfgrestbrewery.web.model.BeerPagedList;
import guru.springframework.sfgrestbrewery.web.model.BeerStyleEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.querydsl.QPageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@WebFluxTest(BeerController.class)
class BeerControllerTest {

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    BeerService beerService;

    BeerDto validBeer;
    @BeforeEach
    void setUp() {

      validBeer =  BeerDto.builder()
                .beerName("Test beer")
                .beerStyle("PALE_ALE")
                .upc(BeerLoader.BEER_1_UPC)
                .price(BigDecimal.valueOf(34.56))
                .build();
    }

    @Test
    void getBeerById(){

        UUID uuid = UUID.randomUUID();
        given(beerService.getById(any(),any())).willReturn(validBeer);

        webTestClient.get()
                .uri("/api/v1/beer/" + uuid)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(BeerDto.class)
                .value(beerDto -> beerDto.getBeerName(),equalTo(validBeer.getBeerName()));




    }

    @Test
    void getBeerByUpc(){
        String testUpc = BeerLoader.BEER_1_UPC;
        given(beerService.getByUpc(testUpc)).willReturn(validBeer);

        webTestClient.get()
                .uri("/api/v1/beerUpc/" + testUpc)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(BeerDto.class)
                .value(beerDto -> beerDto.getUpc(),equalTo(validBeer.getUpc()));
    }

    @Test
    void shouldListBeers(){


        List<BeerDto> validBeerList = List.of(this.validBeer);
        BeerPagedList beerPagedList = new BeerPagedList(validBeerList);
        given(beerService.listBeers("Test beer",BeerStyleEnum.PALE_ALE,
                PageRequest.of(0,25),false))
                .willReturn(beerPagedList);


        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/beer")
                        .queryParam("beerName","Test beer")
                        .queryParam("beerStyle","PALE_ALE")
                        .build()
                )
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(BeerPagedList.class)
          .value(bpl -> bpl.getContent(),equalTo(beerPagedList.getContent()));


    }

    @AfterEach
    void tearDown() {
    }
}