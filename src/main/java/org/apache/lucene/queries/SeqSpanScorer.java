package org.apache.lucene.queries;

import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.search.ConjunctionDISI;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TwoPhaseIterator;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.similarities.Similarity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

  SeqSpanScorer(Weight weight, PostingsAndFreq[] postings,
                    Similarity.SimScorer docScorer, boolean needsScores,
                    float matchCost) throws IOException {
    super(weight);
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
        return phraseFreq() > 0;
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

  private int phraseFreq() throws IOException {
    // reset state
    final PostingsAndPosition[] postings = this.postings;
    for (PostingsAndPosition posting : postings) {
      posting.freq = posting.postings.freq();
      posting.pos = posting.postings.nextPosition();
      posting.upTo = 1;
    }

    int freq = 0;
    final PostingsAndPosition lead = postings[0];

    advanceHead:
    while (true) {
      final int phrasePos = lead.pos - lead.offset;
      for (int j = 1; j < postings.length; ++j) {
        final PostingsAndPosition posting = postings[j];
        final int expectedPos = phrasePos + posting.offset;

        // advance up to the same position as the lead
        if (advancePosition(posting, expectedPos) == false) {
          break advanceHead;
        }

        if (posting.pos != expectedPos) { // we advanced too far
          if (advancePosition(lead, posting.pos - posting.offset + lead.offset)) {
            continue advanceHead;
          } else {
            break advanceHead;
          }
        }
      }

      freq += 1;
      if (needsScores == false) {
        break;
      }

      if (lead.upTo == lead.freq) {
        break;
      }
      lead.pos = lead.postings.nextPosition();
      lead.upTo += 1;
    }

    return this.freq = freq;
  }
}
