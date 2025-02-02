package com.traveltime.plugin.solr.query.timefilter;

import com.traveltime.plugin.solr.cache.RequestCache;
import com.traveltime.plugin.solr.query.ParamSource;
import com.traveltime.plugin.solr.query.TraveltimeValueSource;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.FunctionQParser;
import org.apache.solr.search.SyntaxError;
import org.apache.solr.search.ValueSourceParser;

public class TimeFilterValueSourceParser extends ValueSourceParser {
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
      RequestCache<TimeFilterQueryParameters> cache = (RequestCache<TimeFilterQueryParameters>) req.getSearcher()
                                                                                                   .getCache(cacheName);
      if (cache == null) {
         throw new SolrException(
               SolrException.ErrorCode.BAD_REQUEST,
               "No request cache configured."
         );
      }

      TimeFilterQueryParameters queryParams = TimeFilterQueryParameters.parse(req.getSchema(),
                                                                              new ParamSource(fp.getParams())
      );
      return new TraveltimeValueSource<>(queryParams, cache.getOrFresh(queryParams));
   }
}
