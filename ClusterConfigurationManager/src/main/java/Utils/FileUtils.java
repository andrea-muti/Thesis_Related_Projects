package Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public class FileUtils {
	
	public static final String CSV_SPLIT_BY = ";";
	public static final String ENOCDING = "UTF-8";
	
	// da rivedere i paths
	public static final File FILE_LOCATION =  new File(new File(new File(System.getProperty("user.dir")).getParent(), "tools.descartes.bungee"), "files");

	public static String getFileNameWithoutExtension(File file){
		int end = file.toString().lastIndexOf(".");
		if (end >= 0){
			return file.toString().substring(0, end);
		} else {
			return file.toString();
		}
	}
	
	public static File getRelativeFilePath(File absolutePath, File reference){
		File relativePath = absolutePath;
		if (absolutePath.getAbsolutePath().contains(reference.getAbsolutePath())){
			relativePath = new File(absolutePath.getAbsolutePath().substring(reference.getAbsolutePath().length()+1));
		}
		return  relativePath;
	}
	
	public static File getAbsolutePath(File file) {
		if (file.exists()) {
			// path is already absolute
			return file;
		} else {
			// create absolute path
			return new File(FILE_LOCATION, file.toString());
		}
	}
	
	public static boolean saveProperties(Properties properties, File file) {
		boolean success = false;
		OutputStream output = null;
		try {
			output = new FileOutputStream(file);
			properties.store(output, "");
			success = true;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (output != null){
				try {
					output.close();
				} catch (IOException e) {}
			}
		}
		return success;
	}
	
	public static Properties loadProperties(File file) {
		Properties properties = new Properties();
		InputStream input = null;
		try {
			input = new FileInputStream(file);
			properties.load(input);
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return properties;
	}
	
	public static String getProperty(File file, String key){
		Properties props = loadProperties(file);
		String ret = props.getProperty(key);
		return ret;
		
	}
	
	public static boolean setProperty(File file, String key, String value){
		boolean success = true;
		try{
			FileInputStream in = new FileInputStream(file);
			Properties props = new Properties();
			props.load(in);
			in.close();
	
			FileOutputStream out = new FileOutputStream(file);
			props.setProperty(key,value);
			props.store(out, "updated ");
			out.close();
		}
		catch(Exception e){
			success = false;
		}
		return success;
	}
	
}