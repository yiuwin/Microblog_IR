import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class FileWriter {
	static BufferedWriter writer;
	
	/* FileWriter Constructor starts a new file by initializing a new BufferedWriter object 
	 * @param String filename	The name of the output file
	 * */
	public FileWriter(String filename) {
    	try {
    		writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename)));
    	} catch (FileNotFoundException e) {
    		System.out.println("Error creating buffered writer");
    		e.printStackTrace();
    	}
	}
	
	/* Writes str to this file
	 * @param String str 	The content to be written to the BufferedWriter 
	 */
	public void write(String str) {
		try {
        	writer.write(str + "\n");
        } catch (IOException e) {
        	System.out.println("Error writing to file");
        	e.printStackTrace();
        }
	}
	
	/* Close this file */
    public void close() {
        try {
        	writer.close();
        } catch (IOException e) {
        	System.out.println("Error closing file");
        }
    }
	
}
