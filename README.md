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


# ES插件使用

### 兼容版本
- lucene 6.6.0
- elasticsearch 5.5.1

### 打包插件

```shell

./gradlew clean build pz
# build/distributions/lucene-custom-query.zip
```

### 使用插件

1. 创建索引：POST http://localhost:9200/index
2. 配置索引：POST http://localhost:9200/index/fulltext/_mapping
```shell
    
    # body
    {
        "fulltext": {
                 "_all": {
                "analyzer": "standard",
                "search_analyzer": "standard",
                "term_vector": "no",
                "store": "false"
            },
            "properties": {
                "content": {
                    "type": "text",
                    "store": "no",
                    "term_vector": "with_positions_offsets",
                    "analyzer": "standard",
                    "search_analyzer": "standard",
                    "include_in_all": "true",
                    "boost": 8
                }
            }
        }
    }
```
3. 创建文档 POST http://localhost:9200/index/fulltext/<docId>
```shell
    
    # http://localhost:9200/index/fulltext/1
    {"content":"a b c d e f"}
    # http://localhost:9200/index/fulltext/2
    {"content":"b c d e f g"}
```
4. 查询 POST http://localhost:9200/index/fulltext/_search
```shell
    {
      "query": {
        "seq_span": {
          "field": "content",
          "start_term": "b",
          "end_term": "g",
          "seq_term": "c d e f",
          "max_span": 8
        }
      }
    }
```
   Result:
```shell
   {
       "took": 101,
       "timed_out": false,
       "_shards": {
           "total": 5,
           "successful": 5,
           "failed": 0
       },
       "hits": {
           "total": 1,
           "max_score": 0,
           "hits": [
               {
                   "_index": "index",
                   "_type": "fulltext",
                   "_id": "2",
                   "_score": 0,
                   "_source": {
                       "content": "b c d e f g"
                   }
               }
           ]
       }
   }
```