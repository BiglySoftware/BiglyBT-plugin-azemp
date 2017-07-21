package org.opensubtitles.api;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.Calendar;
import java.util.Locale;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;


import com.biglybt.core.util.ByteFormatter;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class OpenSubtitlesAPI {
	
	public static String login(String username,String password) {
		return OSXMLRPC.logIn(username, password);
	}
	
	public static boolean upload(String subtitleFileName,String movieFileName) {
		return upload(subtitleFileName,movieFileName,"");
	}
	
	public static boolean upload(String subtitleFileName,String movieFileName,String token) {
		if(subtitleFileName == null || movieFileName == null) return false;
		
		File subtitleFile = new File(subtitleFileName);
		File movieFile = new File(movieFileName);
		
		if(!subtitleFile.exists() || ! movieFile.exists()) return false;
		
		long size = subtitleFile.length();
		//Abort the process if subtitles are too big
		if(size > 1024*1024) return false;
		
		String movieHash = null;
		long movieSize = 0;
		try {
			movieHash = OpenSubtitlesHasher.computeHash(movieFile);
			movieSize = movieFile.length();
		} catch (Exception e) {
		}
		
		if(movieSize <= 0 || movieHash == null) return false;
		
		String subtitleName = subtitleFile.getName();
		String movieName = movieFile.getName();
		
		String subtitleHash = null;
		
		byte[] subFileContents = new byte[(int)size];
		
		try {
			
			FileInputStream is = new FileInputStream(subtitleFile);
			int read = -1;
			int totalRead = 0;
			while( totalRead < size && (read= is.read(subFileContents,totalRead,(int)size-totalRead)) != -1) {
				totalRead += read;
			}
			
			is.close();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			md5.reset();
			md5.update(subFileContents);
			byte[] hash = md5.digest();
			subtitleHash = ByteFormatter.encodeString(hash);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if(subtitleHash == null) return false;
		
		UploadInfo info =  OSXMLRPC.getUploadInfo(token,subtitleHash, subtitleName, movieHash, movieSize, movieName);
		
		if(!info.isShouldUpload()) return false;
		if(info.getImdbId() == null) return false;
		
		String subContent = null;

		try {
			
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DeflaterOutputStream os = new DeflaterOutputStream(baos);
			
			os.write(subFileContents);
			os.finish();
			os.close();
			byte[] toBeEncoded = baos.toByteArray();
			subContent = new String(toBase64(toBeEncoded));
			
		} catch (Exception e) {
			
		}
		
		if(subContent == null) return false;
		
		Locale language = identifyLanguage(subFileContents);
		
		OSXMLRPC.uploadSubtitles(token,subtitleHash, subtitleName, movieHash, movieSize, movieName,subContent,info.getImdbId(),language);
		
		return true;

	}
	
	public static Subtitle[] getSubtitlesForFile(String file) {
		if(file == null) return new Subtitle[0];
		
		File f  = new File(file);
		if(!f.exists()) return new Subtitle[0];
		
		String hash = null;
		try {
			hash = OpenSubtitlesHasher.computeHash(f);
		} catch (Exception e) {
			
		}
		
		if(hash == null) return new Subtitle[0];
		
		return OSXMLRPC.searchSubtitles(hash, f.length());
		
	}
	
	
	//We're going to download the subtitle to the parent of "file", so that the video file and the subtitle are in the same location
	//We'll also rename the subtitle file with the name of the file, minus the extension + the language-code
	public static boolean downloadSubtitle(String subtitleFileName,Subtitle subtitle) {
		try {
			URL url = new URL(subtitle.getDownloadLink());
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			GZIPInputStream zis = new GZIPInputStream(con.getInputStream());
			
			File subFile = new File(subtitleFileName);
			FileOutputStream fos = new FileOutputStream(subFile);
			
			byte[] buffer = new byte[8192];
			int read = 0;
			
			while((read = zis.read(buffer)) != -1) {
				fos.write(buffer, 0, read);
			}
			fos.close();
			
			return true;
			
		} catch (Exception e) {
			//e.printStackTrace();
			return false;
		}
	}
	
	public static String getSubtitleFileName(String mediaFile,Subtitle subtitle) {
		int extension = mediaFile.lastIndexOf('.');
		if(extension != -1) {
			mediaFile = mediaFile.substring(0,extension);
		}
		mediaFile += "_" + subtitle.getLocale().getLanguage() + "." + subtitle.getFileExtenstion();
		
		return mediaFile;
		
	}
	
	public static String[] getLocalSubtitlesForFile(String movieFileName) {
		File movieFile = new File(movieFileName);
		if(! (movieFile.exists() && movieFile.isFile())) return new String[0];
		File parent = movieFile.getParentFile();
		
		final String filter = getMediaFileBaseName(movieFileName) + "_";
		
		File[] fResults = parent.listFiles(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				if(name.startsWith(filter)) {
					name = name.toLowerCase();
					if( name.endsWith(".srt") ||
							name.endsWith(".txt") ||
							name.endsWith(".sub") ||
							name.endsWith(".ssa") ||
							name.endsWith(".smi") ||
							name.endsWith(".mpl") ||
							name.endsWith(".tmp")
							) {
						return true;
					}
				}
				return false;
			}
		});
		
		if(fResults == null) {
			return new String[0];
		}
		
		String[] results = new String[fResults.length];
		for(int i = 0 ; i < results.length ; i++) {
			results[i] = fResults[i].getAbsolutePath();
		}
		
		return results;
		
	}
	
	public static Locale getLocalSubtitleLanguage(String movieFileName,String subtitleFileName) {
		String mediaBaseName = getMediaFileBaseName(movieFileName) + "_";
		
		//If we're given a full path to the subtitleFile, let's extract the filename
		File subtitleFile = new File(subtitleFileName);
		if(subtitleFile.exists()) {
			subtitleFileName = subtitleFile.getName();
		}
		
		if(subtitleFileName.startsWith(mediaBaseName)) {
			int dotIndex = subtitleFileName.lastIndexOf('.');
			if(dotIndex != -1) {
				String language = subtitleFileName.substring(mediaBaseName.length(),dotIndex);
				return new Locale(language);
			}
		}
		
		return null;
	}
	
	public static String getMediaFileBaseName(String movieFileName) {
		File movieFile = new File(movieFileName);
		String mediaBaseName = movieFileName;
		if(movieFile.exists()) {
			mediaBaseName = movieFile.getName();
		}
		int extension = mediaBaseName.lastIndexOf('.');
		if(extension != -1) {
			mediaBaseName = mediaBaseName.substring(0,extension);
		}
		return mediaBaseName;
	}
	
	private static String toBase64(byte s[]) {
        // You may use this for lower  applet size
        // return new  sun.misc.BASE64Encoder().encode(s);
        
        char tx;
        String str="";
        String t = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        char t2[]=t.toCharArray();        
        char wynik[]=new char[(s.length/3+1)*4];
        
        int tri=0;
        for (int i=0;i<(s.length/3+1) ;i++) {
            tri=0;
            int iii=i*3;
            try{
                tri|= (s[iii  ] & 0xff)<<16;
                tri|= (s[iii+1] & 0xff)<<8;
                tri|= (s[iii+2] & 0xff);            
             } catch(Exception e) {};
            for(int j = 0; j < 4; j++)              
                wynik[i*4+j]=(iii*8+j*6>=s.length*8) ? '=' : t2[(tri >> 6*(3-j)) & 0x3f];                                  
            //  if((i+1) % 19 ==0 ) str +="\n";
        };
        str = new String(wynik);
        if ( str.endsWith("====") ) str=str.substring(0, str.length()-4);
        
        return str;
    };
    
    public static Locale identifyLanguage(byte[] subtitleFileContent) {
    	ByteArrayInputStream bais = new ByteArrayInputStream(subtitleFileContent);
    	BufferedReader br = new BufferedReader(new InputStreamReader(bais));
    	StringBuffer sb = new StringBuffer();
    	int nbLines = 0;
    	String line;
    	try {
    		final String excludeChars = "0123456789{(";
	    	while(nbLines < 10 && (line = br.readLine()) != null) {
	    		if(line.length() == 0) {
	    			continue;
	    		}
	    		char c  = line.charAt(0);
	    		if(excludeChars.indexOf(c) == -1) {
	    			sb.append(line);
	    			sb.append(" ");
	    			nbLines++;
	    		}
	    	}
	    	
	    	String toTest = sb.toString();
	    	toTest = URLEncoder.encode(toTest, "UTF-8");
	    	
	    	URL url = new URL("http://ajax.googleapis.com/ajax/services/language/detect?v=1.0&q=" + toTest);
	    	URLConnection connection = url.openConnection();
	    	connection.addRequestProperty("Referer", "http://www.vuze.com");
	    	
	    	BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
	    	
	    	JSONObject json = (JSONObject) JSONValue.parse(reader);
	    	JSONObject responseData = (JSONObject) json.get("responseData");
	    	String language = (String) responseData.get("language");
	    	return new Locale(language);
	    	
	    	
    	} catch (Exception e) {
    		e.printStackTrace();
			return null;
		}
    	
    	
    }
	
	public static void main(String[] args) {
		
		//String subFileName  = "/Users/olivier/Documents/Azureus Downloads/Chuck.S03E09.HDTV.XviD-LOL_en.srt";
		//String fileName = "/Users/olivier/Documents/Azureus Downloads/Chuck.S03E09.HDTV.XviD-LOL.avi";
		
		String fileName = "C:\\Users\\Olivier\\Documents\\Azureus Downloads\\Chuck.S03E09.HDTV.XviD-LOL.avi";
		String subFileName = "C:\\Users\\Olivier\\Documents\\Azureus Downloads\\Chuck.S03E09.HDTV.XviD-LOL_en.srt";
				
		String token = login("gudy", "password");
		
		//TODO : login first
		boolean didUpload = upload(subFileName,fileName,token);
		
		
		
		/*Subtitle[] results = getSubtitlesForFile(fileName);
		for(Subtitle sub : results) {
			String subFileName = getSubtitleFileName(fileName, sub);
			System.out.println("Downloading : " + sub + " to " + subFileName);
			System.out.println(downloadSubtitle(subFileName, sub));
			
		}*/
		
		
	}

}
