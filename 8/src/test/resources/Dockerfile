FROM solr:8.11


COPY build/libs/*.jar /var/solr/data/lib/
COPY src/test/resources/part0.json ./
COPY src/test/resources/mock-proto-server ./
