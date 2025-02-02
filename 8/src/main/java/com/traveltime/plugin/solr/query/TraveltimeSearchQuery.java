package com.traveltime.plugin.solr.query;

import com.traveltime.plugin.solr.cache.RequestCache;
import com.traveltime.plugin.solr.fetcher.Fetcher;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.apache.lucene.search.IndexSearcher;
import org.apache.solr.search.DelegatingCollector;
import org.apache.solr.search.ExtendedQueryBase;
import org.apache.solr.search.PostFilter;
import org.apache.solr.search.SolrIndexSearcher;

@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class TraveltimeSearchQuery<Params extends QueryParams> extends ExtendedQueryBase implements PostFilter {
   private final Params params;
   private final float weight;
   private final Fetcher<Params> fetcher;
   private final String cacheName;

   @Override
   public String toString(String field) {
      return String.format("TraveltimeSearchQuery(params = %s)", params);
   }

   @Override
   public DelegatingCollector getFilterCollector(IndexSearcher indexSearcher) {
      SolrIndexSearcher searcher = (SolrIndexSearcher) indexSearcher;
      RequestCache<Params> cache = (RequestCache<Params>) searcher.getCache(cacheName);
      int maxDoc = searcher.maxDoc();
      int leafCount = searcher.getTopReaderContext().leaves().size();
      return new TraveltimeDelegatingCollector<>(maxDoc, leafCount, params, weight, fetcher, cache);
   }

   @Override
   public boolean getCache() {
      return false;
   }

   @Override
   public void setCache(boolean cache) {
   }

   @Override
   public int getCost() {
      return 100;
   }

   @Override
   public void setCost(int cost) {
   }

   @Override
   public boolean getCacheSep() {
      return false;
   }

   @Override
   public void setCacheSep(boolean cacheSep) {
   }
}
