package org.apache.lucene.queries.plugins;

import org.elasticsearch.common.ParsingException;
import org.elasticsearch.common.xcontent.XContentLocation;
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
    String fieldName = null;
    XContentParser.Token token = null;
    while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
      if (token == XContentParser.Token.FIELD_NAME) {
        currentFieldName = parser.currentName();
      } else if (parseContext.isDeprecatedSetting(currentFieldName)) {
        // skip
      } else if (token == XContentParser.Token.START_OBJECT) {
        throwParsingExceptionOnMultipleFields(SeqSpanQueryBuilder.NAME, parser.getTokenLocation(), fieldName, currentFieldName);
        fieldName = currentFieldName;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {

          if (token == XContentParser.Token.FIELD_NAME) {
            currentFieldName = parser.currentName();
          } else if (token.isValue()) {

            if (SeqSpanQueryBuilder.START_TERM_FIELD.match(currentFieldName)) {
              builder.setStartTerm(parser.text());
            } else if (SeqSpanQueryBuilder.END_TERM_FIELD.match(currentFieldName)) {
              builder.setEndTerm(parser.text());
            } else if (SeqSpanQueryBuilder.SEQ_TERM_FIELD.match(currentFieldName)) {
              builder.setSeqTerm(parser.list().toArray(new String[0]));
            } else if (SeqSpanQueryBuilder.MAX_SPAN_FIELD.match(currentFieldName)) {
              builder.setMaxSpan(parser.intValue());
            } else {
              throw new ParsingException(parser.getTokenLocation(),
                  "[" + SeqSpanQueryBuilder.NAME + "] unknown token [" + token + "] after [" + currentFieldName + "]");
            }
          } else {
            throw new ParsingException(parser.getTokenLocation(),
                "[" + SeqSpanQueryBuilder.NAME + "] unknown token [" + token + "] after [" + currentFieldName + "]");
          }
        }
      } else {
        throwParsingExceptionOnMultipleFields(SeqSpanQueryBuilder.NAME, parser.getTokenLocation(), fieldName, currentFieldName);
        fieldName = currentFieldName;
      }
    }
    builder.setFieldName(fieldName);
    return Optional.of(builder);
  }

  private static void throwParsingExceptionOnMultipleFields(String queryName, XContentLocation contentLocation,
                                                              String processedFieldName, String currentFieldName) {
    if (processedFieldName != null) {
      throw new ParsingException(contentLocation, "[" + queryName + "] query doesn't support multiple fields, found ["
          + processedFieldName + "] and [" + currentFieldName + "]");
    }
  }
}
