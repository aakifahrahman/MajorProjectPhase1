package hello;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import opennlp.tools.cmdline.postag.POSModelLoader;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.InvalidFormatException;

public class HelloWorld {
	
	private static Map<String, Double> dictionary;
	
	public static void main(String[] args) {
        try {
        	
        	dictionary = new HashMap<String, Double>();
    	
    		SentiCode("senti_word_net.txt");
    		    		
			SentenceDetect();
			
		} catch (InvalidFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

	//Function to split a review in a text file into sentences
	
	public static void SentenceDetect() throws InvalidFormatException,
	IOException {
		
		File f = new File("review1.txt");
	    FileInputStream fin = new FileInputStream(f);
	    byte[] buffer = new byte[(int) f.length()];
	    new DataInputStream(fin).readFully(buffer);
	    fin.close();
	    String paragraph = new String(buffer, "UTF-8");

		// always start with a model, a model is learned from training data
		InputStream is = new FileInputStream("en-sent.bin");
		SentenceModel model = new SentenceModel(is);
		SentenceDetectorME sdetector = new SentenceDetectorME(model);

		String sentences[] = sdetector.sentDetect(paragraph);

		int noOfSentences = sentences.length;
		
		int i=0;
		
		//Splitting each sentence into tokens
		
		InputStream is2 = new FileInputStream("en-token.bin");
		 
		TokenizerModel model2 = new TokenizerModel(is2);
	 
		Tokenizer tokenizer = new TokenizerME(model2);
		
		POSModel model3 = new POSModelLoader().load(new File("en-pos-maxent.bin"));
		
		POSTaggerME tagger = new POSTaggerME(model3);

		
		while(i <noOfSentences){
			
			String tokens[] = tokenizer.tokenize(sentences[i]);
		 			
			String[] tags = tagger.tag(tokens);
		
			for(int k = 0 ; k < tags.length; k++){
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
				if(!tags[j].isEmpty())
				System.out.println(tokens[j] + " - " + tags[j]+ " - " + extract(tokens[j], tags[j]));
			}
		 			
			i++;
		}
		
		is.close();
	}


	public static void SentiCode(String pathToSWN) throws IOException {
		
		// This is our main dictionary representation
	

		// From String to list of doubles.
		HashMap<String, HashMap<Integer, Double>> tempDictionary = new HashMap<String, HashMap<Integer, Double>>();

		BufferedReader csv = null;
		try {
			csv = new BufferedReader(new FileReader(pathToSWN));
			int lineNumber = 0;

			String line;
			while ((line = csv.readLine()) != null) {
				lineNumber++;

				// If it's a comment, skip this line.
				if (!line.trim().startsWith("#")) {
					// We use tab separation
					String[] data = line.split("\t");
					String wordTypeMarker = data[0];

					// Example line:
					// POS ID PosS NegS SynsetTerm#sensenumber Desc
					// a 00009618 0.5 0.25 spartan#4 austere#3 ascetical#2
					// ascetic#2 practicing great self-denial;...etc

					// Is it a valid line? Otherwise, through exception.
					if (data.length != 6) {
						throw new IllegalArgumentException(
								"Incorrect tabulation format in file, line: "
										+ lineNumber);
					}

					// Calculate synset score as score = PosS - NegS
					Double synsetScore = Double.parseDouble(data[2])
							- Double.parseDouble(data[3]);

					// Get all Synset terms
					String[] synTermsSplit = data[4].split(" ");

					// Go through all terms of current synset.
					for (String synTermSplit : synTermsSplit) {
						// Get synterm and synterm rank
						String[] synTermAndRank = synTermSplit.split("#");
						String synTerm = synTermAndRank[0] + "#"
								+ wordTypeMarker;

						int synTermRank = Integer.parseInt(synTermAndRank[1]);
						// What we get here is a map of the type:
						// term -> {score of synset#1, score of synset#2...}

						// Add map to term if it doesn't have one
						if (!tempDictionary.containsKey(synTerm)) {
							tempDictionary.put(synTerm,
									new HashMap<Integer, Double>());
						}

						// Add synset link to synterm
						tempDictionary.get(synTerm).put(synTermRank,
								synsetScore);
					}
				}
			}

			// Go through all the terms.
			for (Map.Entry<String, HashMap<Integer, Double>> entry : tempDictionary
					.entrySet()) {
				String word = entry.getKey();
				Map<Integer, Double> synSetScoreMap = entry.getValue();

				// Calculate weighted average. Weigh the synsets according to
				// their rank.
				// Score= 1/2*first + 1/3*second + 1/4*third ..... etc.
				// Sum = 1/1 + 1/2 + 1/3 ...
				double score = 0.0;
				double sum = 0.0;
				for (Map.Entry<Integer, Double> setScore : synSetScoreMap
						.entrySet()) {
					score += setScore.getValue() / (double) setScore.getKey();
					sum += 1.0 / (double) setScore.getKey();
				}
				score /= sum;

				dictionary.put(word, score);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (csv != null) {
				csv.close();
			}
		}
	}

	public static double extract(String word, String pos) {
		if(dictionary.get(word + "#" + pos) != null)
			return dictionary.get(word + "#" + pos);
		else
			return 0.0;
	}
	
}
