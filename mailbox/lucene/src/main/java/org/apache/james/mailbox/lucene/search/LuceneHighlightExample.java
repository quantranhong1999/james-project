/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.mailbox.lucene.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleFragmenter;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.TokenSources;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;

public class LuceneHighlightExample {

    public static void main(String[] args) throws IOException, ParseException, InvalidTokenOffsetsException {
        // Initialize an in-memory index using ByteBuffersDirectory
        Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter indexWriter = new IndexWriter(directory, config);

        // Index some documents
        indexDocument(indexWriter, "1", "Lucene is a powerful search library.", "Lucene is widely used for text search. Lucene can handle complex queries.");
        indexDocument(indexWriter, "2", "Lucene Search Features", "Searching and indexing are supported by Lucene.");
        indexWriter.close();

        // Search and highlight results
        String queryText = "Lucene";
        QueryParser parser = new QueryParser("body", analyzer); // Search in the body field
        Query query = parser.parse(queryText);

        DirectoryReader directoryReader = DirectoryReader.open(directory);
        IndexSearcher searcher = new IndexSearcher(directoryReader);
        TopDocs topDocs = searcher.search(query, 10);

        // Highlighter setup
        Formatter formatter = new SimpleHTMLFormatter("<mark>", "</mark>");
        QueryScorer scorer = new QueryScorer(query);
        Highlighter highlighter = new Highlighter(formatter, scorer);
        Fragmenter fragmenter = new SimpleFragmenter(255); // Fragment size
        highlighter.setTextFragmenter(fragmenter);

        // Create a list to store the search snippets
        List<SearchSnippet> searchSnippets = new ArrayList<>();

        // Display highlighted results for both body and subject fields
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            Document doc = searcher.doc(scoreDoc.doc);

            // Highlight the subject field
            String subject = doc.get("subject");
            TokenStream subjectTokenStream = TokenSources.getTokenStream(doc, "subject", analyzer);
            String highlightedSubject = highlighter.getBestFragment(subjectTokenStream, subject);

            // Highlight the body field
            String body = doc.get("body");
            TokenStream bodyTokenStream = TokenSources.getTokenStream(doc, "body", analyzer);
            String highlightedBody = highlighter.getBestFragment(bodyTokenStream, body);

            // Create a SearchSnippet object and add it to the list
            SearchSnippet snippet = new SearchSnippet(doc.get("id"), highlightedSubject, highlightedBody);
            searchSnippets.add(snippet);
        }

        // Print out the search snippets
        for (SearchSnippet snippet : searchSnippets) {
            System.out.println(snippet);
        }

        // Result:
        // SearchSnippet{id='1', highlightedSubject='<mark>Lucene</mark> is a powerful search library.', highlightedBody='<mark>Lucene</mark> is widely used for text search. <mark>Lucene</mark> can handle complex queries.'}
        // SearchSnippet{id='2', highlightedSubject='<mark>Lucene</mark> Search Features', highlightedBody='Searching and indexing are supported by <mark>Lucene</mark>.'}

        directoryReader.close();
    }

    private static void indexDocument(IndexWriter indexWriter, String id, String subject, String body) throws IOException {
        Document doc = new Document();
        doc.add(new StringField("id", id, Field.Store.YES));
        doc.add(new TextField("subject", subject, Field.Store.YES));
        doc.add(new TextField("body", body, Field.Store.YES));
        indexWriter.addDocument(doc);
    }
}



