package org.apache.lucene.queries.plugins;

import org.apache.lucene.queries.SeqSpanQuery;
import org.apache.lucene.search.Query;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.AbstractQueryBuilder;
import org.elasticsearch.index.query.QueryShardContext;

import java.io.IOException;
import java.util.Objects;

public class SeqSpanQueryBuilder extends AbstractQueryBuilder<SeqSpanQueryBuilder> {

  protected static final String NAME = "seq_span";

  protected static final ParseField START_TERM_FIELD = new ParseField("start_term");
  protected static final ParseField SEQ_TERM_FIELD = new ParseField("seq_term");
  protected static final ParseField END_TERM_FIELD = new ParseField("end_term");
  protected static final ParseField MAX_SPAN_FIELD = new ParseField("max_span");

  private String fieldName;
  private String startTerm;
  private String[] seqTerm;
  private String endTerm;
  private int maxSpan;

  public SeqSpanQueryBuilder() {
  }

  public SeqSpanQueryBuilder(StreamInput in) throws IOException {

    fieldName = in.readString();
    startTerm = in.readString();
    seqTerm = in.readStringArray();
    endTerm = in.readString();
    maxSpan = in.readInt();
  }

  @Override
  protected void doWriteTo(StreamOutput out) throws IOException {

    out.writeString(fieldName);
    out.writeString(startTerm);
    out.writeStringArray(seqTerm);
    out.writeString(endTerm);
    out.writeInt(maxSpan);
  }

  @Override
  protected void doXContent(XContentBuilder builder, Params params) throws IOException {

    builder.startObject(NAME);
    builder.startObject(fieldName);
    builder.field(START_TERM_FIELD.getPreferredName(), startTerm);
    builder.array(SEQ_TERM_FIELD.getPreferredName(), seqTerm);
    builder.field(END_TERM_FIELD.getPreferredName(), endTerm);
    builder.field(MAX_SPAN_FIELD.getPreferredName(), maxSpan);
    builder.endObject();
    builder.endObject();
  }

  @Override
  protected Query doToQuery(QueryShardContext context) throws IOException {
    return new SeqSpanQuery(fieldName, startTerm, endTerm, seqTerm, maxSpan);
  }

  @Override
  protected boolean doEquals(SeqSpanQueryBuilder other) {
    if (!Objects.equals(fieldName, other.fieldName)) {
      return false;
    }
    if (!Objects.equals(startTerm, other.startTerm)) {
      return false;
    }
    if (!Objects.deepEquals(seqTerm, seqTerm)) {
      return false;
    }
    if (!Objects.equals(endTerm, endTerm)) {
      return false;
    }
    if (!Objects.equals(maxSpan, other.maxSpan)) {
      return false;
    }
    return true;
  }

  @Override
  protected int doHashCode() {
    return Objects.hash(fieldName, startTerm, seqTerm, endTerm, maxSpan);
  }

  @Override
  public String getWriteableName() {
    return NAME;
  }

  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }

  public void setStartTerm(String startTerm) {
    this.startTerm = startTerm;
  }

  public void setSeqTerm(String[] seqTerm) {
    this.seqTerm = seqTerm;
  }

  public void setEndTerm(String endTerm) {
    this.endTerm = endTerm;
  }

  public void setMaxSpan(int maxSpan) {
    this.maxSpan = maxSpan;
  }
}
