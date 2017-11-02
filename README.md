# lucene-custom-query
a b seq/x c seq/x d seq/x e f

| 分支      | tag        | elasticsearch版本 | Release Link                                                                                  |
| ---       | ---        | ---               | ---                                                                                           |
| 0.1       | tag v0.1 | v0.1            | Download: [v0.1](https://github.com/sing1ee/lucene-custom-query/releases/tag/v0.1) |

```java

try {


            IndexWriterConfig conf = new IndexWriterConfig(new StandardAnalyzer());

            Directory dir = FSDirectory.open(new File("/Users/zhangcheng/Downloads/idx").toPath());

            IndexWriter writer = new IndexWriter(dir, conf);

            {
                Document doc = new Document();
                doc.add(new TextField("field", "b c d e f", Field.Store.YES));
                writer.addDocument(doc);
            }
            {
                Document doc = new Document();
                doc.add(new TextField("field", "x b c d x e f", Field.Store.YES));
                writer.addDocument(doc);
            }
            writer.commit();

            IndexReader reader = DirectoryReader.open(writer);

            IndexSearcher searcher = new IndexSearcher(reader);

            SeqSpanQuery ssQuery = new SeqSpanQuery("field", "b", "f", new String[]{"c", "d", "e"}, 3);

            TopScoreDocCollector collector = TopScoreDocCollector.create(10);

            searcher.search(ssQuery, collector);

            Stream.of(collector.topDocs().scoreDocs).forEach(x -> {
                System.out.println(x.doc + " : " + x.score);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

```
