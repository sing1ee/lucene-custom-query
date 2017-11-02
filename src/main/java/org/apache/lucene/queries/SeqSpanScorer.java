package org.apache.lucene.queries;

import com.google.common.collect.Maps;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.search.ConjunctionDISI;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TwoPhaseIterator;
import org.apache.lucene.search.similarities.Similarity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class SeqSpanScorer extends Scorer{


  private static class PostingsAndPosition {
    private final PostingsEnum postings;
    private final int offset;
    private int freq, upTo, pos;

    public PostingsAndPosition(PostingsEnum postings, int offset) {
      this.postings = postings;
      this.offset = offset;
    }
  }

  private final ConjunctionDISI conjunction;
  private final SeqSpanScorer.PostingsAndPosition[] postings;

  private int freq;

  private final Similarity.SimScorer docScorer;
  private final boolean needsScores;
  private float matchCost;

  private final SeqSpanWeight selfWeight;

  SeqSpanScorer(SeqSpanWeight weight, PostingsAndFreq[] postings,
                    Similarity.SimScorer docScorer, boolean needsScores,
                    float matchCost) throws IOException {
    super(weight);
    this.selfWeight = weight;
    this.docScorer = docScorer;
    this.needsScores = needsScores;

    List<DocIdSetIterator> iterators = new ArrayList<>();
    List<PostingsAndPosition> postingsAndPositions = new ArrayList<>();
    for(PostingsAndFreq posting : postings) {
      iterators.add(posting.postings);
      postingsAndPositions.add(new PostingsAndPosition(posting.postings, posting.position));
    }
    conjunction = ConjunctionDISI.intersectIterators(iterators);
    this.postings = postingsAndPositions.toArray(new PostingsAndPosition[postingsAndPositions.size()]);
    this.matchCost = matchCost;
  }

  @Override
  public TwoPhaseIterator twoPhaseIterator() {
    return new TwoPhaseIterator(conjunction) {
      @Override
      public boolean matches() throws IOException {
        return seqSpanFreq() > 0;
      }

      @Override
      public float matchCost() {
        return matchCost;
      }
    };
  }

  @Override
  public DocIdSetIterator iterator() {
    return TwoPhaseIterator.asDocIdSetIterator(twoPhaseIterator());
  }

  @Override
  public String toString() {
    return "ExactPhraseScorer(" + weight + ")";
  }

  @Override
  public int freq() {
    return freq;
  }

  @Override
  public int docID() {
    return conjunction.docID();
  }

  @Override
  public float score() {
    return docScorer.score(docID(), freq);
  }

  /** Advance the given pos enum to the first doc on or after {@code target}.
   *  Return {@code false} if the enum was exhausted before reaching
   *  {@code target} and {@code true} otherwise. */
  private static boolean advancePosition(PostingsAndPosition posting, int target) throws IOException {
    while (posting.pos < target) {
      if (posting.upTo == posting.freq) {
        return false;
      } else {
        posting.pos = posting.postings.nextPosition();
        posting.upTo += 1;
      }
    }
    return true;
  }

  private int seqSpanFreq() throws IOException {
    // reset state
    final PostingsAndPosition[] postings = this.postings;
    for (PostingsAndPosition posting : postings) {
      posting.freq = posting.postings.freq();
      posting.pos = posting.postings.nextPosition();
      posting.upTo = 1;
    }

    // order constraint
    final String[] seqTerms = Stream.of(selfWeight.getTerms()).map(x -> x.text()).toArray(t -> new String[t]);

    Map<String, PostingsAndPosition> idx = Maps.newHashMap();

    for (int i = 0; i < seqTerms.length; ++i) {

      idx.put(seqTerms[i], postings[i]);
    }

    final PostingsAndPosition lead = idx.get(seqTerms[0]);

    LOOP:
    while (true) {
      for (int i = 0; i < seqTerms.length - 1; ++i) {
        PostingsAndPosition before = idx.get(seqTerms[i]);
        PostingsAndPosition after = idx.get(seqTerms[i + 1]);
        boolean less = before.pos < after.pos;
        if (!less) {
          if (!advancePosition(after, before.pos - before.offset + after.offset)) {
            if (!advancePosition(lead, lead.pos)) {
              return 0;
            }
            continue LOOP;
          }
          continue LOOP;
        }
      }

      int span = idx.get(seqTerms[seqTerms.length - 2]).pos - idx.get(seqTerms[1]).pos;
      if (span < selfWeight.getMaxSpan()) {
        return 1;
      }
      break;
    }
    return 0;
  }
}
