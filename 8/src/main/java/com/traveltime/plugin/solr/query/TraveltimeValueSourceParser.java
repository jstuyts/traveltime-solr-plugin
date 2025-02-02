package com.traveltime.plugin.solr.query;

import com.traveltime.plugin.solr.cache.RequestCache;
import lombok.val;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.FunctionQParser;
import org.apache.solr.search.SyntaxError;
import org.apache.solr.search.ValueSourceParser;

public class TraveltimeValueSourceParser extends ValueSourceParser {
   private String cacheName = RequestCache.NAME;

   @Override
   public void init(NamedList args) {
      super.init(args);
      Object cache = args.get("cache");
      if (cache != null) cacheName = cache.toString();
   }

   @Override
   public ValueSource parse(FunctionQParser fp) throws SyntaxError {
      SolrQueryRequest req = fp.getReq();
      RequestCache<TraveltimeQueryParameters> cache = (RequestCache<TraveltimeQueryParameters>) req.getSearcher()
                                                                                                   .getCache(cacheName);
      if (cache == null) {
         throw new SolrException(
               SolrException.ErrorCode.BAD_REQUEST,
               "No request cache configured."
         );
      }

      val queryParameters = TraveltimeQueryParameters.parse(req.getSchema(), new ParamSource(fp.getParams()));
      return new TraveltimeValueSource<>(queryParameters, cache.getOrFresh(queryParameters));
   }
}
