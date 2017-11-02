package org.apache.lucene.queries;

import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;

import java.util.Arrays;

final class PostingsAndFreq implements Comparable<PostingsAndFreq> {

  final PostingsEnum postings;
  final int position;
  final Term[] terms;
  final int nTerms; // for faster comparisons

  public PostingsAndFreq(PostingsEnum postings, int position, Term... terms) {
    this.postings = postings;
    this.position = position;
    nTerms = terms==null ? 0 : terms.length;
    if (nTerms>0) {
      if (terms.length==1) {
        this.terms = terms;
      } else {
        Term[] terms2 = new Term[terms.length];
        System.arraycopy(terms, 0, terms2, 0, terms.length);
        Arrays.sort(terms2);
        this.terms = terms2;
      }
    } else {
      this.terms = null;
    }
  }

  @Override
  public int compareTo(PostingsAndFreq other) {
    if (position != other.position) {
      return position - other.position;
    }
    if (nTerms != other.nTerms) {
      return nTerms - other.nTerms;
    }
    if (nTerms == 0) {
      return 0;
    }
    for (int i=0; i<terms.length; i++) {
      int res = terms[i].compareTo(other.terms[i]);
      if (res!=0) return res;
    }
    return 0;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + position;
    for (int i=0; i<nTerms; i++) {
      result = prime * result + terms[i].hashCode();
    }
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    PostingsAndFreq other = (PostingsAndFreq) obj;
    if (position != other.position) return false;
    if (terms == null) return other.terms == null;
    return Arrays.equals(terms, other.terms);
  }
}