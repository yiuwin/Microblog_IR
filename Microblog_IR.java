import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import java.text.DecimalFormat;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class Preprocessing {

	/* FINAL FILE PATHS */
	private final static String INPUT_FILE = "resources/Trec_microblog11.txt";
	private final static String QUERY_FILE = "resources/topics_MB1-49.txt";
	private final static String RESULTS_FILE = "output/Results.txt";
	private final static String VOCAB_FILE = "output/SampleVocab.txt";
	
	
	/* Lucene Object Initializations */
	static StandardAnalyzer analyzer;
	static Directory index;
	static IndexWriterConfig config;
	static IndexWriter writer;
	static IndexReader reader;

	
	/* Create new Document in index collection
	 * 
	 * @param IndexWriter writer 	lucene object that puts together the inverted index
	 * @param String tweetid		document field representing tweet id
	 * @param String message		document field representing the tweet message
	 */	
	private static void addDoc(IndexWriter writer, String tweetid, String message) throws IOException {
	    Document doc = new Document();
        doc.add(new StoredField("tweetid", tweetid));
	    doc.add(new TextField("message", message, Field.Store.YES));
	    writer.addDocument(doc);
	}
	
	/* Initializes StandardAnalyzer and IndexWriter used for writing and assembling the inverted index.
	 * The resulting index is stored in RAM
	 */
	/* Initialize Default Variables */
	private static void initialize()  throws IOException, ParseException  {
		
	    analyzer = new StandardAnalyzer(); // Builds an analyzer with the default stop words (ENGLISH_STOP_WORDS_SET).
		index = new RAMDirectory(); // RAM index storage
		
	    /* Alternatively, comment the previous line and uncomment the next two lines to store the index in Disk */
	    //File f = new File("results.txt");
		//Directory index = FSDirectory.open(f.toPath());
	    
	    config = new IndexWriterConfig(analyzer);
	    writer = new IndexWriter(index, config);

	}
	
	/* Read and parse input file and save the document to RAM by calling function addDoc(IndexWriter, String, String)
	 * 
	 * @param String filename	Input file name containing collection of documents (Twitter messages)
	 */
	private static void buildInputIndex(String filename) {
		try{
            BufferedReader buf = new BufferedReader(new FileReader(filename));
            String lineJustFetched = null;
            String[] fields;

            while(true){
                lineJustFetched = buf.readLine();              
                if(lineJustFetched == null){  // end of file
                    break; 
                }else{
                    fields = lineJustFetched.split("\t");
					addDoc(writer, fields[0], fields[1]);
                }
            }

            buf.close();
            writer.close();

        }catch(Exception e){
            e.printStackTrace();
        }
	}

	/* Save a sample of 100 terms from the inverted index into a new file
	 * 
	 * @param String filename	Output sample vocabulary file
	 */
	private static void sampleVocabulary(String filename) throws IOException {

		reader = null; // clear IndexReader
		
		try {
			reader = DirectoryReader.open(index);
		} catch (IOException e) {
			System.out.println("Cannot open index");
			e.printStackTrace();
		}
		
		try {
			Fields fields = MultiFields.getFields(reader);
			Terms terms = fields.terms("message");
			TermsEnum it = terms.iterator(); // iterates through the vocabulary in the index
			FileWriter fw = new FileWriter(filename);
			fw.write("Total Number of Terms: " + terms.size());
			for (int i=0; i<100; i++){
				fw.write(it.next().utf8ToString()); 
			}

			fw.close();
			
		} catch (IOException e) {
			System.out.println("Error writing to file: " + filename);
			e.printStackTrace();
		}
		
		reader.close();
		
		
	}
	
	/* Helper function for outputResults(String, LinkedHashMap<String,String>) that will search a query title
	 * and write it to the results file in the correct TREC format
	 * 
	 * @param FileWriter writer		Used to output the result of the query search to the Results.txt file
	 * @param String num			"topic_id" to be printed in Results.txt
	 * @param String title			Test query title
	 */	
	private static void search(FileWriter writer, String num, String title, int q_counter) throws ParseException, IOException {
		
	    int hitsPerPage = 1000;
	    
	    Query q = new QueryParser("message", analyzer).parse(title);

	    IndexReader reader = DirectoryReader.open(index);
		IndexSearcher searcher = new IndexSearcher(reader);
		
	    TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage);
	    searcher.search(q, collector);
	    ScoreDoc[] hits = collector.topDocs().scoreDocs;
	    
	    for(int i=0;i<hits.length;++i) {
	        int docId = hits[i].doc;
	        Document d = searcher.doc(docId);
	        
	        DecimalFormat df = new DecimalFormat();
			df.setMaximumFractionDigits(3);
	        
	        writer.write(q_counter + " Q0 " + d.get("tweetid") + " " + (i+1) + " " + df.format(hits[i].score/hits[0].score) + " " + "myRun");
	        
	    }
	    
		reader.close();
	}
	
	/* Read and parse an input file containing the test queries in XML format
	 * 
	 * @param String filename					File name of input file containing test queries
	 * @return LinkedHashMap<String,String>		Linked Hashmap containing the topic_id and title of each test query
	 * */
	private static LinkedHashMap<String,String> parseInput(String filepath) {

		LinkedHashMap<String, String> queries = new LinkedHashMap<String, String>();
		
	    try {
	
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser saxParser = factory.newSAXParser();
		
		// add root to the malformed XML file
		File file = new File(filepath); // open the file passed from parameter
		FileInputStream fis = new FileInputStream(file);
		List<InputStream> streams = 
			    Arrays.asList(
			        new ByteArrayInputStream("<root>".getBytes()),
			    fis,
			    new ByteArrayInputStream("</root>".getBytes()));
		InputStream cntr = 
				new SequenceInputStream(Collections.enumeration(streams));
		// end add root
		
	
		DefaultHandler handler = new DefaultHandler() {

				String tmpNum, tmpTitle, tmpQTime,tmpTweetTime;
				boolean bnum = false;
				boolean btitle = false;
				boolean bqtime = false;
				boolean btweet = false;
				
				
				public void startElement(String uri, String localName,String qName,
			                Attributes attributes) throws SAXException {
					if (qName.equalsIgnoreCase("num")) {
						bnum = true;
					}
					if (qName.equalsIgnoreCase("title")) {
						btitle = true;
					}
					if (qName.equalsIgnoreCase("querytime")) {
						bqtime = true;
					}
					if (qName.equalsIgnoreCase("querytweettime")) {
						btweet = true;
					}
				}
			
				public void endElement(String uri, String localName,
					String qName) throws SAXException {

					if (qName.equalsIgnoreCase("Top")) {
						if (tmpNum != null && tmpTitle != null && tmpQTime != null && tmpTweetTime != null){
							queries.put(tmpNum, tmpTitle);
						}
					}
			
				}
			
				public void characters(char ch[], int start, int length) throws SAXException {
					
					if (bnum) {
						tmpNum = new String(ch, start, length);
						tmpNum = tmpNum.substring(9);
						bnum = false;
					}
			
					if (btitle) {
						tmpTitle = new String(ch, start, length);
						tmpTitle = tmpTitle.substring(1);
						btitle = false;
					}
			
					if (bqtime) {
						tmpQTime = new String(ch, start, length);
						tmpQTime = tmpQTime.substring(1);
						bqtime = false;
					}
			
					if (btweet) {
						tmpTweetTime = new String(ch, start, length);
						tmpTweetTime = tmpTweetTime.substring(1);
						btweet = false;
					}
				}
	
	     		};
	     	saxParser.parse(cntr, handler);
	
	    } // end try 
	    catch (Exception e) {
	       e.printStackTrace();
	    }
	    
	    return queries;
	    
	}
	
	/* Creates output results file by calling search(FileWriter writer, String, String) on each test query
	 * 
	 * @param String filename					The desired name of the output file
	 * @param LinkedHashMap<String, String>		The collection of test queries returned from parseInput(String)
	 * */
	private static void outputResults(String filename, LinkedHashMap<String, String> queries) throws ParseException, IOException {
		
		FileWriter fw = new FileWriter(filename);
	 	
	    Set set = queries.entrySet(); // Get a set of the entries

	      // Get an iterator
	    Iterator i = set.iterator();
	    
	    int q_counter = 1;
	      // Display elements
	      while(i.hasNext()) {
	         Map.Entry me = (Map.Entry)i.next();
	         
	         search(fw, String.valueOf(me.getKey()), String.valueOf(me.getValue()), q_counter);
	         q_counter++;
	      }
	      System.out.println();
	      
	    fw.close();
	}
	
	/* Prints a limited set of documents that contain query word "query" to the console
	 * 
	 * @param String query	The sample query string used to fetch documents that contain at least one instance of the query
	 * @param numHits		Number of documents to be returned as the limited set of results
	 * */
	private static void retrievalAndRanking(String query, int numHits) throws IOException, ParseException {
		Query q = new QueryParser("message", analyzer).parse(query);

	    IndexReader reader = DirectoryReader.open(index);
		IndexSearcher searcher = new IndexSearcher(reader);
		TopScoreDocCollector collector = TopScoreDocCollector.create(numHits);
	    searcher.search(q, collector);
	    ScoreDoc[] hits = collector.topDocs().scoreDocs;
	    
		System.out.println( "Score\tTweet ID\t\tMessage" );
		System.out.println( "----------------------------------------------------------------------------------" );
	    
	    for(int i=0;i<hits.length;++i) {
	        int docId = hits[i].doc;
	        Document d = searcher.doc(docId);
	        
	        DecimalFormat df = new DecimalFormat();
			df.setMaximumFractionDigits(3);
	        
			System.out.println(df.format(hits[i].score/hits[0].score) + "\t" + d.get("tweetid") + "\t" + d.get("message") );
	        
	    }
	    
		reader.close();
	}
	
	public static void main(String argv[]) throws IOException, ParseException {
		   
			LinkedHashMap<String, String> queries; // store test queries returned by parseInput(filename)
			
			/* Preprocessing and Indexing */
		 	initialize();
		 	buildInputIndex(INPUT_FILE);
			
		 	/* Retrieval and Ranking */
		 	retrievalAndRanking("bbc", 10); // outputs limited set of documents that contain query word "bbc" to the console
		 	
		 	/* Results */
		 	queries = parseInput(QUERY_FILE);
		 	outputResults(RESULTS_FILE, queries);
		 	
		 	/* Optionally, generate file with 100 sample terms from inverted index */
		 	sampleVocabulary(VOCAB_FILE); 
		 	
	 }
	
}
