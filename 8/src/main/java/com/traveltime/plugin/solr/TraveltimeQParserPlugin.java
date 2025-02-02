package com.traveltime.plugin.solr;

import com.traveltime.plugin.solr.cache.RequestCache;
import com.traveltime.plugin.solr.fetcher.ProtoFetcherSingleton;
import com.traveltime.plugin.solr.query.TraveltimeQueryParser;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;

import java.net.URI;

public class TraveltimeQParserPlugin extends QParserPlugin {
   private String cacheName = RequestCache.NAME;

   @Override
   public void init(NamedList args) {
      Object cache = args.get("cache");
      if (cache != null) cacheName = cache.toString();

      Object uriVal = args.get("api_uri");
      URI uri = null;
      if (uriVal != null) uri = URI.create(uriVal.toString());

      String appId = args.get("app_id").toString();
      String apiKey = args.get("api_key").toString();
      ProtoFetcherSingleton.INSTANCE.init(uri, appId, apiKey);
   }

   @Override
   public QParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
      return new TraveltimeQueryParser(qstr,
                                       localParams,
                                       params,
                                       req,
                                       ProtoFetcherSingleton.INSTANCE.getFetcher(),
                                       cacheName
      );
   }

}
