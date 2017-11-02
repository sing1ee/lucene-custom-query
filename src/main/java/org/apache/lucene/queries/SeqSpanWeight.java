package org.apache.lucene.queries;

import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.index.TermState;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.similarities.Similarity;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

public class SeqSpanWeight extends Weight{

  private final Similarity similarity;
  private final Similarity.SimWeight stats;
  private final boolean needsScores = true;
  private transient TermContext states[];
  private final Term[] terms;
  private final int[] positions;
  private final SeqSpanQuery selfQuery;
  private final String field;

  protected SeqSpanWeight(SeqSpanQuery query, IndexSearcher searcher) throws IOException {
    super(query);
    this.selfQuery = query;
    this.similarity = searcher.getSimilarity(needsScores);
    this.positions = selfQuery.getPositions();
    this.terms = selfQuery.getTerms();
    this.field = terms[0].field();
    if (positions.length < 2) {
      throw new IllegalStateException("PhraseWeight does not support less than 2 terms, call rewrite first");
    } else if (positions[0] != 0) {
      throw new IllegalStateException("PhraseWeight requires that the first position is 0, call rewrite first");
    }
    final IndexReaderContext context = searcher.getTopReaderContext();
    states = new TermContext[terms.length];
    TermStatistics termStats[] = new TermStatistics[terms.length];
    for (int i = 0; i < terms.length; i++) {
      final Term term = terms[i];
      states[i] = TermContext.build(context, term);
      termStats[i] = searcher.termStatistics(term, states[i]);
    }
    stats = similarity.computeWeight(searcher.collectionStatistics(terms[0].field()), termStats);
  }

  @Override
  public void extractTerms(Set<Term> terms) {
    Collections.addAll(terms, this.terms);
  }

  @Override
  public Explanation explain(LeafReaderContext context, int doc) throws IOException {
    return  Explanation.noMatch("future");
  }

  @Override
  public float getValueForNormalization() throws IOException {
    return stats.getValueForNormalization();
  }

  @Override
  public void normalize(float norm, float boost) {
    stats.normalize(norm, boost);
  }

  @Override
  public Scorer scorer(LeafReaderContext context) throws IOException {
    assert terms.length > 0;
    final LeafReader reader = context.reader();
    PostingsAndFreq[] postingsFreqs = new PostingsAndFreq[terms.length];

    final Terms fieldTerms = reader.terms(field);
    if (fieldTerms == null) {
      return null;
    }

    if (fieldTerms.hasPositions() == false) {
      throw new IllegalStateException("field \"" + terms[0].field() + "\" was indexed without position data; cannot run "
          + "SeqSpanQuery (phrase=" + getQuery() + ")");
    }

    // Reuse single TermsEnum below:
    final TermsEnum te = fieldTerms.iterator();
    float totalMatchCost = 0;

    for (int i = 0; i < terms.length; i++) {
      final Term t = terms[i];
      final TermState state = states[i].get(context.ord);
      if (state == null) { /* term doesnt exist in this segment */
        assert reader.docFreq(t) == 0 : "no termstate found but term exists in reader";
        return null;
      }
      te.seekExact(t.bytes(), state);
      PostingsEnum postingsEnum = te.postings(null, PostingsEnum.POSITIONS);
      postingsFreqs[i] = new PostingsAndFreq(postingsEnum, positions[i], t);
//      totalMatchCost += termPositionsCost(te);
    }
    return new SeqSpanScorer(this, postingsFreqs, similarity.simScorer(stats, context),
        needsScores, totalMatchCost);
  }

  public Term[] getTerms() {
    return selfQuery.getTerms();
  }

  public int getMaxSpan() {
    return selfQuery.getMaxSpan();
  }
}
