package com.traveltime.plugin.solr;

import com.traveltime.plugin.solr.query.timefilter.TimeFilterQueryParameters;
import com.traveltime.plugin.solr.util.Util;
import com.traveltime.sdk.TravelTimeSDK;
import com.traveltime.sdk.auth.TravelTimeCredentials;
import com.traveltime.sdk.dto.common.Coordinates;
import com.traveltime.sdk.dto.common.Location;
import com.traveltime.sdk.dto.common.Property;
import com.traveltime.sdk.dto.requests.TimeFilterRequest;
import com.traveltime.sdk.dto.requests.timefilter.ArrivalSearch;
import com.traveltime.sdk.dto.requests.timefilter.DepartureSearch;
import com.traveltime.sdk.dto.responses.TimeFilterResponse;
import com.traveltime.sdk.dto.responses.errors.IOError;
import com.traveltime.sdk.dto.responses.errors.ResponseError;
import com.traveltime.sdk.dto.responses.errors.TravelTimeError;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class JsonFetcher {
   private final TravelTimeSDK api;

   private final Logger log = LoggerFactory.getLogger(JsonFetcher.class);

   private void logError(TravelTimeError left) {
      if (left instanceof IOError) {
         val ioerr = (IOError) left;
         log.warn(ioerr.getMessage());
         log.warn(
                 Arrays.stream(ioerr.getCause().getStackTrace())
                         .map(StackTraceElement::toString)
                         .reduce("", (a, b) -> a + "\n\t" + b)
         );
      } else if (left instanceof ResponseError) {
         val error = (ResponseError) left;
         log.warn(error.getDescription());
      }
   }

   public JsonFetcher(URI uri, String id, String key) {
      val auth = TravelTimeCredentials.builder().appId(id).apiKey(key).build();
      val builder = TravelTimeSDK.builder().credentials(auth);
      if(uri != null) {
         builder.baseProtoUri(uri);
      }
      api = builder.build();
   }

   private List<Integer> extractTimes(TimeFilterResponse response) {
      val result = response.getResults().get(0);

      val travelTimes = new Integer[result.getLocations().size() + result.getUnreachable().size()];

      result.getLocations()
              .forEach(location ->
                      travelTimes[Integer.parseInt(location.getId())] = location.getProperties().get(0).getTravelTime()
              );

      result.getUnreachable()
              .forEach(unreachableId ->
                      travelTimes[Integer.parseInt(unreachableId)] = -1
              );

      return Arrays.stream(travelTimes).collect(Collectors.toList());
   }

   public List<Integer> getTimes(TimeFilterQueryParameters parameters, ArrayList<Coordinates> points) {
      val requestBuilder = TimeFilterRequest.builder();

      val locations = IntStream
              .range(0,points.size())
              .mapToObj(i -> new Location(String.valueOf(i), points.get(i)))
              .collect(Collectors.toList());

      requestBuilder
              .location(parameters.getLocation())
              .locations(locations);

      switch (parameters.getSearchType()) {
          case ARRIVAL:
             requestBuilder.arrivalSearch(
                     ArrivalSearch
                              .builder()
                              .id("search")
                              .arrivalLocationId(parameters.getLocation().getId())
                              .departureLocationIds(locations.stream().map(Location::getId).collect(Collectors.toList()))
                              .arrivalTime(parameters.getTime())
                              .travelTime(parameters.getTravelTime())
                              .properties(Collections.singletonList(Property.TRAVEL_TIME))
                              .transportation(parameters.getTransportation())
                              .range(parameters.getRange()).build()
             );
             break;
         case DEPARTURE:
            requestBuilder.departureSearch(
                    DepartureSearch
                            .builder()
                            .id("search")
                            .departureLocationId(parameters.getLocation().getId())
                            .arrivalLocationIds(locations.stream().map(Location::getId).collect(Collectors.toList()))
                            .departureTime(parameters.getTime())
                            .travelTime(parameters.getTravelTime())
                            .properties(Collections.singletonList(Property.TRAVEL_TIME))
                            .transportation(parameters.getTransportation())
                            .range(parameters.getRange()).build()
            );
            break;
      }

      val request = requestBuilder.build();


      log.info(String.format("Fetching %d locations", request.getLocations().size() - 1));
      val result = Util.time(log, () -> api.send(request));

      return result.fold(
              err -> {
                 logError(err);
                 throw new RuntimeException(err.toString());
              },
              this::extractTimes
      );
   }

}
