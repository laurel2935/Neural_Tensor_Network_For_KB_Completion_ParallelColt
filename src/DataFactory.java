

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix2D;
import cern.jet.math.tdouble.DoubleFunctions;

import com.jmatio.io.MatFileReader;
import com.jmatio.types.MLArray;
import com.jmatio.types.MLCell;
import com.jmatio.types.MLDouble;

public class DataFactory{
	private static DataFactory instance;
	
	ArrayList<Tripple> trainingTripples = new ArrayList<Tripple>(); //data for learning the paramters, without labels
	ArrayList<Tripple> devTripples = new ArrayList<Tripple>(); //data for learning the threshold, with labels
	ArrayList<Tripple> testTripples = new ArrayList<Tripple>(); //data for learning the threshold, with labels
		 
	private HashMap<Integer, String> entitiesNumWord = new HashMap<Integer, String>();
	private HashMap<String, Integer> entitiesWordNum = new HashMap<String, Integer>();
	private HashMap<Integer, String> relationsNumWord = new HashMap<Integer, String>();
	private HashMap<String, Integer> relationsWordNum = new HashMap<String, Integer>();
	
	private HashMap<Integer, Tripple> trainingDataNumTripple = new HashMap<Integer, Tripple>();
	private HashMap<Integer, Tripple> devDataNumTripple = new HashMap<Integer, Tripple>();
	private HashMap<Integer, Tripple> testDataNumTripple = new HashMap<Integer, Tripple>();
	
	private HashMap<Integer, String> vocabNumWord = new HashMap<Integer, String>(); //contains vocab of word vectors and index of a word
	private HashMap<String, Integer> vocabWordNum = new HashMap<String, Integer>();
	private static HashMap<String, INDArray> worvectorWordVec = new HashMap<String, INDArray>(); // return word vector for a given word as string
	private static HashMap<Integer, INDArray> worvectorNumVec = new HashMap<Integer, INDArray>(); // return word vector for a given word index
	private INDArray wordVectorMaxtrixLoaded; //matrix contains the original loaded word vectors, size: Word_embedding * numOfWords, every column contains a word vector
	private ArrayList<String> vocab = new ArrayList<String>(); //List with words of entities for only loading word vectors, that need for entity vector creation
	
	private HashMap<Integer, int[]> wordindices = new HashMap<Integer, int[]>();
	
	private int numOfentities;
	private int numOfRelations;
	private int numOfWords; 
	private int batch_size;
	private int corrupt_size;
	private int embeddings_size;
	private boolean reduced_RelationSize;
	private int reduceRelationToSize=1;
	private double random_training_combination_handler;
	private boolean train_both;
	

	public double getRandom_training_combination_handler() {
		return random_training_combination_handler;
	}
	// a random collection inclusive corrupt examples is created by evoking generateNewTrainingBatchJob()
	private ArrayList<Tripple> batchjob = new ArrayList<Tripple>();  // contains the data of a batch training job to optimize paramters

	// Singelton
	public static DataFactory getInstance (int _batch_size, int _corrupt_size, int _embedding_size, boolean _reduce_RelationSize, boolean _train_both) {
		if (DataFactory.instance == null) {
			DataFactory.instance = new DataFactory (_batch_size, _corrupt_size, _embedding_size, _reduce_RelationSize, _train_both);
		    }
		    return DataFactory.instance;
	
	}
	
	private DataFactory(int _batch_size, int _corrupt_size,int _embedding_size, boolean _reduce_RelationSize, boolean _train_both){
		batch_size = _batch_size;
		corrupt_size = _corrupt_size;
		embeddings_size = _embedding_size;
		reduced_RelationSize = _reduce_RelationSize;
		train_both = _train_both;
	}
	public int getNumOfentities() {
		return numOfentities;
	}
	
	public ArrayList<Tripple> getBatchJobTripplesOfRelation(int _relation_index){
		ArrayList<Tripple> tripplesOfThisRelationFromBatchJob = new ArrayList<Tripple>();
		//System.out.println("batchjob size: "+batchjob.size()+" | trainingTripples size: "+trainingTripples.size());
		for (int i = 0; i < batchjob.size(); i++) {
			if (batchjob.get(i).getIndex_relation()==_relation_index) {
				tripplesOfThisRelationFromBatchJob.add(batchjob.get(i));
			}
		}
		return tripplesOfThisRelationFromBatchJob;
	}
	
	public ArrayList<Tripple> getTripplesOfRelation(int _relation_index, ArrayList<Tripple> _listWithTripples){
		ArrayList<Tripple> tripples = new ArrayList<Tripple>();
		//System.out.println("batchjob size: "+batchjob.size());
		for (int i = 0; i < _listWithTripples.size(); i++) {
			if (_listWithTripples.get(i).getIndex_relation()==_relation_index) {
				tripples.add(_listWithTripples.get(i));
			}
		}
		return tripples;
	}

	public ArrayList<Tripple> getAllTrainingTripples() {
		return trainingTripples;
	}


	public void generateNewTrainingBatchJob(){
		if (train_both) {
			random_training_combination_handler=  Math.random();
		}else{
			random_training_combination_handler= 1;
		}
		batchjob.clear();
		Collections.shuffle(trainingTripples);
		Random rand = new Random();
		//int[] e3 = {1,2,3,4,5,6,7,8,9,10,1,2,3,4,5,6,7,8,9,10,1,2,3,4,5,6,7,8,9,10,1,2,3,4,5,6,7,8,9,10};
		//int e3counter=0;
		for (int h = 0; h < corrupt_size; h++) {
			for (int i = 0; i < batch_size; i++) {
				//Set e3 as random corrupt tripple			
				//min =0, max = maxIndexNumOfEntity << numOfEntity-1
				// int randomNum = rand.nextInt((max - min) + 1) + min;
				int random_corrupt_entity = rand.nextInt(((numOfentities-1) - 0) + 1) + 0;
				 //batchjob.add(new Tripple(trainingTripples.get(i), e3[e3counter]));
				batchjob.add(new Tripple(trainingTripples.get(i), random_corrupt_entity));
				//e3counter++;
			}//System.out.println("e1: "+trainingTripples.get(i).getEntity1()+" | e3:"+trainingTripples.get(i).getIndex_entity3_corrupt());
		}
		System.out.println("Training Batch Job created and contains of "+batchjob.size()+" Trippels.");
	}
	
	public INDArray getINDArrayOfTripples(ArrayList<Tripple> _tripples){
		INDArray tripplesMatrix = Nd4j.zeros(_tripples.size(),3);
		
		for (int i = 0; i < _tripples.size(); i++) {
			tripplesMatrix.put(i,0, _tripples.get(i).getIndex_entity1());
			tripplesMatrix.put(i,1, _tripples.get(i).getIndex_relation());
			tripplesMatrix.put(i,2, _tripples.get(i).getIndex_entity2());
			System.out.println("tripplesMatrix: "+tripplesMatrix);
		}
		
		return tripplesMatrix;
	}
	
	public void loadEntitiesFromSocherFile(String path) throws IOException{
		FileReader fr = new FileReader(path);
	    BufferedReader br = new BufferedReader(fr);
	    String line = br.readLine();
	    int entities_counter = 0;
	    while (line != null) {
	    	entitiesNumWord.put(entities_counter, line);
	    	entitiesWordNum.put(line,entities_counter);
	    	//get words from entity 	
	    	String entity_name_clear; //clear name without _  __name_ -> name
			try {
				entity_name_clear = line.substring(2, line.lastIndexOf("_"));
			} catch (Exception e) {
				entity_name_clear =line.substring(2);			
			}
			//System.out.println("entity_name_clear: "+entity_name_clear);			
			
			if (entity_name_clear.contains("_")) { //whitespaces are _
				//Entity conains of more than one word
				for (int j = 0; j <entity_name_clear.split("_").length; j++) {
						vocab.add(entity_name_clear.split("_")[j]);
				}
			}else{
				// Entity conains of only one word
				vocab.add(entity_name_clear);
			}
			
	    	line = br.readLine();
	    	entities_counter++;
		}  
	    br.close();
	    //number of entities need increased by one to handle the zero entry
	    numOfentities = entities_counter;
	    System.out.println(numOfentities + " Entities loaded, containing of "+vocab.size()+" different words| last entity:"+entitiesNumWord.get(entitiesNumWord.size()-1));
	}
	
	public void loadRelationsFromSocherFile(String path) throws IOException{
		// example: _has_instance
		FileReader fr = new FileReader(path);
	    BufferedReader br = new BufferedReader(fr);
	    String line = br.readLine();
	    int relations_counter = 0;
  

	    if (reduced_RelationSize==false) {
		    while (line!=null) {
		    	relationsNumWord.put(relations_counter,line);
		    	relationsWordNum.put(line,relations_counter);
		    	line = br.readLine();
		    	relations_counter++;
			} 
			numOfRelations = relations_counter;
		}else{
		    while (relations_counter<reduceRelationToSize) {
		    	relationsNumWord.put(relations_counter,line);
		    	relationsWordNum.put(line,relations_counter);
		    	line = br.readLine();
		    	relations_counter++;
			} 
	    	numOfRelations = relations_counter;
		}
	    br.close();
	    System.out.println(numOfRelations+ " Relations loaded reduced relation size is: "+reduced_RelationSize);

	}
	
	public void loadTrainingDataTripplesE1rE2(String path) throws IOException{
		/*if (true) {
			FileReader fr = new FileReader(path);
		}else{
			
		}*/
		InputStreamReader fr = new InputStreamReader(new FileInputStream(path));
	    BufferedReader br = new BufferedReader(fr);
	    String line = br.readLine();
	    int trainings_tripple_counter = 0;
	    while (line != null) {
	    	//System.out.println("line: "+line);
	    	//System.out.println(trainings_tripple_counter+ " line: "+line.split("\\s")[0]+"|"+line.split("\\s")[1]+"|"+line.split("\\s")[2]);
	    	if (reduced_RelationSize==false) {
		    	int e1 = entitiesWordNum.get(line.split("\\s")[0]);
		    	int rel = relationsWordNum.get(line.split("\\s")[1]);
		    	int e2 = entitiesWordNum.get(line.split("\\s")[2]);
		    	trainingDataNumTripple.put(trainings_tripple_counter,new Tripple(e1, line.split("\\s")[0], rel, line.split("\\s")[1], e2, line.split("\\s")[2] ));
		    	trainingTripples.add(new Tripple(e1, line.split("\\s")[0], rel, line.split("\\s")[1], e2, line.split("\\s")[2] ));
		    	line = br.readLine();
		    	trainings_tripple_counter++;
	    	}else{	
	    		if (line.split("\\s")[1].equals(relationsNumWord.get(0))) {
	    		//if (line.split("\\s")[1].equals(relationsNumWord.get(0))|line.split("\\s")[1].equals(relationsNumWord.get(1))) {
	    			int e1 = entitiesWordNum.get(line.split("\\s")[0]);
			    	int rel = relationsWordNum.get(line.split("\\s")[1]);
			    	int e2 = entitiesWordNum.get(line.split("\\s")[2]);
			    	trainingDataNumTripple.put(trainings_tripple_counter,new Tripple(e1, line.split("\\s")[0], rel, line.split("\\s")[1], e2, line.split("\\s")[2] ));
			    	trainingTripples.add(new Tripple(e1, line.split("\\s")[0], rel, line.split("\\s")[1], e2, line.split("\\s")[2] ));
			    	line = br.readLine();
			    	trainings_tripple_counter++;
				}else{
					line = br.readLine();
				}
					
	    	}
		}   
	    br.close();
	    System.out.println(trainings_tripple_counter+" Training Examples loaded...");
	}
	
	public void loadDevDataTripplesE1rE2Label(String path) throws IOException{
		//get dev data for calculation the threshold
		FileReader fr = new FileReader(path);
	    BufferedReader br = new BufferedReader(fr);
	    String line = br.readLine();
	    int dev_tripple_counter = 0;
	    while (line != null) {
	    	//System.out.println("line: "+line.split("\\s")[0]+"|"+line.split("\\s")[1]+"|"+line.split("\\s")[2]+"|"+line.split("\\s")[3]+"|");
	    	if (reduced_RelationSize==false) {
		    	int e1 = entitiesWordNum.get(line.split("\\s")[0]);
		    	int rel = relationsWordNum.get(line.split("\\s")[1]);
		    	int e2 = entitiesWordNum.get(line.split("\\s")[2]); 
		    	int label = Integer.parseInt(line.split("\\s")[3]);
		    	devDataNumTripple.put(dev_tripple_counter,new Tripple(e1, line.split("\\s")[0], rel, line.split("\\s")[1], e2, line.split("\\s")[2],label ));
		    	devTripples.add(new Tripple(e1, line.split("\\s")[0], rel, line.split("\\s")[1], e2, line.split("\\s")[2],label ));
		    	line = br.readLine();
		    	dev_tripple_counter++;
	    	}else{
	    		if (line.split("\\s")[1].equals(relationsNumWord.get(0))) {
	    			int e1 = entitiesWordNum.get(line.split("\\s")[0]);
			    	int rel = relationsWordNum.get(line.split("\\s")[1]);
			    	int e2 = entitiesWordNum.get(line.split("\\s")[2]); 
			    	int label = Integer.parseInt(line.split("\\s")[3]);
			    	devDataNumTripple.put(dev_tripple_counter,new Tripple(e1, line.split("\\s")[0], rel, line.split("\\s")[1], e2, line.split("\\s")[2],label ));
			    	devTripples.add(new Tripple(e1, line.split("\\s")[0], rel, line.split("\\s")[1], e2, line.split("\\s")[2],label ));
			    	line = br.readLine();
			    	dev_tripple_counter++;
				}else{
					line = br.readLine();
				}
	    	}
		}   
	    br.close();
	    System.out.println(dev_tripple_counter +" Dev Examples loaded...");
	}
	
	public void loadTestDataTripplesE1rE2Label(String path) throws IOException{
		//get test data
		FileReader fr = new FileReader(path);
	    BufferedReader br = new BufferedReader(fr);
	    String line = br.readLine();
	    int test_tripple_counter = 0;
	    while (line != null) {
	    	if (reduced_RelationSize==false) {
		    	//System.out.println("line: "+line.split("\\s")[0]+"|"+line.split("\\s")[1]+"|"+line.split("\\s")[2]+"|"+line.split("\\s")[3]+"|");
		    	int e1 = entitiesWordNum.get(line.split("\\s")[0]);
		    	int rel = relationsWordNum.get(line.split("\\s")[1]);
		    	int e2 = entitiesWordNum.get(line.split("\\s")[2]); 
		    	int label = Integer.parseInt(line.split("\\s")[3]);
		    	testDataNumTripple.put(test_tripple_counter,new Tripple(e1, line.split("\\s")[0], rel, line.split("\\s")[1], e2, line.split("\\s")[2],label ));
		    	testTripples.add(new Tripple(e1, line.split("\\s")[0], rel, line.split("\\s")[1], e2, line.split("\\s")[2],label ));
		    	line = br.readLine();
		    	test_tripple_counter++;
	    	}else{
	    		if (line.split("\\s")[1].equals(relationsNumWord.get(0))) {
	    			int e1 = entitiesWordNum.get(line.split("\\s")[0]);
			    	int rel = relationsWordNum.get(line.split("\\s")[1]);
			    	int e2 = entitiesWordNum.get(line.split("\\s")[2]); 
			    	int label = Integer.parseInt(line.split("\\s")[3]);
			    	testDataNumTripple.put(test_tripple_counter,new Tripple(e1, line.split("\\s")[0], rel, line.split("\\s")[1], e2, line.split("\\s")[2],label ));
			    	testTripples.add(new Tripple(e1, line.split("\\s")[0], rel, line.split("\\s")[1], e2, line.split("\\s")[2],label ));
			    	line = br.readLine();
			    	test_tripple_counter++;
	    		}else{
					line = br.readLine();
				}
	    	}
	    		
		}   
	    br.close();
	    System.out.println(test_tripple_counter +" Test Examples loaded...");

	}
	
	public void loadWordVectorsFromMatFile(String path, boolean optimizedLoad){
		//load word vectors with a dimension of 100 from a matlab mat file
		try {
			MatFileReader matfilereader = new MatFileReader(path);
			//words
			MLCell words_mat = (MLCell) matfilereader.getMLArray("words");
			//word embeddings
			MLArray wordvectors_mat = (MLArray) matfilereader.getMLArray("We");
			MLDouble mlArrayDouble = (MLDouble) wordvectors_mat;
			System.out.println("mlArrayDouble"+mlArrayDouble.getM()+"|"+mlArrayDouble.getN()+"|"+mlArrayDouble.getNDimensions()+"|"+mlArrayDouble.getSize());
			//INDArray wordvectormatrix = Nd4j.zeros(100,1); //100 dimension of word embeddings
			String word;
			int wvCounter=0;
			//PrintWriter writer = new PrintWriter("C://Users//Patrick//Documents//master arbeit//original_code//data//wordsForOrgENVecros.txt", "Cp1252");
			//PrintWriter writer2 = new PrintWriter("C://Users//Patrick//Documents//master arbeit//original_code//data//wordvecsForTSNE.txt", "Cp1252");
			for (int i = 0; i < mlArrayDouble.getSize()/100; i++) {
				
				//System.out.println("word: "+words_mat.get(i).contentToString().substring(7,words_mat.get(i).contentToString().lastIndexOf("'")));				
				//load word vector
				word = words_mat.get(i).contentToString().substring(7,words_mat.get(i).contentToString().lastIndexOf("'"));
				//writer.println(word);
				
				vocab.add("unknown");
				if (optimizedLoad==true) {
					//only load word vectors which are needed for entity vectors
					if (vocab.contains(word)) { //look up if there is an entity with this word
						vocabNumWord.put(wvCounter, word);
						vocabWordNum.put(word, wvCounter);
						INDArray wordvector = Nd4j.zeros(100,1);
						for (int j = 0; j < 100; j++) {
							wordvector.put(j, 0, mlArrayDouble.get(i, j));	
						}
						
						worvectorNumVec.put(wvCounter, wordvector);
						worvectorWordVec.put(word, wordvector);	
						wvCounter++;
					}
					
				}else{
					vocabNumWord.put(wvCounter, word);
					vocabWordNum.put(word, wvCounter);
					String wv = ""+mlArrayDouble.get(i, 0);
					INDArray wordvector = Nd4j.ones(100,1);
					for (int j = 0; j < 100; j++) {
						wordvector.put(j, 0, mlArrayDouble.get(i, j));
						if (j>0) {
							wv = wv +","+ mlArrayDouble.get(i, j);
						}
						
					}					
					//writer2.println(wv);
					worvectorNumVec.put(wvCounter, wordvector);
					worvectorWordVec.put(word, wordvector);	
					//System.out.println(word+": "+wordvector);
					wvCounter++;
				}
								
			}
			//writer.close();
			//writer2.close();
			numOfWords = worvectorNumVec.size();
			System.out.println("num of words: "+numOfWords);
			System.out.println(worvectorNumVec.size() +" Word Vectors loaded... Counter: "+wvCounter);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//create a word vector matrix
		wordVectorMaxtrixLoaded = Nd4j.zeros(100, numOfWords);
		for (int i = 0; i < numOfWords; i++) {
			//System.out.println("worvectorNumVec.get(i): "+worvectorNumVec.get(i));
			wordVectorMaxtrixLoaded.putColumn(i, worvectorNumVec.get(i));
		}
		System.out.println("asterid: "+wordVectorMaxtrixLoaded.getColumn(vocabWordNum.get("asterid")));
		System.out.println("bike: "+wordVectorMaxtrixLoaded.getColumn(vocabWordNum.get("bike")));
		//System.out.println("wordVectorMaxtrixLoaded col 1: "+wordVectorMaxtrixLoaded.getColumn(1));
		//System.out.println("word vector matrix ready..."+wordVectorMaxtrixLoaded);
		/*try {
			Nd4j.writeTxt(wordVectorMaxtrixLoaded, "C://Users//Patrick//Documents//master arbeit//original_code//data//Wordnet//wordvecmatrix_matlabFull.txt", ",");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}
	
	public void loadWordIndicesFromFile(String path) throws IOException{
		// example: line=entity, numbers == wordindices
		FileReader fr = new FileReader(path);
	    BufferedReader br = new BufferedReader(fr);
	    String line = br.readLine();
	    int wi_counter = 0;
	    while (line!=null) {
	    	if (line.split(",").length==0) {
	    		//int[] e3 = {9167, 2100, 30636, 13136, 22825, 24943};
	    		wordindices.put(wi_counter, new int[]{Integer.parseInt(line)});
			}else{
				int[] temp = new int[line.split(",").length];
				for (int i = 0; i < temp.length; i++) {
					temp[i] = Integer.parseInt(line.split(",")[i]);
				}
				wordindices.put(wi_counter,temp);
			}

	    	line = br.readLine();
	    	wi_counter++;
		} 
	    br.close();
	    System.out.println(wi_counter+ " Word Indices loaded.");

	}
	
	public INDArray createVectorsForEachEntityByWordVectors(){
		//NOT MORE IN USE
		//Method doesnt used the new in readed word vectors
		INDArray entity_vectors = Nd4j.zeros(embeddings_size,numOfentities);
		for (int i = 0; i < entitiesNumWord.size(); i++) {
			String entity_name; //clear name without _  __name_ -> name
			try {
				entity_name = entitiesNumWord.get(i).substring(2, entitiesNumWord.get(i).lastIndexOf("_"));
			} catch (Exception e) {
				entity_name =entitiesNumWord.get(i).substring(2);			
			}
			//System.out.println("entity: "+entitiesNumWord.get(i)+" | word: "+entity_name);			

			if (entity_name.contains("_")) { //whitespaces are _
				//Entity conains of more than one word
				INDArray entityvector = Nd4j.zeros(embeddings_size, 1);
				int counterOfWordVecs = 0;

				for (int j = 0; j < entity_name.split("_").length; j++) {
					try {
						entityvector = entityvector.add(worvectorWordVec.get(entity_name.split("_")[j]));
					} catch (Exception e) {
						//if no word vector available, use "unknown" word vector
						entityvector = entityvector.add(worvectorWordVec.get("unknown"));
					}
					counterOfWordVecs++;
				}
				
				entityvector = entityvector.div(counterOfWordVecs);
				entity_vectors.putColumn(i, entityvector);
			}else{
				// Entity contains of only one word
				/*if (entity_name.equals("absorption") | entity_name.equals("centering")) {
					System.out.println("entity_name: "+entity_name +"wv: "+worvectorWordVec.get(entity_name));
				}*/
				
				try {
					entity_vectors.putColumn(i, worvectorWordVec.get(entity_name));
				} catch (Exception e) {
					// if no word vector available, use "unknown" word vector
					entity_vectors.putColumn(i, worvectorWordVec.get("unknown"));
				}			
			}		
		}
		return entity_vectors;
	}
	
	public INDArray createVectorsForEachEntityByWordVectors(INDArray updatedWVMatrix){
		// NOT IN USE
		INDArray entity_vectors = Nd4j.zeros(embeddings_size,numOfentities);
		for (int i = 0; i < entitiesNumWord.size(); i++) {
			String entity_name; //clear name without _  __name_ -> name
			try {
				entity_name = entitiesNumWord.get(i).substring(2, entitiesNumWord.get(i).lastIndexOf("_"));
			} catch (Exception e) {
				entity_name =entitiesNumWord.get(i).substring(2);			
			}
			//System.out.println("entity: "+entitiesNumWord.get(i)+" | word: "+entity_name);			
			
			if (entity_name.contains("_")) { //whitespaces are _
				//Entity conains of more than one word
				INDArray entityvector = Nd4j.zeros(embeddings_size, 1);
				for (int j = 0; j <entity_name.split("_").length; j++) {
					try {
						entityvector = entityvector.add(updatedWVMatrix.getColumn(vocabWordNum.get(entity_name.split("_")[j])));
						//System.out.println("etest: "+entity_name.split("_")[j]);
					} catch (Exception e) {
						//if no word vector available, use "unknown" word vector
						System.out.println(" is Unknown"+i);
						entityvector = entityvector.add(updatedWVMatrix.getColumn(vocabWordNum.get("unknown")));
					}			
				}
				entityvector = entityvector.div(entity_name.split("_").length);
				entity_vectors.putColumn(i, entityvector);
			}else{
				// Entity conains of only one word
				try {
					entity_vectors.putColumn(i, updatedWVMatrix.getColumn(vocabWordNum.get(entity_name)));
				} catch (Exception e) {
					// if no word vector available, use "unknown" word vector
					System.out.println("unknown"+i);
					entity_vectors.putColumn(i, updatedWVMatrix.getColumn(vocabWordNum.get("unknown")));
				}

			}
			if (entity_vectors.getColumn(i).equals(createVectorForEachEntityByWordVectorsWithPreloadIdices(updatedWVMatrix,i))) {
				
			}else{
				System.out.println("Not equal: "+i+":"+entity_name+" word indices: "+wordindices.get(i).length);
				System.out.println(entity_vectors.getColumn(i)+"|"+createVectorForEachEntityByWordVectorsWithPreloadIdices(updatedWVMatrix,i));
			}
			
		}
		return entity_vectors;
	}
	public INDArray createVectorsForEachEntityByWordVectorsWithPreloadIdices(INDArray updatedWVMatrix){
		INDArray entity_vectors = Nd4j.zeros(embeddings_size,numOfentities);
		for (int i = 0; i < entitiesNumWord.size(); i++) {
			INDArray entityvector = Nd4j.zeros(embeddings_size, 1);
			for (int j = 0; j <wordindices.get(i).length; j++) {
					try {
						entityvector = entityvector.add(updatedWVMatrix.getColumn(wordindices.get(i)[j]));
					} catch (Exception e) {
						//if no word vector available, use "unknown" word vector
						System.out.println("CATCH!");
						entityvector = entityvector.add(updatedWVMatrix.getColumn(vocabWordNum.get("unknown")));
					}			
				}
				entityvector = entityvector.div(wordindices.get(i).length);
				entity_vectors.putColumn(i, entityvector);		
		}
		return entity_vectors;
	}
	
	public INDArray createVectorForEachEntityByWordVectorsWithPreloadIdices(INDArray updatedWVMatrix, int i){
		INDArray entityvector = Nd4j.zeros(embeddings_size, 1);
		for (int j = 0; j <wordindices.get(i).length; j++) {
					entityvector = entityvector.add(updatedWVMatrix.getColumn(wordindices.get(i)[j]));		
		}
		return entityvector.div(wordindices.get(i).length);
	}
	
	public int entityLength(int entityIndexNum){
		//of how much words contains this entity
		// return: 1 means 1 word | -3 because of other _		
		/*try {
			return entitiesNumWord.get(entityIndexNum).split("_").length-3;
		} catch (Exception e) {
			// TODO: handle exception
			System.out.println("entityIndexNum: "+entityIndexNum+" | "+entitiesNumWord.get(entityIndexNum)+" | false vocab word by entitynum: "+vocabNumWord.get(entityIndexNum));
			return 1;
		}*/
		return wordindices.get(entityIndexNum).length;
		
		
	}
	
	public DoubleMatrix2D createVectorsForEachEntityByWordVectorsWithPreloadIdicesP(DoubleMatrix2D updatedWVMatrixP){
		DoubleMatrix2D entity_vectors = new DenseDoubleMatrix2D(embeddings_size,numOfentities);
		for (int i = 0; i < entitiesNumWord.size(); i++) {
			DoubleMatrix1D entityvector = new DenseDoubleMatrix1D(embeddings_size);
			for (int j = 0; j <wordindices.get(i).length; j++) {
					try {
						//entityvector = entityvector.add(updatedWVMatrix.getColumn(wordindices.get(i)[j]));
						entityvector.assign(updatedWVMatrixP.viewColumn(wordindices.get(i)[j]), DoubleFunctions.plus);
					} catch (Exception e) {
						//if no word vector available, use "unknown" word vector
						System.out.println("CATCH!");
						//entityvector = entityvector.add(updatedWVMatrix.getColumn(vocabWordNum.get("unknown")));
						entityvector.assign(updatedWVMatrixP.viewColumn(vocabWordNum.get("unknown")), DoubleFunctions.plus);
					}			
			}
			//entityvector = entityvector.div(wordindices.get(i).length);
			entityvector.assign(DoubleFunctions.div(wordindices.get(i).length));
			//entity_vectors.putColumn(i, entityvector);
			entity_vectors.viewColumn(i).assign(entityvector);
		}
		return entity_vectors;
	}
	
	public INDArray createVectorForEachEntityByWordVectorsWithPreloadIdicesP(INDArray updatedWVMatrix, int i){
		INDArray entityvector = Nd4j.zeros(embeddings_size, 1);
		for (int j = 0; j <wordindices.get(i).length; j++) {
					entityvector = entityvector.add(updatedWVMatrix.getColumn(wordindices.get(i)[j]));		
		}
		return entityvector.div(wordindices.get(i).length);
	}
	
	public int entityLengthP(int entityIndexNum){
		//of how much words contains this entity
		// return: 1 means 1 word | -3 because of other _		
		/*try {
			return entitiesNumWord.get(entityIndexNum).split("_").length-3;
		} catch (Exception e) {
			// TODO: handle exception
			System.out.println("entityIndexNum: "+entityIndexNum+" | "+entitiesNumWord.get(entityIndexNum)+" | false vocab word by entitynum: "+vocabNumWord.get(entityIndexNum));
			return 1;
		}*/
		return wordindices.get(entityIndexNum).length;
		
		
	}

	public ArrayList<Tripple> getDevTripples() {
		return devTripples;
	}
	public ArrayList<Tripple> getTestTripples() {
		return testTripples;
	}
	public int getNumOfWords() {
		return numOfWords;
	}

	public int getNumOfRelations() {
		return numOfRelations;
	}
	
	public INDArray getWordVectorMaxtrixLoaded() {
		return wordVectorMaxtrixLoaded;
	}
	public INDArray getEntitiy1IndexNumbers(ArrayList<Tripple> list){
		//number is corresponding to column in entityvectors matrix
		INDArray e1_list = Nd4j.create(list.size());
		for (int i = 0; i < list.size(); i++) {
			e1_list.putScalar(i, list.get(i).getIndex_entity1());
		}
		return e1_list;
	}
	public INDArray getEntitiy2IndexNumbers(ArrayList<Tripple> list){
		//number is corresponding to column in entityvectors matrix
		INDArray e2_list = Nd4j.create(list.size());
		for (int i = 0; i < list.size(); i++) {
			e2_list.putScalar(i, list.get(i).getIndex_entity2());
		}
		return e2_list;
	}
	public INDArray getRelIndexNumbers(ArrayList<Tripple> list){
		//number is corresponding to column in entityvectors matrix
		INDArray rel_list = Nd4j.create(list.size());
		for (int i = 0; i < list.size(); i++) {
			rel_list.putScalar(i, list.get(i).getIndex_relation());
		}
		return rel_list;
	}
	public INDArray getEntitiy3IndexNumbers(ArrayList<Tripple> list){
		//number is corresponding to column in entityvectors matrix
		INDArray e3_list = Nd4j.create(list.size());
		for (int i = 0; i < list.size(); i++) {
			e3_list.putScalar(i, list.get(i).getIndex_entity3_corrupt());
		}
		return e3_list;
	}
	public int[] getWordIndexes(int entityIndex){
		/*int[] wordIndexes = new int[entityLength(entityIndex)];
		if (entityLength(entityIndex)==0) {
			//System.out.println("+++++ "+entitiesNumWord.get(entityIndex) +" entityLength(entityIndex)"+entityLength(entityIndex));
			//exception for corrupt training data: entityIndexNum: 9847 | __2 |
			wordIndexes = new int[1];
		}
		
		// get words of entity	
		String entity_name; //clear name without _  __name_ -> name
		try {
			entity_name = entitiesNumWord.get(entityIndex).substring(2, entitiesNumWord.get(entityIndex).lastIndexOf("_"));
		} catch (Exception e) {
			entity_name =entitiesNumWord.get(entityIndex).substring(2);			
		}
		
		// get word indexes
		if (entity_name.contains("_")) { //whitespaces are _
			//Entity conains of more than one word
			for (int j = 0; j <entity_name.split("_").length; j++) {
				try {
					wordIndexes[j] = vocabWordNum.get(entity_name.split("_")[j]);
				} catch (Exception e) {
					//if no word vector available, use "unknown" word vector
					wordIndexes[j] = vocabWordNum.get("unknown");
				}			
			}
		}else{
			// Entity conains of only one word
			try {
				wordIndexes[0] = vocabWordNum.get(entity_name);
			} catch (Exception e) {
				// if no word vector available, use "unknown" word vector
				wordIndexes[0] = vocabWordNum.get("unknown");
			}			
		}	
		//System.out.println("wordIndexes: "+ wordIndexes.length + " | "+wordIndexes[0]);		
		*/
		return wordindices.get(entityIndex);
	}

	public void loadWordVectorsFromFile(String path) throws IOException{
		//load word vectors with a dimension of 100 from a txt file
	    INDArray wordvecs = Nd4j.readTxt(path,",");
	    wordvecs.reshape(100, (wordvecs.length()-1)/100);
		numOfWords = wordvecs.length()/100;
		System.out.println(numOfWords +" Word Vectors loaded... ");
		wordVectorMaxtrixLoaded = wordvecs;
		//System.out.println(wordVectorMaxtrixLoaded.getColumn(10000));
		//smoothing 
		//wordVectorMaxtrixLoaded.muli(0.1);
		System.out.println("wordVectorMaxtrixLoaded: "+wordVectorMaxtrixLoaded);
		
	}
	

}
