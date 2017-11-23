package org.apache.lucene.queries.plugins;

import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.query.QueryParseContext;
import org.elasticsearch.index.query.QueryParser;

import java.io.IOException;
import java.util.Optional;

public class SeqSpanQueryParser implements QueryParser<SeqSpanQueryBuilder>{


  @Override
  public Optional<SeqSpanQueryBuilder> fromXContent(QueryParseContext parseContext) throws IOException {
    XContentParser parser = parseContext.parser();
    SeqSpanQueryBuilder builder = new SeqSpanQueryBuilder();

    String currentFieldName = null;
    XContentParser.Token token = null;
    while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
      if (token == XContentParser.Token.FIELD_NAME) {
        currentFieldName = parser.currentName();
      } else if (parseContext.isDeprecatedSetting(currentFieldName)) {
        // skip
      } else {

      }
    }
    return Optional.of(builder);
  }
}
