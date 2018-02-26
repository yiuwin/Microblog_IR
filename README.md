# Microblog_IR
Implement an Information Retrieval System to index a collection of documents extracted from the TREC Microblog Retrieval Task in 2011

The IR system is implemented using the Apache Lucene open-source information retrieval software library in Java. The system takes in the following files as input: the TREC tweet corpus file (Trec_microblog11.txt), and the question topics ( topics_MB1-49.txt). All files are located in the file path "resources/...". Two source files are required for the execution of the program: Microblog_IR.java and FileWriter.java. The program outputs a retrieval and ranking results file (Result.txt) in the TREC format and, optionally, a sample vocabulary file (SampleVocab.txt). 


**To run the Java solution:**

-	Ensure that Java (Eclipse) is installed
-	In Eclipse, add external JARs from "lucene/..." to Java Build Path
-	Compile Microblog_IR.java and FileWriter.java
-	Run Microblog_IR.java

**Evaluation**

TRECEVAL is a standard program used to evaluate the rankings of documents by taking in a query relevance file and a results file with the output of your IR program. Download trec_eval scipt from http://trec.nist.gov/trec_eval/

To Run TRECEVAL:
1. Run $make in the source directory.
2. From the directory containing the trec_eval executable, run the following command in Terminal:

$ ./trec_eval -a Trec_microblog11.txt Results.txt > evaluation.txt


The feedback file retrieved 2640 relevant results and our system got 2214 of those, so we are getting 83% of results. Overall our MAP is 0.2508 and our P@10 is 0.3673. 
