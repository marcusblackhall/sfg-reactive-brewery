package guru.springframework.sfgrestbrewery.services;

import guru.springframework.sfgrestbrewery.domain.Beer;
import guru.springframework.sfgrestbrewery.repositories.BeerRepository;
import guru.springframework.sfgrestbrewery.web.mappers.BeerMapper;
import guru.springframework.sfgrestbrewery.web.model.BeerDto;
import guru.springframework.sfgrestbrewery.web.model.BeerPagedList;
import guru.springframework.sfgrestbrewery.web.model.BeerStyleEnum;
import io.micrometer.core.instrument.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.data.r2dbc.query.Criteria.where;

/**
 * Created by jt on 2019-04-20.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BeerServiceImpl implements BeerService {
    private final BeerRepository beerRepository;
    private final BeerMapper beerMapper;
    private final R2dbcEntityTemplate r2dbcEntityTemplate;

    @Cacheable(cacheNames = "beerListCache", condition = "#showInventoryOnHand == false ")
    @Override
    public Mono<BeerPagedList> listBeers(String beerName, BeerStyleEnum beerStyle, PageRequest pageRequest, Boolean showInventoryOnHand) {
        // strange that the rd Query is not used
        Query query = null;
        if (!StringUtils.isEmpty(beerName)  && beerStyle != null){
            query = Query.query(where("beerName").is(beerName).and("beerStyle").is(beerStyle));

        } else if( !StringUtils.isEmpty(beerName) && beerStyle == null){
            query = Query.query(where("beerName").is(beerName));
        } else if(StringUtils.isEmpty(beerName) && beerStyle != null ){
            query = Query.query(where("beerStyle").is(beerStyle));
        } else {
            query = Query.empty();
        }

            return r2dbcEntityTemplate.select(Beer.class)
                    .matching(query.with(pageRequest))
                    .all()
                    .map(beerMapper::beerToBeerDto)
                    .collect(Collectors.toList())
                    .map(beers -> {
                        return new BeerPagedList(beers,PageRequest.of(pageRequest.getPageNumber(),pageRequest.getPageSize()),beers.size());
                    });

    }

    @Cacheable(cacheNames = "beerCache", key = "#beerId", condition = "#showInventoryOnHand == false ")
    @Override
    public Mono<BeerDto> getById(Integer beerId, Boolean showInventoryOnHand) {
        if (showInventoryOnHand) {

            return beerRepository.findById(beerId)
                    .map(beerMapper::beerToBeerDtoWithInventory);

        } else {

            return beerRepository.findById(beerId)
                    .map(beerMapper::beerToBeerDto);

        }
    }

    @Override
    public BeerDto saveNewBeer(BeerDto beerDto) {
//        return beerMapper.beerToBeerDto(beerRepository.save(beerMapper.beerDtoToBeer(beerDto)));
        return null;
    }

    @Override
    public BeerDto updateBeer(UUID beerId, BeerDto beerDto) {
//        Beer beer = beerRepository.findById(beerId).orElseThrow(NotFoundException::new);
//
//        beer.setBeerName(beerDto.getBeerName());
//        beer.setBeerStyle(BeerStyleEnum.PILSNER.valueOf(beerDto.getBeerStyle()));
//        beer.setPrice(beerDto.getPrice());
//        beer.setUpc(beerDto.getUpc());
//
//        return beerMapper.beerToBeerDto(beerRepository.save(beer));
        return null;
    }

    @Cacheable(cacheNames = "beerUpcCache")
    @Override
    public Mono<BeerDto> getByUpc(String upc) {

       return  beerRepository.findByUpc(upc).map(beerMapper::beerToBeerDto);



    }

    @Override
    public void deleteBeerById(Integer beerId) {

        beerRepository.deleteById(beerId);
    }
}
