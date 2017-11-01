package org.apache.lucene.queries;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Weight;

import java.io.IOException;
import java.util.stream.Stream;

public class SeqSpanQuery extends Query {

  private Term startTerm;
  private Term endTerm;
  private Term[] seqTerms;
  private int maxSpan;

  public SeqSpanQuery(String field, String startTermStr, String endTermStr, String[] seqTermStrs, int maxSpan) {

    if (null == startTermStr || startTermStr.trim().isEmpty()) {
      throw new WrongArgException("We need startTermStr");
    }

    if (null == endTermStr || endTermStr.trim().isEmpty()) {
      throw new WrongArgException("We need endTermStr");
    }

    if (null == seqTermStrs || 0 == seqTermStrs.length) {
      throw new WrongArgException("We need seqTermStrs");
    }

    this.startTerm = new Term(field, startTermStr.trim());
    this.endTerm = new Term(field, endTermStr.trim());
    this.seqTerms = Stream.of(seqTermStrs).filter(x -> null != x)
        .filter(x -> !x.isEmpty())
        .map(x -> new Term(field, x.trim()))
        .toArray(t -> new Term[t]);
  }

  @Override
  public Weight createWeight(IndexSearcher searcher, boolean needsScores) throws IOException {
    return super.createWeight(searcher, needsScores);
  }

  @Override
  public Query rewrite(IndexReader reader) throws IOException {
    return super.rewrite(reader);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }

  @Override
  public String toString(String field) {
    return "SeqSpanQuery: " + "";
  }
}