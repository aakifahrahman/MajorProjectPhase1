package phases;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import opennlp.tools.cmdline.postag.POSModelLoader;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.InvalidFormatException;

public class PhaseOne {
	
	private static Map<String, Double> dictionary; //final dictionary created from SentiWordNet of the form word#pos --> weighted average score
	
	public static void main(String[] args) {
        try {
        	
        	dictionary = new HashMap<String, Double>(); //initialise an empty hashmap
    	
    		SentiCode("senti_word_net.txt"); //populate the dictionary from SentiWordNet
    		    	 		
    		Scanner in = new Scanner(System.in); 
    		String bookName;
    		
    	    System.out.println("Enter the Book Name : ");
    	    bookName = in.nextLine(); //enter a book name
    		
			reviewABook(bookName); //call function to review a Book
			
		} catch (InvalidFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
	
	//Function to split a review in a text file into sentences
	
	public static void reviewABook(String bookName) throws InvalidFormatException,
	IOException {
	    
	    ArrayList<File> reviews = new ArrayList<File>();
	    listf(bookName, reviews); //return all files ie. reviews in the directory with the same name as the book
		
	    double bookScore = 0.0;
	    
	    for(File bookReview : reviews){ //for each review
	    
	    	FileInputStream fin = new FileInputStream(bookReview); //open a file stream
	    	byte[] buffer = new byte[(int) bookReview.length()];
	    	new DataInputStream(fin).readFully(buffer); //read the file into a buffer string
	    	fin.close();
	    	String paragraph = new String(buffer, "UTF-8");

	    	//start with the opennlp trained model, a model is learned from training data
	    	InputStream is = new FileInputStream("en-sent.bin");
	    	SentenceModel model = new SentenceModel(is);
	    	SentenceDetectorME sdetector = new SentenceDetectorME(model);

	    	String sentences[] = sdetector.sentDetect(paragraph); //detect all sentences in the review file

	    	int noOfSentences = sentences.length;
	    	int i=0;
		
	    	//model to split each sentence into tokens
		
	    	InputStream is2 = new FileInputStream("en-token.bin");
	    	TokenizerModel model2 = new TokenizerModel(is2);
	    	Tokenizer tokenizer = new TokenizerME(model2);
		
	    	//model to tag each token with pos tags
	    	
	    	POSModel model3 = new POSModelLoader().load(new File("en-pos-maxent.bin"));
	    	POSTaggerME tagger = new POSTaggerME(model3);

	    	//initialize score of the review file as 0
	    	double reviewScore = 0.0;
	    	
	    	//loop through all the sentences
	    	while(i <noOfSentences){
			
	    		String tokens[] = tokenizer.tokenize(sentences[i]); //tokenize each sentence
	    		String[] tags = tagger.tag(tokens); //assign pos tag to each token
		
	    		for(int k = 0 ; k < tags.length; k++){ //code to reduce assigned pos into the 4 basic pos present in SentiWordNet 
	    			if (tags[k].charAt(0) == 'J')
	    				tags[k] = "j" ;
	    			else if (tags[k].charAt(0) == 'V')
	    				tags[k] = "v" ;
	    			else if (tags[k].charAt(0) == 'N')
	    				tags[k] = "n" ;
	    			else if (tags[k].charAt(0) == 'R')
	    				tags[k] = "r" ;
	    			else
	    				tags[k] = " " ;
	    		}
			
	    		for (int j = 0; j< tokens.length ; j++){
	    			reviewScore += extract(tokens[j], tags[j]); //call extract function on each token tag pair, sum up the scores returned
	    		}
	    				 			
	    		i++; //iterate to next sentence
	    	}
	    	
	    	bookScore += reviewScore; //sum over all the review scores to get bookscore
	    	is.close();
	    }
	    
	    if((int)bookScore > 0)	//determine overall polarity for the book based on all reviews		
	    	System.out.println("The book is well worth a read :) !");
	    else			
	    	System.out.println("The book is not worth a read :("); 
	    	
	}


	public static void SentiCode(String pathToSWN) throws IOException {
		
		// hashmap from String to list of integer double pairs
		HashMap<String, HashMap<Integer, Double>> tempDictionary = new HashMap<String, HashMap<Integer, Double>>();

		BufferedReader csv = null;
		try {
			csv = new BufferedReader(new FileReader(pathToSWN)); //read from the txt file
			int lineNumber = 0;

			String line;
			while ((line = csv.readLine()) != null) {
				lineNumber++;

				// id the line is a comment, skip this line
				if (!line.trim().startsWith("#")) {
					// use tab separation to split the line into array of strings
					String[] data = line.split("\t");
					String wordTypeMarker = data[0]; //pos

					// throw an exception for invalid lines
					if (data.length != 6) {
						throw new IllegalArgumentException("Incorrect tabulation format in file, line: " + lineNumber);
					}

					// calculate synset score as score = PosS - NegS
					Double synsetScore = Double.parseDouble(data[2]) - Double.parseDouble(data[3]);

					// get all synset terms in a line
					String[] synTermsSplit = data[4].split(" ");

					// go through all terms of current synset.
					for (String synTermSplit : synTermsSplit) {
						// get synterm and synterm rank
						String[] synTermAndRank = synTermSplit.split("#");
						String synTerm = synTermAndRank[0] + "#" + wordTypeMarker;

						int synTermRank = Integer.parseInt(synTermAndRank[1]);

						// add term to map if it doesn't have one
						if (!tempDictionary.containsKey(synTerm)) {
							tempDictionary.put(synTerm, new HashMap<Integer, Double>());
						}

						// add synset link to synterm
						tempDictionary.get(synTerm).put(synTermRank, synsetScore);
					}
				}
			}

			// go through all the terms
			for (Map.Entry<String, HashMap<Integer, Double>> entry : tempDictionary.entrySet()) {
				String word = entry.getKey();
				Map<Integer, Double> synSetScoreMap = entry.getValue();

				// calculate weighted average - weigh the synsets according to their rank
				double score = 0.0;
				double sum = 0.0;
				for (Map.Entry<Integer, Double> setScore : synSetScoreMap.entrySet()) {
					score += setScore.getValue() / (double) setScore.getKey();
					sum += 1.0 / (double) setScore.getKey();
				}
				score /= sum;

				dictionary.put(word, score); //populate final dictionary
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (csv != null) {
				csv.close();
			}
		}
	}

	//function to get score of word from dictionary
	public static double extract(String word, String pos) {
		if(dictionary.get(word + "#" + pos) != null)
			return dictionary.get(word + "#" + pos);
		else
			return 0.0;
	}
	
	//function to list all files in a given directory
	public static void listf(String directoryName, ArrayList<File> files) {
	    File directory = new File(directoryName);

	    // get all the files from a directory
	    File[] fList = directory.listFiles();
	    for (File file : fList) {
	        if (file.isFile()) {
	            files.add(file);
	        } else if (file.isDirectory()) {
	            listf(file.getAbsolutePath(), files);
	        }
	    }
	}
	
}